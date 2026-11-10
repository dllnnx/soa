package ru.itmo.oscar.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.hibernate.LockOptions;
import org.hibernate.jpa.SpecHints;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import ru.itmo.oscar.model.JobError;
import ru.itmo.oscar.model.JobState;
import ru.itmo.oscar.model.JobStatus;
import ru.itmo.oscar.model.JobType;
import ru.itmo.oscar.model.OperationCommand;
import ru.itmo.oscar.model.UpdatedMoviesCount;

@Repository
@Transactional(timeout = 3)
public class JobRepository {
    @PersistenceContext
    private EntityManager entityManager;

    public UUID create(JobType type, Map<String, String> arguments) {
        var job = JobEntity.pending(type, arguments, databaseNow());
        entityManager.persist(job);
        return job.getId();
    }

    @Transactional(readOnly = true, timeout = 3)
    public Optional<JobStatus> find(UUID id) {
        return Optional.ofNullable(entityManager.find(JobEntity.class, id)).map(job -> {
            Object result = switch (job.getStatus()) {
                case COMPLETED -> new UpdatedMoviesCount(job.getUpdatedCount());
                case FAILED -> new JobError(job.getErrorMessage());
                default -> null;
            };
            return new JobStatus(job.getId(), job.getStatus(), result);
        });
    }

    public Optional<ClaimedJob> claim(Duration lease) {
        if (lease.isZero() || lease.isNegative()) throw new IllegalArgumentException("Lease must be positive");
        var now = databaseNow();
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(JobEntity.class);
        var job = query.from(JobEntity.class);

        query.select(job).where(
                job.get("status").in(JobState.PENDING, JobState.RUNNING),
                builder.lessThanOrEqualTo(job.get("nextAttemptAt"), now),
                builder.or(builder.isNull(job.get("leaseUntil")), builder.lessThanOrEqualTo(job.get("leaseUntil"), now))
        ).orderBy(builder.asc(job.get("nextAttemptAt")), builder.asc(job.get("createdAt")), builder.asc(job.get("id")));

        var candidate = entityManager.createQuery(query)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .setHint(SpecHints.HINT_SPEC_LOCK_TIMEOUT, LockOptions.SKIP_LOCKED)
                .setMaxResults(1)
                .getResultList().stream().findFirst();

        return candidate.map(entity -> {
            var token = UUID.randomUUID();
            entity.claim(token, now, lease);
            var command = new OperationCommand(entity.getId(), entity.getType(), Map.copyOf(entity.getArguments()));
            return new ClaimedJob(command, token, entity.getAttempts());
        });
    }

    public boolean complete(ClaimedJob job, long count) {
        return ownedJob(job).map(entity -> {
            entity.complete(count, databaseNow());
            return true;
        }).orElse(false);
    }

    public boolean fail(ClaimedJob job, String error) {
        return ownedJob(job).map(entity -> {
            entity.fail(error, databaseNow());
            return true;
        }).orElse(false);
    }

    public boolean retry(ClaimedJob job, String error, Duration delay) {
        return ownedJob(job).map(entity -> {
            entity.retry(error, databaseNow(), delay);
            return true;
        }).orElse(false);
    }

    private Optional<JobEntity> ownedJob(ClaimedJob job) {
        return Optional.ofNullable(entityManager.find(JobEntity.class, job.command().jobId(), LockModeType.PESSIMISTIC_WRITE))
                .filter(entity -> entity.ownedBy(job.leaseToken()));
    }

    private Instant databaseNow() {
        var builder = entityManager.getCriteriaBuilder();
        var query = builder.createQuery(Instant.class);
        query.select(builder.function("clock_timestamp", Instant.class));
        return entityManager.createQuery(query).getSingleResult();
    }
}

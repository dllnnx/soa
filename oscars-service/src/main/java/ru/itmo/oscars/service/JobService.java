package ru.itmo.oscars.service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.springframework.data.domain.Limit;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.itmo.oscars.model.ClaimedJob;
import ru.itmo.oscars.model.JobAccepted;
import ru.itmo.oscars.model.JobError;
import ru.itmo.oscars.model.JobOutcome;
import ru.itmo.oscars.model.JobState;
import ru.itmo.oscars.model.JobStatus;
import ru.itmo.oscars.model.JobType;
import ru.itmo.oscars.model.MovieGenre;
import ru.itmo.oscars.model.OperationCommand;
import ru.itmo.oscars.model.UpdatedMoviesCount;
import ru.itmo.oscars.repository.JobEntity;
import ru.itmo.oscars.repository.JobRepository;

@Service
public class JobService {
    private static final List<JobState> CLAIMABLE_STATES = List.of(JobState.PENDING, JobState.RUNNING);

    private final JobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;

    public JobService(JobRepository jobRepository, JdbcTemplate jdbcTemplate) {
        this.jobRepository = jobRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(timeout = 3)
    public JobAccepted submit(JobType type, MovieGenre genre) {
        if (type == null || (type == JobType.REWARD_R && genre != null) || (type == JobType.RESET_BY_GENRE && genre == null)) {
            throw new InvalidParameterException("Неверные аргументы операции");
        }
        var job = jobRepository.save(JobEntity.pendingJob(type, genre == null ? Map.of() : Map.of("genre", genre.name()), databaseNow()));
        return new JobAccepted(job.getId(), "/oscar/jobs/" + job.getId() + "/status");
    }

    @Transactional(readOnly = true, timeout = 3)
    public JobStatus get(UUID id) {
        return jobRepository.findById(id).map(this::toStatus).orElseThrow(() -> new JobNotFoundException(id));
    }

    @Transactional(timeout = 3)
    public Optional<ClaimedJob> claim(Duration lease) {
        if (lease.isZero() || lease.isNegative()) throw new IllegalArgumentException("Lease must be positive");

        var now = databaseNow();
        return jobRepository.findClaimable(CLAIMABLE_STATES, now, Limit.of(1)).stream().findFirst()
                .map(job -> {
                    var token = UUID.randomUUID();
                    job.claim(token, now, lease);
                    var command = new OperationCommand(job.getId(), job.getType(), Map.copyOf(job.getArguments()));
                    return new ClaimedJob(command, token, job.getAttempts());
                });
    }

    @Transactional(timeout = 3)
    public boolean complete(ClaimedJob claimed, long count) {
        return applyIfOwned(claimed, job -> job.complete(count, databaseNow()));
    }

    @Transactional(timeout = 3)
    public boolean fail(ClaimedJob claimed, String error) {
        return applyIfOwned(claimed, job -> job.fail(error, databaseNow()));
    }

    @Transactional(timeout = 3)
    public boolean retry(ClaimedJob claimed, String error, Duration delay) {
        return applyIfOwned(claimed, job -> job.retry(error, databaseNow(), delay));
    }

    private boolean applyIfOwned(ClaimedJob claimed, Consumer<JobEntity> transition) {
        return jobRepository.findWithLockById(claimed.command().jobId())
                .filter(job -> job.ownedBy(claimed.leaseToken()))
                .map(job -> {
                    transition.accept(job);
                    return true;
                })
                .orElse(false);
    }

    private JobStatus toStatus(JobEntity job) {
        JobOutcome result = switch (job.getStatus()) {
            case COMPLETED -> new UpdatedMoviesCount(job.getUpdatedCount());
            case FAILED -> new JobError(job.getErrorMessage());
            default -> null;
        };
        return new JobStatus(job.getId(), job.getStatus(), result);
    }

    private Instant databaseNow() {
        return jdbcTemplate.queryForObject("select clock_timestamp()", OffsetDateTime.class).toInstant();
    }
}

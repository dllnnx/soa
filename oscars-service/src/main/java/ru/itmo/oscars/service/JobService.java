package ru.itmo.oscars.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.itmo.oscars.model.JobAccepted;
import ru.itmo.oscars.model.JobError;
import ru.itmo.oscars.model.JobOutcome;
import ru.itmo.oscars.model.JobStatus;
import ru.itmo.oscars.model.JobType;
import ru.itmo.oscars.model.MovieGenre;
import ru.itmo.oscars.model.OperationCommand;
import ru.itmo.oscars.model.UpdatedMoviesCount;
import ru.itmo.oscars.repository.JobEntity;
import ru.itmo.oscars.repository.JobRepository;

@Slf4j
@Service
public class JobService {
    private static final String JOBS_TOPIC = "movie-jobs";

    private final JobRepository jobRepository;
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<String, OperationCommand> kafkaTemplate;

    public JobService(JobRepository jobRepository, JdbcTemplate jdbcTemplate,
                      KafkaTemplate<String, OperationCommand> kafkaTemplate) {
        this.jobRepository = jobRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
    }

    public JobAccepted submit(JobType type, MovieGenre genre) {
        if (type == null || (type == JobType.REWARD_R && genre != null) || (type == JobType.RESET_BY_GENRE && genre == null)) {
            throw new InvalidParameterException("Неверные аргументы операции");
        }

        Map<String, String> arguments = genre == null ? Map.of() : Map.of("genre", genre.name());

        JobEntity job = jobRepository.save(JobEntity.pendingJob(type, arguments, databaseNow()));
        OperationCommand command = new OperationCommand(job.getId(), type, arguments);

        try {
            kafkaTemplate.send(JOBS_TOPIC, job.getId().toString(), command).get(5, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            rollbackSubmission(job.getId(), exception);
        } catch (Exception exception) {
            rollbackSubmission(job.getId(), exception);
        }

        return new JobAccepted(job.getId(), "/oscar/jobs/" + job.getId() + "/status");
    }

    private void rollbackSubmission(UUID jobId, Exception cause) {
        try {
            jobRepository.deleteById(jobId);
        } catch (RuntimeException deleteFailure) {
            cause.addSuppressed(deleteFailure);
        }
        log.warn("Kafka send failed, submission of job {} rolled back", jobId, cause);
        throw new BrokerUnavailableException();
    }

    @Transactional(readOnly = true, timeout = 3)
    public JobStatus get(UUID id) {
        return jobRepository.findById(id).map(this::toStatus).orElseThrow(() -> new JobNotFoundException(id));
    }

    @Transactional(timeout = 3)
    public boolean start(UUID jobId) {
        return jobRepository.findById(jobId)
                .map(job -> {
                    job.start(databaseNow());
                    return true;
                })
                .orElse(false);
    }

    @Transactional(timeout = 3)
    public void complete(UUID jobId, long count) {
        jobRepository.findById(jobId).ifPresent(job -> job.complete(count, databaseNow()));
    }

    @Transactional(timeout = 3)
    public void fail(UUID jobId, String error) {
        jobRepository.findById(jobId).ifPresent(job -> job.fail(error, databaseNow()));
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

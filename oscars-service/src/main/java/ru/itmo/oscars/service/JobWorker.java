package ru.itmo.oscars.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.itmo.oscars.client.MoviesClient;
import ru.itmo.oscars.client.MoviesServiceException;
import ru.itmo.oscars.model.ClaimedJob;

@Slf4j
@Component
public class JobWorker {
    private final JobService jobService;
    private final MoviesClient moviesClient;
    private final Duration lease;

    public JobWorker(JobService jobService, MoviesClient moviesClient, @Value("${jobs.lease-duration}") Duration lease) {
        if (lease.isZero() || lease.isNegative()) throw new IllegalArgumentException("Lease must be positive");
        this.jobService = jobService;
        this.moviesClient = moviesClient;
        this.lease = lease;
    }

    @Scheduled(fixedDelayString = "${jobs.poll-delay:1000}", initialDelayString = "${jobs.initial-delay:1000}")
    public void tick() {
        try {
            jobService.claim(lease).ifPresent(this::process);
        } catch (RuntimeException exception) {
            log.warn("Cannot process durable job; it will be recovered from PostgreSQL", exception);
        }
    }

    private void process(ClaimedJob job) {
        try {
            var result = moviesClient.execute(job.command());

            switch (result.status()) {
                case COMPLETED -> logIfLeaseLost(job, "COMPLETED", jobService.complete(job, result.updatedCount()));
                case FAILED -> logIfLeaseLost(job, "FAILED", jobService.fail(job, result.errorMessage()));
                case PENDING -> logIfLeaseLost(job, "RETRY", jobService.retry(job, result.errorMessage(), retryDelay(job.attempts())));
            }

        } catch (MoviesServiceException exception) {
            if (exception.retryable())
                logIfLeaseLost(job, "RETRY", jobService.retry(job, exception.getMessage(), retryDelay(job.attempts())));
            else
                logIfLeaseLost(job, "FAILED", jobService.fail(job, exception.getMessage()));
        }
    }

    private void logIfLeaseLost(ClaimedJob job, String outcome, boolean applied) {
        if (!applied) {
            log.warn("Lease for job {} expired before its {} outcome could be stored; the outcome is dropped and the job will be reprocessed", job.command().jobId(), outcome);
        }
    }

    static Duration retryDelay(int attempt) {
        long baseMillis = Math.min(60_000L, 1000L << Math.min(6, Math.max(0, attempt - 1)));
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(Math.max(1, baseMillis / 2),
                baseMillis + 1));
    }
}

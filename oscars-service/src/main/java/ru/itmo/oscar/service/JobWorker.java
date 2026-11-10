package ru.itmo.oscar.service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.itmo.oscar.client.MoviesClient;
import ru.itmo.oscar.client.MoviesServiceException;
import ru.itmo.oscar.repository.ClaimedJob;
import ru.itmo.oscar.repository.JobRepository;

@Component
public class JobWorker {
    private static final Logger LOG = LoggerFactory.getLogger(JobWorker.class);
    private final JobRepository jobs;
    private final MoviesClient movies;
    private final Duration lease;

    public JobWorker(JobRepository jobs, MoviesClient movies, @Value("${jobs.lease-duration}") Duration lease) {
        if (lease.isZero() || lease.isNegative()) throw new IllegalArgumentException("Lease must be positive");
        this.jobs = jobs;
        this.movies = movies;
        this.lease = lease;
    }

    @Scheduled(fixedDelayString = "${jobs.poll-delay:1000}", initialDelayString = "${jobs.initial-delay:1000}")
    public void tick() {
        try {
            jobs.claim(lease).ifPresent(this::process);
        } catch (RuntimeException exception) {
            LOG.warn("Cannot process durable job; it will be recovered from PostgreSQL", exception);
        }
    }

    private void process(ClaimedJob job) {
        try {
            var result = movies.execute(job.command());

            switch (result.status()) {
                case COMPLETED -> jobs.complete(job, result.updatedCount());
                case FAILED -> jobs.fail(job, result.errorMessage());
                case PENDING -> jobs.retry(job, result.errorMessage(), retryDelay(job.attempts()));
            }

        } catch (MoviesServiceException exception) {
            if (exception.retryable()) jobs.retry(job, exception.getMessage(), retryDelay(job.attempts()));
            else jobs.fail(job, exception.getMessage());
        }
    }


    static Duration retryDelay(int attempt) {
        long baseMillis = Math.min(60_000L, 1000L << Math.min(6, Math.max(0, attempt - 1)));
        return Duration.ofMillis(ThreadLocalRandom.current().nextLong(Math.max(1, baseMillis / 2),
                baseMillis + 1));
    }
}

package ru.itmo.oscar.service;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.LongSupplier;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import ru.itmo.oscar.model.JobAccepted;
import ru.itmo.oscar.model.JobStatus;

@Service
public class JobService {

    private final ConcurrentMap<UUID, JobStatus> jobs = new ConcurrentHashMap<>();
    private final ReentrantLock operationLock = new ReentrantLock();
    private final ThreadPoolTaskExecutor executor;

    public JobService(ThreadPoolTaskExecutor jobExecutor) {
        this.executor = jobExecutor;
    }

    public JobAccepted submit(LongSupplier operation) {
        var jobId = UUID.randomUUID();
        jobs.put(jobId, JobStatus.pending(jobId));
        try {
            executor.execute(() -> run(jobId, operation));
        } catch (RuntimeException exception) {
            jobs.computeIfPresent(jobId, (id, status) -> status.failed(errorMessage(exception)));
        }
        return new JobAccepted(jobId, "/oscar/jobs/" + jobId + "/status");
    }

    public JobStatus get(UUID jobId) {
        var status = jobs.get(jobId);
        if (status == null) {
            throw new JobNotFoundException(jobId);
        }
        return status;
    }

    private void run(UUID jobId, LongSupplier operation) {
        operationLock.lock();
        try {
            jobs.computeIfPresent(jobId, (id, status) -> status.running());
            var updatedCount = operation.getAsLong();
            jobs.computeIfPresent(jobId, (id, status) -> status.completed(updatedCount));
        } catch (Exception exception) {
            jobs.computeIfPresent(jobId, (id, status) -> status.failed(errorMessage(exception)));
        } finally {
            operationLock.unlock();
        }
    }

    private String errorMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}

package ru.itmo.oscar.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobStatus(UUID jobId, JobState status, Object result) {

    public static JobStatus pending(UUID jobId) {
        return new JobStatus(jobId, JobState.PENDING, null);
    }

    public JobStatus running() {
        return new JobStatus(jobId, JobState.RUNNING, null);
    }

    public JobStatus completed(long updatedCount) {
        return new JobStatus(jobId, JobState.COMPLETED, new UpdatedMoviesCount(updatedCount));
    }

    public JobStatus failed(String message) {
        return new JobStatus(jobId, JobState.FAILED, new JobError(message));
    }
}

package ru.itmo.movies.operation;

import java.util.UUID;

public class OperationResult {
    public UUID jobId;
    public OperationStatus status;
    public Long updatedCount;
    public String errorCode;
    public String errorMessage;
    public boolean retryable;

    public OperationResult(UUID jobId, OperationStatus status, Long updatedCount, String errorCode,
                           String errorMessage, boolean retryable) {
        this.jobId = jobId;
        this.status = status;
        this.updatedCount = updatedCount;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryable = retryable;
    }
}

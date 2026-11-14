package ru.itmo.oscars.model;

import java.util.UUID;

public record OperationResult(UUID jobId, OperationStatus status, Long updatedCount,
                              String errorCode, String errorMessage, boolean retryable) {
}

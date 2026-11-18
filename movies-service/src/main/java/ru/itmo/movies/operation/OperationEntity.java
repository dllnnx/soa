package ru.itmo.movies.operation;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movie_operation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OperationEntity {

    @Id
    @Column(name = "job_id")
    private UUID jobId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OperationCommand.Type type;

    @Column(length = 64)
    private String genre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private OperationStatus status;

    @Column(name = "updated_count")
    private Long updatedCount;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void complete(long count) {
        status = OperationStatus.COMPLETED;
        updatedCount = count;
    }

    public void fail(String code, String message) {
        status = OperationStatus.FAILED;
        errorCode = code;
        errorMessage = message;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public OperationResult result() {
        return new OperationResult(jobId, status, updatedCount, errorCode, errorMessage,
                status == OperationStatus.PENDING);
    }
}

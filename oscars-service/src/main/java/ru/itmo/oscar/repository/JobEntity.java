package ru.itmo.oscar.repository;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import ru.itmo.oscar.model.JobState;
import ru.itmo.oscar.model.JobType;

@Entity
@Table(name = "oscar_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobEntity {
    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private JobType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, String> arguments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private JobState status;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "lease_token")
    private UUID leaseToken;
    @Column(name = "lease_until")
    private Instant leaseUntil;
    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;
    @Column(name = "updated_count")
    private Long updatedCount;
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Column(name = "started_at")
    private Instant startedAt;
    @Column(name = "finished_at")
    private Instant finishedAt;

    static JobEntity pending(JobType type, Map<String, String> arguments, Instant now) {
        var job = new JobEntity();
        job.id = UUID.randomUUID();
        job.type = type;
        job.arguments = new LinkedHashMap<>(arguments);
        job.status = JobState.PENDING;
        job.createdAt = now;
        job.updatedAt = now;
        job.nextAttemptAt = now;
        return job;
    }

    void claim(UUID token, Instant now, Duration lease) {
        status = JobState.RUNNING;
        attempts = Math.incrementExact(attempts);
        leaseToken = token;
        leaseUntil = now.plus(lease);
        if (startedAt == null) startedAt = now;
        updatedAt = now;
    }

    boolean ownedBy(UUID token) {
        return status == JobState.RUNNING && token.equals(leaseToken);
    }

    void complete(long count, Instant now) {
        status = JobState.COMPLETED;
        updatedCount = count;
        errorMessage = null;
        finishedAt = now;
        release(now);
    }

    void fail(String message, Instant now) {
        status = JobState.FAILED;
        errorMessage = message;
        finishedAt = now;
        release(now);
    }

    void retry(String message, Instant now, Duration delay) {
        errorMessage = message;
        nextAttemptAt = now.plus(delay);
        release(now);
    }

    private void release(Instant now) {
        leaseToken = null;
        leaseUntil = null;
        updatedAt = now;
    }
}

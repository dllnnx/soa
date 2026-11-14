package ru.itmo.oscars.repository;

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
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

import ru.itmo.oscars.model.JobState;
import ru.itmo.oscars.model.JobType;

@Entity
@Table(name = "oscar_job")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JobEntity {
    @Id
    @UuidGenerator
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

    public static JobEntity pendingJob(JobType type, Map<String, String> arguments, Instant now) {
        var job = new JobEntity();
        job.type = type;
        job.arguments = new LinkedHashMap<>(arguments);
        job.status = JobState.PENDING;
        job.createdAt = now;
        job.updatedAt = now;
        return job;
    }

    public void start(Instant now) {
        status = JobState.RUNNING;
        attempts = Math.incrementExact(attempts);
        if (startedAt == null) startedAt = now;
        updatedAt = now;
    }

    public void complete(long count, Instant now) {
        status = JobState.COMPLETED;
        updatedCount = count;
        errorMessage = null;
        finishedAt = now;
        updatedAt = now;
    }

    public void fail(String message, Instant now) {
        status = JobState.FAILED;
        errorMessage = message;
        finishedAt = now;
        updatedAt = now;
    }
}

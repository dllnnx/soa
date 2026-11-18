package ru.itmo.movies.operation;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OperationRepository extends JpaRepository<OperationEntity, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO movie_operation(job_id, type, genre) VALUES (:jobId, :type, :genre)
            ON CONFLICT (job_id) DO NOTHING
            """, nativeQuery = true)
    void register(@Param("jobId") UUID jobId, @Param("type") String type, @Param("genre") String genre);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select operation from OperationEntity operation where operation.jobId = :jobId")
    Optional<OperationEntity> findByJobIdForUpdate(@Param("jobId") UUID jobId);
}

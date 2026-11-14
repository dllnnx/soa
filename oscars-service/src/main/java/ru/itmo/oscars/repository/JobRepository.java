package ru.itmo.oscars.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.hibernate.jpa.SpecHints;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import ru.itmo.oscars.model.JobState;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = SpecHints.HINT_SPEC_LOCK_TIMEOUT, value = "-2"))
    @Query("""
            select j from JobEntity j
            where j.status in :states
              and j.nextAttemptAt <= :now
              and (j.leaseUntil is null or j.leaseUntil <= :now)
            order by j.nextAttemptAt asc, j.createdAt asc, j.id asc
            """)
    List<JobEntity> findClaimable(@Param("states") Collection<JobState> states, @Param("now") Instant now, Limit limit);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<JobEntity> findWithLockById(UUID id);
}

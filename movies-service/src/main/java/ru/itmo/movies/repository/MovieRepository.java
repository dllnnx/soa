package ru.itmo.movies.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.itmo.movies.model.MpaaRating;

public interface MovieRepository extends JpaRepository<MovieEntity, Long>, JpaSpecificationExecutor<MovieEntity> {

    @Query("select avg(m.budget) from MovieEntity m where m.budget is not null")
    Double averageBudget();

    @Query("select sum(m.oscarsCount) from MovieEntity m")
    Long totalOscars();

    @Modifying
    @Query("delete from MovieEntity m where m.mpaaRating = :rating")
    long deleteByMpaaRating(@Param("rating") MpaaRating rating);

    Optional<MovieEntity> findFirstByScreenwriterNameOrderByIdAsc(String screenwriterName);
}

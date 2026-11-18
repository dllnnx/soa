package ru.itmo.movies.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.model.MovieGenre;

public interface MovieRepository extends JpaRepository<MovieEntity, Long>, JpaSpecificationExecutor<MovieEntity> {

    @Query("select avg(m.budget) from MovieEntity m where m.budget is not null")
    Double averageBudget();

    @Query("select sum(m.oscarsCount) from MovieEntity m")
    Long totalOscars();

    @Modifying
    @Query("delete from MovieEntity m where m.mpaaRating = :rating")
    int deleteByMpaaRating(@Param("rating") MpaaRating rating);

    Optional<MovieEntity> findFirstByScreenwriterNameOrderByIdAsc(String screenwriterName);

    @Modifying
    @Query("""
            update MovieEntity movie set movie.oscarsCount = movie.oscarsCount + 1
            where movie.mpaaRating = :rating
            """)
    int rewardByMpaaRating(@Param("rating") MpaaRating rating);

    @Modifying
    @Query("""
            update MovieEntity target set target.oscarsCount = 0
            where target.oscarsCount > 0 and target.screenwriterName is not null
              and exists (
                  select 1 from MovieEntity source
                  where source.genre = :genre and source.screenwriterName = target.screenwriterName
            )
            """)
    int resetOscarsByGenre(@Param("genre") MovieGenre genre);
}

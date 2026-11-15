package ru.itmo.movies.service;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.mapper.MovieMapper;
import ru.itmo.movies.model.AverageBudget;
import ru.itmo.movies.model.DeleteResult;
import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.model.TotalOscars;
import ru.itmo.movies.repository.MovieEntity;
import ru.itmo.movies.repository.MovieRepository;
import ru.itmo.movies.repository.MovieSpecifications;
import ru.itmo.movies.repository.SortOrder;

@Service
@Transactional
@AllArgsConstructor
public class MovieService {

    private final MovieRepository repository;
    private final MovieMapper mapper;

    public Movie create(MovieInput input) {
        MovieEntity entity = mapper.toEntity(input);
        return mapper.toModel(repository.save(entity));
    }

    public Movie getById(int id) {
        MovieEntity entity = repository.findById((long) id).orElseThrow(() -> movieNotFound(id));
        return mapper.toModel(entity);
    }

    public Movie update(int id, MovieInput input) {
        MovieEntity entity = repository.findById((long) id).orElseThrow(() -> movieNotFound(id));
        mapper.updateEntity(input, entity);
        return mapper.toModel(entity);
    }

    public void deleteById(int id) {
        MovieEntity entity = repository.findById((long) id).orElseThrow(() -> movieNotFound(id));
        repository.delete(entity);
    }

    public MoviePage filter(MovieFilter filter, int page, int size, SortOrder sort) {
        var pageable = PageRequest.of(page, size,
                Sort.by(sort.ascending() ? Sort.Direction.ASC : Sort.Direction.DESC, sort.key().getAttribute())
                        .and(Sort.by(Sort.Direction.ASC, "id")));
        var result = repository.findAll(MovieSpecifications.byFilter(filter), pageable);
        return mapper.toPage(result.getContent(), page, size, result.getTotalElements());
    }

    public AverageBudget getAverageBudget() {
        Double average = repository.averageBudget();
        AverageBudget result = new AverageBudget();
        result.setAverageBudget(average == null ? 0 : average);
        return result;
    }

    public TotalOscars getTotalOscars() {
        Long total = repository.totalOscars();
        TotalOscars result = new TotalOscars();
        result.setTotalOscars(String.valueOf(total == null ? 0L : total));
        return result;
    }

    public DeleteResult deleteByMpaaRating(MpaaRating rating) {
        DeleteResult result = new DeleteResult();
        result.setDeletedCount(repository.deleteByMpaaRating(rating));
        return result;
    }

    public void deleteByScreenwriter(String name) {
        repository.findFirstByScreenwriterNameOrderByIdAsc(name).ifPresentOrElse(repository::delete, () -> {
            throw new ApiException(ErrorCode.NO_MATCHING_MOVIE, "Фильм с указанным сценаристом не найден");
        });
    }

    private static ApiException movieNotFound(int id) {
        return new ApiException(ErrorCode.MOVIE_NOT_FOUND, "Фильм с id=" + id + " не найден");
    }
}

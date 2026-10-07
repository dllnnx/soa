package ru.itmo.movies.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.model.AverageBudget;
import ru.itmo.movies.model.DeleteResult;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.mapper.MovieMapper;
import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.repository.MovieEntity;
import ru.itmo.movies.repository.MovieEntityPage;
import ru.itmo.movies.repository.MovieRepository;
import ru.itmo.movies.repository.SortOrder;

@ApplicationScoped
@Transactional
public class MovieService {

    @Inject
    private MovieRepository repository;

    @Inject
    private MovieMapper mapper;

    public Movie create(MovieInput input) {
        MovieEntity entity = mapper.toEntity(input);
        return mapper.toModel(repository.insert(entity));
    }

    public Movie get(int id) {
        MovieEntity entity = repository.find(id).orElseThrow(() -> movieNotFound(id));
        return mapper.toModel(entity);
    }

    public Movie update(int id, MovieInput input) {
        MovieEntity entity = repository.find(id).orElseThrow(() -> movieNotFound(id));
        mapper.updateEntity(input, entity);
        return mapper.toModel(entity);
    }

    public void delete(int id) {
        MovieEntity entity = repository.find(id).orElseThrow(() -> movieNotFound(id));
        repository.delete(entity);
    }

    public MoviePage filter(MovieFilter filter, int page, int size, SortOrder sort) {
        MovieEntityPage result = repository.filter(filter, page, size, sort);
        return mapper.toPage(
                result.content(), result.page(), result.size(), result.totalElements());
    }

    public AverageBudget getAverageBudget() {
        AverageBudget result = new AverageBudget();
        result.setAverageBudget(repository.averageBudget());
        return result;
    }

    public DeleteResult deleteByMpaaRating(MpaaRating rating) {
        DeleteResult result = new DeleteResult();
        result.setDeletedCount(repository.deleteByMpaaRating(rating));
        return result;
    }

    public void deleteByScreenwriter(String name) {
        if (!repository.deleteByScreenwriter(name)) {
            throw new ApiException(ErrorCode.NO_MATCHING_MOVIE, "Фильм с указанным сценаристом не найден");
        }
    }

    private static ApiException movieNotFound(int id) {
        return new ApiException(ErrorCode.MOVIE_NOT_FOUND, "Фильм с id=" + id + " не найден");
    }
}

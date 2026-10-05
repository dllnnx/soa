package ru.itmo.movies.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.model.AverageBudget;
import ru.itmo.movies.model.DeleteResult;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.repository.MovieRepository;
import ru.itmo.movies.repository.SortOrder;
import ru.itmo.movies.validation.MovieValidator;

@ApplicationScoped
public class MovieService {

    @Inject
    private MovieRepository repository;

    public Movie create(MovieInput input) {
        MovieValidator.validateForCreate(input);
        return repository.insert(input);
    }

    public Movie get(int id) {
        return repository.find(id).orElseThrow(() -> movieNotFound(id));
    }

    public Movie update(int id, MovieInput input) {
        MovieValidator.validateForUpdate(input);
        Movie updated = repository.update(id, input);
        if (updated == null) {
            throw movieNotFound(id);
        }
        return updated;
    }

    public void delete(int id) {
        if (!repository.delete(id)) {
            throw movieNotFound(id);
        }
    }

    public MoviePage filter(MovieFilter filter, int page, int size, SortOrder sort) {
        MovieValidator.validateFilter(filter);
        return repository.filter(filter, page, size, sort);
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

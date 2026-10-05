package ru.itmo.oscar.service;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ru.itmo.oscar.client.MoviesClient;
import ru.itmo.oscar.model.MovieGenre;
import ru.itmo.oscar.model.MpaaRating;

@Service
public class OscarService {

    private final MoviesClient moviesClient;

    public OscarService(MoviesClient moviesClient) {
        this.moviesClient = moviesClient;
    }

    public long rewardRMovies() {
        var movies = moviesClient.findByMpaaRating(MpaaRating.R);
        for (var movie : movies) {
            moviesClient.update(movie.id(), movie.withOscarsCount(Math.addExact(movie.oscarsCount(), 1)));
        }
        return movies.size();
    }

    public long resetOscarsByGenre(MovieGenre genre) {
        Set<String> screenwriters = moviesClient.findByGenre(genre).stream()
                .map(movie -> movie.screenwriter() == null ? null : movie.screenwriter().name())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (screenwriters.isEmpty()) {
            return 0;
        }

        var movies = moviesClient.findAll().stream()
                .filter(movie -> movie.screenwriter() != null)
                .filter(movie -> screenwriters.contains(movie.screenwriter().name()))
                .filter(movie -> movie.oscarsCount() > 0)
                .toList();
        for (var movie : movies) {
            moviesClient.update(movie.id(), movie.withOscarsCount(0));
        }
        return movies.size();
    }
}

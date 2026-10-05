package ru.itmo.oscar.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import ru.itmo.oscar.model.Movie;
import ru.itmo.oscar.model.MovieGenre;
import ru.itmo.oscar.model.MovieInput;
import ru.itmo.oscar.model.MoviePage;
import ru.itmo.oscar.model.MpaaRating;

@Component
public class MoviesClient {

    private static final int PAGE_SIZE = 100;

    private final RestClient restClient;

    public MoviesClient(RestClient moviesRestClient) {
        this.restClient = moviesRestClient;
    }

    public List<Movie> findByMpaaRating(MpaaRating rating) {
        return findAll(Map.of("mpaaRating", rating));
    }

    public List<Movie> findByGenre(MovieGenre genre) {
        return findAll(Map.of("genres", List.of(genre)));
    }

    public List<Movie> findAll() {
        return findAll(Map.of());
    }

    public void update(Integer id, MovieInput movie) {
        try {
            restClient.put()
                    .uri("/movies/{id}", id)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(movie)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw downstreamError(exception);
        } catch (RuntimeException exception) {
            throw new MoviesServiceException("Сервис movies недоступен: " + exception.getMessage());
        }
    }

    private List<Movie> findAll(Map<String, ?> filter) {
        var movies = new ArrayList<Movie>();
        var pageNumber = 0;
        while (true) {
            MoviePage page;
            try {
                int requestedPage = pageNumber;
                page = restClient.post()
                        .uri(builder -> builder
                                .path("/movies/filter")
                                .queryParam("page", requestedPage)
                                .queryParam("size", PAGE_SIZE)
                                .build())
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(filter)
                        .retrieve()
                        .body(MoviePage.class);
            } catch (RestClientResponseException exception) {
                throw downstreamError(exception);
            } catch (RuntimeException exception) {
                throw new MoviesServiceException("Сервис movies недоступен: " + exception.getMessage());
            }
            if (page == null || page.content() == null) {
                throw new MoviesServiceException("Сервис movies вернул пустой ответ");
            }
            movies.addAll(page.content());
            pageNumber++;
            if (pageNumber >= page.totalPages()) {
                return movies;
            }
        }
    }

    private MoviesServiceException downstreamError(RestClientResponseException exception) {
        var body = exception.getResponseBodyAsString();
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }
        var suffix = body.isBlank() ? "" : ": " + body;
        return new MoviesServiceException("Сервис movies вернул HTTP "
                + exception.getStatusCode().value() + suffix);
    }
}

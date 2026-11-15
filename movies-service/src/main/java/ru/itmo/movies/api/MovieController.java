package ru.itmo.movies.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.service.MovieService;

@Validated
@RestController
@RequestMapping("movies")
public class MovieController {

    private final MovieService service;

    public MovieController(MovieService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Movie> create(@NotNull @Valid @RequestBody MovieInput input) {
        Movie created = service.create(input);
        return ResponseEntity.status(201).body(created);
    }

    @GetMapping("{movie-id}")
    public Movie get(@PathVariable("movie-id") @Min(1) int movieId) {
        return service.getById(movieId);
    }

    @PutMapping(value = "{movie-id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Movie update(@PathVariable("movie-id") @Min(1) int movieId, @NotNull @Valid @RequestBody MovieInput input) {
        return service.update(movieId, input);
    }

    @DeleteMapping("{movie-id}")
    public ResponseEntity<Void> delete(@PathVariable("movie-id") @Min(1) int movieId) {
        service.deleteById(movieId);
        return ResponseEntity.noContent().build();
    }
}

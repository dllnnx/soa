package ru.itmo.movies.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.repository.SortOrder;
import ru.itmo.movies.service.MovieService;

@Validated
@RestController
@RequestMapping("movies/filter")
public class MovieFilterController {

    private final MovieService service;

    public MovieFilterController(MovieService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public MoviePage filter(
            @Valid @RequestBody MovieFilter filter,
            @RequestParam(value = "page", defaultValue = "0") @Min(0) @Max(100000) int page,
            @RequestParam(value = "size", defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(value = "sort", defaultValue = "id,asc") String sort) {
        return service.filter(filter, page, size, SortOrder.parse(sort));
    }
}

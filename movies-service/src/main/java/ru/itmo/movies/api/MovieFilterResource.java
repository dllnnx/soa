package ru.itmo.movies.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.repository.SortOrder;
import ru.itmo.movies.service.MovieService;

@Path("movies/filter")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MovieFilterResource {

    @Inject
    private MovieService service;

    @POST
    public MoviePage filter(
            @NotNull @Valid MovieFilter filter,
            @QueryParam("page") @DefaultValue("0") @Min(0) @Max(100000) int page,
            @QueryParam("size") @DefaultValue("10") @Min(1) @Max(100) int size,
            @QueryParam("sort") @DefaultValue("id,asc") SortOrder sort) {
        return service.filter(filter, page, size, sort);
    }
}

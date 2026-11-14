package ru.itmo.movies.api;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.service.MovieService;

@Path("movies")
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    private MovieService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(@NotNull @Valid MovieInput input) {
        Movie created = service.create(input);
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("{movie-id}")
    public Movie get(@PathParam("movie-id") @Min(1) int movieId) {
        return service.get(movieId);
    }

    @PUT
    @Path("{movie-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Movie update(@PathParam("movie-id") @Min(1) int movieId, @NotNull @Valid MovieInput input) {
        return service.update(movieId, input);
    }

    @DELETE
    @Path("{movie-id}")
    public Response delete(@PathParam("movie-id") @Min(1) int movieId) {
        service.delete(movieId);
        return Response.noContent().build();
    }
}

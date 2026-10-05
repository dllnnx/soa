package ru.itmo.movies.api;

import jakarta.inject.Inject;
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
import ru.itmo.movies.parse.BodyParser;
import ru.itmo.movies.parse.PathIds;
import ru.itmo.movies.service.MovieService;

@Path("movies")
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    private MovieService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(String body) {
        Movie created = service.create(BodyParser.parse(body, MovieInput.class));
        return Response.status(Response.Status.CREATED).entity(created).build();
    }

    @GET
    @Path("{movie-id}")
    public Movie get(@PathParam("movie-id") String rawId) {
        return service.get(PathIds.movieId(rawId));
    }

    @PUT
    @Path("{movie-id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Movie update(@PathParam("movie-id") String rawId, String body) {
        return service.update(PathIds.movieId(rawId), BodyParser.parse(body, MovieInput.class));
    }

    @DELETE
    @Path("{movie-id}")
    public Response delete(@PathParam("movie-id") String rawId) {
        service.delete(PathIds.movieId(rawId));
        return Response.noContent().build();
    }
}

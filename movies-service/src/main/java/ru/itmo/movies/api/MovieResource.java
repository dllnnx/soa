package ru.itmo.movies.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ru.itmo.movies.service.MovieService;

@Path("movies")
@Produces(MediaType.APPLICATION_JSON)
public class MovieResource {

    @Inject
    private MovieService movieService;

    @GET
    @Path("average-budget")
    public Response averageBudget() {
        return Response.ok().build();
    }
}

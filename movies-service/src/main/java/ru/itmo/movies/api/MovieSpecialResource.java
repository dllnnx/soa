package ru.itmo.movies.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import ru.itmo.movies.model.AverageBudget;
import ru.itmo.movies.model.DeleteResult;
import ru.itmo.movies.parse.QueryParams;
import ru.itmo.movies.service.MovieService;

@Path("movies")
@Produces(MediaType.APPLICATION_JSON)
public class MovieSpecialResource {

    @Inject
    private MovieService service;

    @GET
    @Path("average-budget")
    public AverageBudget getAverageBudget() {
        return service.getAverageBudget();
    }

    @DELETE
    @Path("by-mpaa-rating")
    public DeleteResult deleteByMpaaRating(UriInfo uri) {
        QueryParams.allow(uri, "mpaa-rating");
        return service.deleteByMpaaRating(QueryParams.mpaaRating(uri));
    }

    @DELETE
    @Path("by-screenwriter")
    public Response deleteByScreenwriter(UriInfo uri) {
        QueryParams.allow(uri, "name");
        service.deleteByScreenwriter(QueryParams.screenwriterName(uri));
        return Response.noContent().build();
    }
}

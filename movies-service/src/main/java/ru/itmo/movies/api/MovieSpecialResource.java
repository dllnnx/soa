package ru.itmo.movies.api;

import jakarta.inject.Inject;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ru.itmo.movies.model.AverageBudget;
import ru.itmo.movies.model.DeleteResult;
import ru.itmo.movies.model.MpaaRating;
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
    public DeleteResult deleteByMpaaRating(
            @QueryParam("mpaa-rating") @NotNull MpaaRating mpaaRating) {
        return service.deleteByMpaaRating(mpaaRating);
    }

    @DELETE
    @Path("by-screenwriter")
    public Response deleteByScreenwriter(
            @QueryParam("name") @NotNull
            @Pattern(regexp = "^[a-zA-Zа-яА-ЯЁё0-9 .,:!?()&-]{1,255}$") String name) {
        service.deleteByScreenwriter(name);
        return Response.noContent().build();
    }
}

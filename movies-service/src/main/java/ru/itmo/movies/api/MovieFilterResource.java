package ru.itmo.movies.api;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.parse.BodyParser;
import ru.itmo.movies.parse.QueryParams;
import ru.itmo.movies.service.MovieService;

@Path("movies/filter")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MovieFilterResource {

    @Inject
    private MovieService service;

    @POST
    public MoviePage filter(String body, UriInfo uri) {
        QueryParams.allow(uri, "page", "size", "sort");
        return service.filter(
                BodyParser.parse(body, MovieFilter.class),
                QueryParams.page(uri),
                QueryParams.size(uri),
                QueryParams.sort(uri));
    }
}

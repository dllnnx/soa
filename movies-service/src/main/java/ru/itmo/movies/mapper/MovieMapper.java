package ru.itmo.movies.mapper;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;

import ru.itmo.movies.model.Coordinates;
import ru.itmo.movies.model.Country;
import ru.itmo.movies.model.EyeColor;
import ru.itmo.movies.model.Location;
import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieGenre;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.model.Person;
import ru.itmo.movies.repository.MovieEntity;

@ApplicationScoped
public class MovieMapper {

    public MovieEntity toEntity(MovieInput input) {
        MovieEntity entity = new MovieEntity();
        updateEntity(input, entity);
        return entity;
    }

    public void updateEntity(MovieInput input, MovieEntity entity) {
        entity.setName(input.getName());
        entity.setCoordinateX(input.getCoordinates().getX());
        entity.setCoordinateY(input.getCoordinates().getY());
        entity.setOscarsCount(input.getOscarsCount());
        entity.setBudget(input.getBudget());
        entity.setGenre(input.getGenre().name());
        entity.setMpaaRating(input.getMpaaRating().name());
        updateScreenwriter(input.getScreenwriter(), entity);
    }

    public Movie toModel(MovieEntity entity) {
        Movie movie = new Movie();
        movie.setId(entity.getId().intValue());
        movie.setName(entity.getName());

        Coordinates coordinates = new Coordinates();
        coordinates.setX(entity.getCoordinateX());
        coordinates.setY(entity.getCoordinateY());
        movie.setCoordinates(coordinates);

        movie.setCreationDate(entity.getCreationDate());
        movie.setOscarsCount(entity.getOscarsCount());
        movie.setBudget(entity.getBudget());
        movie.setGenre(MovieGenre.valueOf(entity.getGenre()));
        movie.setMpaaRating(MpaaRating.valueOf(entity.getMpaaRating()));
        movie.setScreenwriter(toPerson(entity));
        return movie;
    }

    public MoviePage toPage(List<MovieEntity> entities, int page, int size, long totalElements) {
        MoviePage result = new MoviePage();
        result.setPage(page);
        result.setSize(size);
        result.setTotalElements(totalElements);
        result.setTotalPages((int) Math.ceil(totalElements / (double) size));
        result.setContent(entities.stream().map(this::toModel).toList());
        return result;
    }

    private void updateScreenwriter(Person person, MovieEntity entity) {
        if (person == null) {
            entity.setScreenwriterName(null);
            entity.setScreenwriterHeight(null);
            entity.setScreenwriterEyeColor(null);
            entity.setScreenwriterHairColor(null);
            entity.setScreenwriterNationality(null);
            entity.setScreenwriterLocationX(null);
            entity.setScreenwriterLocationY(null);
            entity.setScreenwriterLocationZ(null);
            return;
        }
        entity.setScreenwriterName(person.getName());
        entity.setScreenwriterHeight(person.getHeight());
        entity.setScreenwriterEyeColor(person.getEyeColor().name());
        entity.setScreenwriterHairColor(
                person.getHairColor() == null ? null : person.getHairColor().getValue());
        entity.setScreenwriterNationality(person.getNationality().name());
        entity.setScreenwriterLocationX(person.getLocation().getX());
        entity.setScreenwriterLocationY(person.getLocation().getY());
        entity.setScreenwriterLocationZ(person.getLocation().getZ());
    }

    private Person toPerson(MovieEntity entity) {
        if (entity.getScreenwriterName() == null) {
            return null;
        }
        Person person = new Person();
        person.setName(entity.getScreenwriterName());
        person.setHeight(entity.getScreenwriterHeight());
        person.setEyeColor(EyeColor.valueOf(entity.getScreenwriterEyeColor()));
        person.setHairColor(entity.getScreenwriterHairColor() == null
                ? null : Person.HairColorEnum.fromValue(entity.getScreenwriterHairColor()));
        person.setNationality(Country.valueOf(entity.getScreenwriterNationality()));

        Location location = new Location();
        location.setX(entity.getScreenwriterLocationX());
        location.setY(entity.getScreenwriterLocationY());
        location.setZ(entity.getScreenwriterLocationZ());
        person.setLocation(location);
        return person;
    }
}

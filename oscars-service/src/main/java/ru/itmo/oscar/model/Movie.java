package ru.itmo.oscar.model;

import java.time.ZonedDateTime;

public record Movie(
        Integer id,
        String name,
        Coordinates coordinates,
        ZonedDateTime creationDate,
        long oscarsCount,
        Integer budget,
        MovieGenre genre,
        MpaaRating mpaaRating,
        Person screenwriter) {

    public MovieInput withOscarsCount(long value) {
        return new MovieInput(name, coordinates, value, budget, genre, mpaaRating, screenwriter);
    }
}

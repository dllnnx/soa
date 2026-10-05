package ru.itmo.oscar.model;

public record MovieInput(
        String name,
        Coordinates coordinates,
        long oscarsCount,
        Integer budget,
        MovieGenre genre,
        MpaaRating mpaaRating,
        Person screenwriter) {
}

package ru.itmo.movies.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieInput {

    private String name;
    private Coordinates coordinates;
    private Long oscarsCount;
    private Integer budget;
    private MovieGenre genre;
    private MpaaRating mpaaRating;
    private Person screenwriter;
}

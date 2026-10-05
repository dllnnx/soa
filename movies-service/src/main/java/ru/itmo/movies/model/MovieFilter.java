package ru.itmo.movies.model;

import java.time.OffsetDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovieFilter {

    private List<Integer> ids;
    private String name;
    private CoordinatesFilter coordinates;
    private OffsetDateTime creationDateBefore;
    private OffsetDateTime creationDateAfter;
    private Long oscarsCountFrom;
    private Long oscarsCountTo;
    private Integer budgetFrom;
    private Integer budgetTo;
    private List<MovieGenre> genres;
    private MpaaRating mpaaRating;
    private PersonFilter screenwriter;
}

package ru.itmo.movies.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LocationFilter {

    private Float xFrom;
    private Float xTo;
    private Integer yFrom;
    private Integer yTo;
    private Long zFrom;
    private Long zTo;
}

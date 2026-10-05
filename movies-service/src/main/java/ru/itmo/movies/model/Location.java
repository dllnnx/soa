package ru.itmo.movies.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Location {

    private Float x; // not null
    private int y;
    private Long z; // not null
}

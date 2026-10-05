package ru.itmo.movies.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Coordinates {

    private Double x; // not null, строго больше -130
    private Double y; // not null, максимум 388
}

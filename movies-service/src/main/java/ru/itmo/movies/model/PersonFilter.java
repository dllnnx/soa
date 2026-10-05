package ru.itmo.movies.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonFilter {

    private String name;
    private Double heightFrom;
    private Double heightTo;
    private EyeColor eyeColor;
    private HairColor hairColor;
    private Country nationality;
    private LocationFilter location;
}

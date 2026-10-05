package ru.itmo.movies.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Person {

    private String name; // not null, не пустая
    private double height; // строго больше 0
    private EyeColor eyeColor; // not null
    private HairColor hairColor; // может быть null
    private Country nationality; // not null
    private Location location; // not null
}

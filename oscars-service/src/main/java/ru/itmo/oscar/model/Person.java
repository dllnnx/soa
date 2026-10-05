package ru.itmo.oscar.model;

public record Person(
        String name,
        double height,
        EyeColor eyeColor,
        HairColor hairColor,
        Country nationality,
        Location location) {
}

package ru.itmo.movies.repository;

import lombok.Getter;

@Getter
public enum SortKey {
    ID("id"),
    NAME("name"),
    COORDINATES_X("coordinateX"),
    COORDINATES_Y("coordinateY"),
    CREATION_DATE("creationDate"),
    OSCARS_COUNT("oscarsCount"),
    BUDGET("budget"),
    GENRE("genre"),
    MPAA_RATING("mpaaRating"),
    SCREENWRITER_NAME("screenwriterName"),
    SCREENWRITER_HEIGHT("screenwriterHeight"),
    SCREENWRITER_EYE_COLOR("screenwriterEyeColor"),
    SCREENWRITER_HAIR_COLOR("screenwriterHairColor"),
    SCREENWRITER_NATIONALITY("screenwriterNationality"),
    SCREENWRITER_LOCATION_X("screenwriterLocationX"),
    SCREENWRITER_LOCATION_Y("screenwriterLocationY"),
    SCREENWRITER_LOCATION_Z("screenwriterLocationZ");

    private final String attribute;

    SortKey(String attribute) {
        this.attribute = attribute;
    }

}

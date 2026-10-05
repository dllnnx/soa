package ru.itmo.movies.repository;

public enum SortKey {
    ID("id"),
    NAME("name"),
    COORDINATES_X("coord_x"),
    COORDINATES_Y("coord_y"),
    CREATION_DATE("creation_date"),
    OSCARS_COUNT("oscars_count"),
    BUDGET("budget"),
    GENRE("genre"),
    MPAA_RATING("mpaa_rating"),
    SCREENWRITER_NAME("sw_name"),
    SCREENWRITER_HEIGHT("sw_height"),
    SCREENWRITER_EYE_COLOR("sw_eye_color"),
    SCREENWRITER_HAIR_COLOR("sw_hair_color"),
    SCREENWRITER_NATIONALITY("sw_nationality"),
    SCREENWRITER_LOCATION_X("sw_loc_x"),
    SCREENWRITER_LOCATION_Y("sw_loc_y"),
    SCREENWRITER_LOCATION_Z("sw_loc_z");

    private final String column;

    SortKey(String column) {
        this.column = column;
    }

    public String getColumn() {
        return column;
    }
}

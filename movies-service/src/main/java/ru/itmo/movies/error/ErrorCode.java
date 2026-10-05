package ru.itmo.movies.error;

import lombok.Getter;

@Getter
public enum ErrorCode {
    MALFORMED_JSON(400),
    INVALID_PARAMETER(400),
    UNKNOWN_PARAMETER(400),
    INVALID_SORT_EXPRESSION(400),
    UNKNOWN_PROPERTY(400),
    READ_ONLY_FIELD(400),
    MOVIE_NOT_FOUND(404),
    NO_MATCHING_MOVIE(404),
    VALIDATION_FAILED(422);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }
}

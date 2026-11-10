package ru.itmo.movies.error;

import lombok.Getter;

@Getter
public enum ErrorCode {
    MALFORMED_JSON(400),
    INVALID_PARAMETER(400),
    INTERNAL_UNAUTHORIZED(401),
    OPERATION_CONFLICT(409),
    OPERATION_UNAVAILABLE(503),
    INVALID_SORT_EXPRESSION(400),
    MOVIE_NOT_FOUND(404),
    NO_MATCHING_MOVIE(404),
    VALIDATION_FAILED(422);

    private final int status;

    ErrorCode(int status) {
        this.status = status;
    }
}

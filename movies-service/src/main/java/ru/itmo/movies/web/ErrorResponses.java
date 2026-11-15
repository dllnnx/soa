package ru.itmo.movies.web;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.error.Error;
import ru.itmo.movies.error.FieldError;

public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static ResponseEntity<Error> of(int status, ErrorCode code, String message) {
        return of(status, code, message, null);
    }

    public static ResponseEntity<Error> of(int status, ErrorCode code, String message, List<FieldError> details) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new Error(status, code, message, details));
    }
}

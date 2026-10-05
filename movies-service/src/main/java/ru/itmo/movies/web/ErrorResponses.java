package ru.itmo.movies.web;

import java.util.List;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import ru.itmo.movies.model.ErrorCode;
import ru.itmo.movies.model.Error;
import ru.itmo.movies.model.FieldError;

public final class ErrorResponses {

    private ErrorResponses() {
    }

    public static Response of(int status, ErrorCode code, String message) {
        return of(status, code, message, null);
    }

    public static Response of(int status, ErrorCode code, String message, List<FieldError> details) {
        String body = Json.write(new Error(status, code, message, details));
        return Response.status(status).type(MediaType.APPLICATION_JSON).entity(body).build();
    }
}

package ru.itmo.movies.web;

import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ru.itmo.movies.error.ErrorCode;

@Provider
public class NotAcceptableMapper implements ExceptionMapper<NotAcceptableException> {

    @Override
    public Response toResponse(NotAcceptableException exception) {
        return ErrorResponses.of(406, ErrorCode.INVALID_PARAMETER, "Заголовок Accept не допускает формат application/json");
    }
}

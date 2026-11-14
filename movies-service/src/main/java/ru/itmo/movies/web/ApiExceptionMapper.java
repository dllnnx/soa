package ru.itmo.movies.web;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ru.itmo.movies.error.ApiException;

@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException exception) {
        return ErrorResponses.of(exception.getStatus(), exception.getCode(),
                exception.getMessage(), exception.getDetails());
    }
}

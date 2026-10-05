package ru.itmo.movies.web;

import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ru.itmo.movies.model.ErrorCode;

@Provider
public class NotSupportedMapper implements ExceptionMapper<NotSupportedException> {

    @Override
    public Response toResponse(NotSupportedException exception) {
        return ErrorResponses.of(415, ErrorCode.INVALID_PARAMETER, "Content-Type должен быть application/json");
    }
}

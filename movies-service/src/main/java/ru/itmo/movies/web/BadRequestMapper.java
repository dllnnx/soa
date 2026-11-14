package ru.itmo.movies.web;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import ru.itmo.movies.error.ErrorCode;

@Provider
public class BadRequestMapper implements ExceptionMapper<BadRequestException> {
    @Override
    public Response toResponse(BadRequestException exception) {
        return ErrorResponses.of(400, ErrorCode.MALFORMED_JSON, "Некорректное тело или параметры запроса");
    }
}

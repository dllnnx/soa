package ru.itmo.movies.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.json.JsonException;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ru.itmo.movies.error.ErrorCode;

@Provider
public class MalformedJsonMapper implements ExceptionMapper<ProcessingException> {

    private static final Logger LOG = Logger.getLogger(MalformedJsonMapper.class.getName());

    @Override
    public Response toResponse(ProcessingException exception) {
        if (isClientPayloadError(exception)) {
            return ErrorResponses.of(400, ErrorCode.MALFORMED_JSON,
                    "Тело запроса содержит некорректный JSON или недопустимые значения полей");
        }
        LOG.log(Level.SEVERE, "Unhandled exception", exception);
        return ErrorResponses.of(500, null, "Внутренняя ошибка сервера");
    }

    private static boolean isClientPayloadError(Throwable exception) {
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof JsonbException || cause instanceof JsonException) {
                return true;
            }
        }
        return false;
    }
}

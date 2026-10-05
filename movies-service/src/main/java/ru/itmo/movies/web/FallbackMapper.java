package ru.itmo.movies.web;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FallbackMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(FallbackMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        LOG.log(Level.SEVERE, "Unhandled exception", exception);
        return ErrorResponses.of(500, null, "Внутренняя ошибка сервера");
    }
}

package ru.itmo.movies.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.error.FieldError;

@Provider
public class ConstraintViolationMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<FieldError> details = exception.getConstraintViolations().stream()
                .map(violation -> new FieldError(field(violation), violation.getMessage()))
                .toList();
        return ErrorResponses.of(422, ErrorCode.VALIDATION_FAILED, "Невалидные значения полей запроса", details);
    }

    private static String field(ConstraintViolation<?> violation) {
        List<String> nodes = new ArrayList<>();
        for (Path.Node node : violation.getPropertyPath()) {
            nodes.add(node.getName());
        }
        int from = Math.min(2, Math.max(0, nodes.size() - 1));
        return String.join(".", nodes.subList(from, nodes.size()));
    }
}

package ru.itmo.movies.web;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.error.Error;
import ru.itmo.movies.error.FieldError;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Error> apiException(ApiException exception) {
        return ErrorResponses.of(exception.getStatus(), exception.getCode(),
                exception.getMessage(), exception.getDetails());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Error> constraintViolation(ConstraintViolationException exception) {
        List<FieldError> details = exception.getConstraintViolations().stream()
                .map(violation -> new FieldError(field(violation), violation.getMessage()))
                .toList();
        return ErrorResponses.of(422, ErrorCode.VALIDATION_FAILED, "Невалидные значения полей запроса", details);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> methodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<FieldError> details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .toList();
        return ErrorResponses.of(422, ErrorCode.VALIDATION_FAILED, "Невалидные значения полей запроса", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Error> malformedJson(HttpMessageNotReadableException exception) {
        return ErrorResponses.of(400, ErrorCode.MALFORMED_JSON,
                "Тело запроса содержит некорректный JSON или недопустимые значения полей");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Error> notAcceptable(HttpMediaTypeNotAcceptableException exception) {
        return ErrorResponses.of(406, ErrorCode.INVALID_PARAMETER,
                "Заголовок Accept не допускает формат application/json");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Error> notSupported(HttpMediaTypeNotSupportedException exception) {
        return ErrorResponses.of(415, ErrorCode.INVALID_PARAMETER, "Content-Type должен быть application/json");
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<Error> badRequest(Exception exception) {
        return ErrorResponses.of(400, ErrorCode.INVALID_PARAMETER, "Некорректное тело или параметры запроса");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Error> fallback(Exception exception) {
        log.error("Unhandled exception", exception);
        return ErrorResponses.of(500, null, "Внутренняя ошибка сервера");
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

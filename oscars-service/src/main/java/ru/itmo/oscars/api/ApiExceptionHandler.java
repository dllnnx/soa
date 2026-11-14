package ru.itmo.oscars.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataAccessException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import ru.itmo.oscars.model.ApiError;
import ru.itmo.oscars.service.InvalidParameterException;
import ru.itmo.oscars.service.JobNotFoundException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ApiError> storageUnavailable(DataAccessException exception) {
        return response(HttpStatus.SERVICE_UNAVAILABLE, "JOB_STORAGE_UNAVAILABLE", "Хранилище джоб временно недоступно");
    }

    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ApiError> invalidParameter(InvalidParameterException exception) {
        return response(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", exception.getMessage());
    }

    @ExceptionHandler(JobNotFoundException.class)
    public ResponseEntity<ApiError> jobNotFound(JobNotFoundException exception) {
        return response(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", exception.getMessage());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(ApiError.of(status.value(), code, message));
    }
}

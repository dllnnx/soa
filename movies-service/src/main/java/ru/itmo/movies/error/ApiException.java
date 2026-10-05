package ru.itmo.movies.error;

import java.util.List;

import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final ErrorCode code;
    private final List<FieldError> details;

    public ApiException(ErrorCode code, String message) {
        this(code, message, null);
    }

    public ApiException(ErrorCode code, String message, List<FieldError> details) {
        super(message);
        this.code = code;
        this.details = details;
    }

    public int getStatus() {
        return code.getStatus();
    }
}

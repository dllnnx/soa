package ru.itmo.movies.error;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Error {

    private OffsetDateTime timestamp;
    private int status;
    private ErrorCode code;
    private String message;
    private List<FieldError> details;

    public Error(int status, ErrorCode code, String message) {
        this(status, code, message, null);
    }

    public Error(int status, ErrorCode code, String message, List<FieldError> details) {
        this.timestamp = OffsetDateTime.now(ZoneOffset.UTC);
        this.status = status;
        this.code = code;
        this.message = message;
        this.details = details;
    }
}

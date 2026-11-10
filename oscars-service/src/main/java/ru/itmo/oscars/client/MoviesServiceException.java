package ru.itmo.oscars.client;

public class MoviesServiceException extends RuntimeException {
    private final boolean retryable;

    public MoviesServiceException(String message, boolean retryable) {
        super(message);
        this.retryable = retryable;
    }

    public boolean retryable() {
        return retryable;
    }
}

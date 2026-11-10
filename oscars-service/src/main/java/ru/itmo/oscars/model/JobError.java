package ru.itmo.oscars.model;

public record JobError(String errorMessage) implements JobOutcome {
}

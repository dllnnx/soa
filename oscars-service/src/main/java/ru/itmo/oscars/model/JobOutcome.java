package ru.itmo.oscars.model;

public sealed interface JobOutcome permits UpdatedMoviesCount, JobError {
}

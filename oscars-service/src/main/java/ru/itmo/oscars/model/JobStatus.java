package ru.itmo.oscars.model;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record JobStatus(UUID jobId, JobState status, JobOutcome result) {
}

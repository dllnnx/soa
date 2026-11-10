package ru.itmo.oscars.model;

import java.util.Map;
import java.util.UUID;

public record OperationCommand(UUID jobId, JobType type, Map<String, String> arguments) {
}

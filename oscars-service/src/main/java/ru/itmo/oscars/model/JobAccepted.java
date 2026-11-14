package ru.itmo.oscars.model;

import java.util.UUID;

public record JobAccepted(UUID jobId, String statusUrl) {
}

package ru.itmo.oscar.model;

import java.util.UUID;

public record JobAccepted(UUID jobId, String statusUrl) {
}

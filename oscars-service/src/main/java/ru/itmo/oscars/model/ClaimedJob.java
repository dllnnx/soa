package ru.itmo.oscars.model;

import java.util.UUID;

public record ClaimedJob(OperationCommand command, UUID leaseToken, int attempts) {
}

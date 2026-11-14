package ru.itmo.oscars.repository;

import java.util.UUID;
import ru.itmo.oscars.model.OperationCommand;

public record ClaimedJob(OperationCommand command, UUID leaseToken, int attempts) {
}

package ru.itmo.oscar.repository;

import java.util.UUID;
import ru.itmo.oscar.model.OperationCommand;

public record ClaimedJob(OperationCommand command, UUID leaseToken, int attempts) {
}

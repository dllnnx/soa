package ru.itmo.oscar.service;

import java.util.UUID;

public class JobNotFoundException extends RuntimeException {

    public JobNotFoundException(UUID jobId) {
        super("Джоба с id=" + jobId + " не найдена");
    }
}

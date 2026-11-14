package ru.itmo.oscars.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ru.itmo.oscars.client.MoviesClient;
import ru.itmo.oscars.client.MoviesServiceException;
import ru.itmo.oscars.model.OperationCommand;
import ru.itmo.oscars.model.OperationResult;

@Slf4j
@Component
@AllArgsConstructor
public class MovieJobConsumer {
    private final JobService jobService;
    private final MoviesClient moviesClient;

    @KafkaListener(topics = "movie-jobs", groupId = "oscars-worker")
    public void onCommand(OperationCommand command) {
        if (!jobService.start(command.jobId())) {
            log.warn("Job {} has no row (submission was compensated); message skipped", command.jobId());
            return;
        }

        execute(command);
    }

    private void execute(OperationCommand command) {
        OperationResult result;
        try {
            result = moviesClient.execute(command);
        } catch (MoviesServiceException exception) {
            if (exception.retryable()) throw exception;
            jobService.fail(command.jobId(), exception.getMessage());
            return;
        }

        switch (result.status()) {
            case COMPLETED -> jobService.complete(command.jobId(), result.updatedCount());
            case FAILED -> jobService.fail(command.jobId(), result.errorMessage());
            case PENDING -> throw new MoviesServiceException(result.errorMessage(), true);
        }
    }
}

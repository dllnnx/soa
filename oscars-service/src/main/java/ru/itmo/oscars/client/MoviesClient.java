package ru.itmo.oscars.client;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import ru.itmo.oscars.model.OperationCommand;
import ru.itmo.oscars.model.OperationResult;

@Component
public class MoviesClient {
    private final RestClient client;

    public MoviesClient(RestClient moviesRestClient) {
        this.client = moviesRestClient;
    }

    public OperationResult execute(OperationCommand command) {
        try {
            var result = client.post().uri("/internal/operations")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(command)
                    .retrieve()
                    .body(OperationResult.class);

            if (result == null || !command.jobId().equals(result.jobId()) || result.status() == null) {
                throw new MoviesServiceException("Movies вернул неподтверждённый результат", true);
            }

            boolean valid = switch (result.status()) {
                case COMPLETED -> !result.retryable() && result.updatedCount() != null && result.updatedCount() >= 0;
                case FAILED -> !result.retryable() && result.errorMessage() != null;
                case PENDING -> result.retryable();
            };

            if (!valid) throw new MoviesServiceException("Movies вернул некорректный статус операции", true);
            return result;
        } catch (RestClientResponseException exception) {
            var status = exception.getStatusCode();
            boolean retryable = status.is5xxServerError()
                    || status.isSameCodeAs(HttpStatus.NOT_FOUND)
                    || status.isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)
                    || status.isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS);
            throw new MoviesServiceException("internal API movies вернуло HTTP " + status.value(), retryable);
        } catch (RestClientException exception) {
            throw new MoviesServiceException("Результат movies не получен: " + exception.getClass().getSimpleName(), true);
        }
    }
}

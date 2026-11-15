package ru.itmo.movies.operation;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

@Slf4j
@Service
public class OperationService {

    private static final String SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE = "22003";

    private final OperationRepository operations;
    private final Duration timeout;

    public OperationService(OperationRepository operations,
                            @Value("${movies.operation-timeout-ms:5000}") long timeoutMillis) {
        if (timeoutMillis <= 0) throw new IllegalArgumentException("Invalid operation limits");
        this.operations = operations;
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    public OperationResult execute(OperationCommand command) {
        try {
            operations.register(command);
        } catch (SQLException exception) {
            log.warn("Cannot register operation " + command.jobId(), exception);
            throw unavailable();
        }

        try (Connection connection = operations.open()) {
            try {
                long deadline = System.nanoTime() + timeout.toNanos();
                operations.configureTimeouts(connection, timeout.toMillis());

                var registered = operations.readRegisteredState(connection, command.jobId());
                if (!command.type().name().equals(registered.type())
                        || !Objects.equals(command.genre(), registered.genre())) {
                    throw new ApiException(ErrorCode.OPERATION_CONFLICT, "Этот jobId уже использован с другой командой");
                }
                if (registered.status() != OperationStatus.PENDING) {
                    connection.commit();
                    return registered.result();
                }

                operations.acquireOperationsLock(connection);
                operations.configureTimeouts(connection, remaining(deadline));
                long count = operations.apply(connection, command);
                operations.configureTimeouts(connection, 1000);
                operations.persistCompleted(connection, command.jobId(), count);
                connection.commit();
                return new OperationResult(command.jobId(), OperationStatus.COMPLETED, count, null, null, false);
            } catch (SQLException exception) {
                operations.rollback(connection, exception);
                return onAttemptFailure(connection, command, exception);
            } catch (RuntimeException exception) {
                operations.rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            log.warn("Operation outcome must be retried/reconciled: " + command.jobId(), exception);
            throw unavailable();
        }
    }

    private OperationResult onAttemptFailure(Connection connection, OperationCommand command, SQLException exception)
            throws SQLException {
        if (SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE.equals(exception.getSQLState())) {
            String message = "Начисление Оскаров привело бы к переполнению long; фильмы не изменены";
            operations.markFailed(connection, command.jobId(), "NUMERIC_OVERFLOW", message);
            return new OperationResult(command.jobId(), OperationStatus.FAILED, null, "NUMERIC_OVERFLOW", message, false);
        }
        log.warn("Operation attempt failed; the caller should retry with the same jobId: "
                + command.jobId(), exception);
        return new OperationResult(command.jobId(), OperationStatus.PENDING, null, "TEMPORARY_DATABASE_ERROR",
                "Временная ошибка базы данных; фильмы не изменены", true);
    }

    private long remaining(long deadline) {
        return Math.max(1, (deadline - System.nanoTime()) / 1_000_000);
    }

    private static ApiException unavailable() {
        return new ApiException(ErrorCode.OPERATION_UNAVAILABLE,
                "Результат операции пока не подтверждён; повторите запрос с тем же jobId");
    }
}

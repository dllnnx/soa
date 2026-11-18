package ru.itmo.movies.operation;

import java.sql.SQLException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionTimedOutException;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

@Slf4j
@Service
public class OperationService {

    private static final String SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE = "22003";

    private final OperationTransactions operations;

    public OperationService(OperationTransactions operations,
                            @Value("${movies.operation-timeout-ms:5000}") long timeoutMillis) {
        if (timeoutMillis <= 0) throw new IllegalArgumentException("Invalid operation limits");
        this.operations = operations;
    }

    public OperationResult execute(OperationCommand command) {
        try {
            operations.register(command);
        } catch (DataAccessException | TransactionException exception) {
            log.warn("Cannot register operation " + command.jobId(), exception);
            throw unavailable();
        }

        try {
            return operations.execute(command);
        } catch (TransactionTimedOutException exception) {
            return onAttemptFailure(command, exception);
        } catch (TransactionException | DataAccessResourceFailureException exception) {
            log.warn("Operation outcome must be retried/reconciled: " + command.jobId(), exception);
            throw unavailable();
        } catch (DataAccessException exception) {
            return onAttemptFailure(command, exception);
        }
    }

    private OperationResult onAttemptFailure(OperationCommand command, RuntimeException exception) {
        if (isNumericOverflow(exception)) {
            String message = "Начисление Оскаров привело бы к переполнению long; фильмы не изменены";
            try {
                return operations.markFailed(command.jobId(), "NUMERIC_OVERFLOW", message);
            } catch (DataAccessException | TransactionException failure) {
                log.warn("Cannot persist failed operation " + command.jobId(), failure);
                throw unavailable();
            }
        }
        log.warn("Operation attempt failed; the caller should retry with the same jobId: "
                + command.jobId(), exception);
        return new OperationResult(command.jobId(), OperationStatus.PENDING, null, "TEMPORARY_DATABASE_ERROR",
                "Временная ошибка базы данных; фильмы не изменены", true);
    }

    private static boolean isNumericOverflow(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }

    private static ApiException unavailable() {
        return new ApiException(ErrorCode.OPERATION_UNAVAILABLE,
                "Результат операции пока не подтверждён; повторите запрос с тем же jobId");
    }
}

package ru.itmo.movies.operation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
public class OperationRepository {
    private static final long OPERATIONS_LOCK = 1936681575L;

    private static final String SQLSTATE_QUERY_CANCELED = "57014";
    private static final String SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE = "22003";

    private static final String REWARD_QUERY = """
            UPDATE movie
            SET oscars_count = oscars_count + 1
            WHERE mpaa_rating = 'R'""";

    private static final String RESET_QUERY = """
            UPDATE movie AS target
            SET oscars_count = 0
            WHERE target.oscars_count > 0 AND target.sw_name IS NOT NULL
              AND EXISTS (SELECT 1 FROM movie AS source WHERE source.genre = ? AND source.sw_name = target.sw_name)
            """;

    private final DataSource source;
    private final Duration timeout;

    public OperationRepository(DataSource source,
                               @Value("${movies.operation-timeout-ms:5000}") long timeoutMillis) {
        if (timeoutMillis <= 0)
            throw new IllegalArgumentException("Invalid operation limits");
        this.source = source;
        this.timeout = Duration.ofMillis(timeoutMillis);
    }

    public OperationResult execute(OperationCommand command) {
        register(command);
        try (var connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long deadline = System.nanoTime() + timeout.toNanos();
                configureTimeout(connection, timeout.toMillis());

                var registered = readRegisteredState(connection, command);
                if (registered.status != OperationStatus.PENDING) {
                    connection.commit();
                    return registered;
                }

                acquireOperationsLock(connection, deadline);
                var outcome = persistOutcome(connection, command.jobId(), apply(connection, command, deadline));
                connection.commit();
                return outcome;
            } catch (SQLException exception) {
                rollback(connection, exception);
                return onAttemptFailure(connection, command, exception);
            } catch (RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            log.warn("Operation outcome must be retried/reconciled: " + command.jobId(), exception);
            throw unavailable();
        }
    }

    private void register(OperationCommand command) {
        try (var connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                configureTimeout(connection, 1000);
                try (var statement = connection.prepareStatement("""
                        INSERT INTO movie_operation(job_id, type, genre) VALUES (?, ?, ?) ON CONFLICT (job_id) DO NOTHING
                        """)) {
                    statement.setObject(1, command.jobId());
                    statement.setString(2, command.type().name());
                    statement.setString(3, command.genre());
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            log.warn("Cannot register operation " + command.jobId(), exception);
            throw unavailable();
        }
    }

    private OperationResult readRegisteredState(Connection connection, OperationCommand command) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT job_id, type, genre, status, updated_count, error_code, error_message
                FROM movie_operation WHERE job_id = ? FOR UPDATE
                """)) {
            statement.setObject(1, command.jobId());
            try (var row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Registered operation is missing");
                if (!command.type().name().equals(row.getString("type")) || !Objects.equals(command.genre(), row.getString("genre"))) {
                    throw new ApiException(ErrorCode.OPERATION_CONFLICT, "Этот jobId уже использован с другой командой");
                }
                return result(row);
            }
        }
    }

    private void acquireOperationsLock(Connection connection, long deadline) throws SQLException {
        remainingTimeout(connection, deadline);
        try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, OPERATIONS_LOCK);
            statement.execute();
        }
    }

    private long apply(Connection connection, OperationCommand command, long deadline) throws SQLException {
        remainingTimeout(connection, deadline);
        try (var statement = connection.prepareStatement(command.type() == OperationCommand.Type.REWARD_R ? REWARD_QUERY : RESET_QUERY)) {
            if (command.type() == OperationCommand.Type.RESET_BY_GENRE)
                statement.setString(1, command.genre());
            return statement.executeLargeUpdate();
        }
    }

    private OperationResult persistOutcome(Connection connection, UUID jobId, long count) throws SQLException {
        configureTimeout(connection, 1000);
        try (var statement = connection.prepareStatement("""
                UPDATE movie_operation SET status = 'COMPLETED', updated_count = ?, updated_at = now()
                WHERE job_id = ?
                """)) {
            statement.setLong(1, count);
            statement.setObject(2, jobId);
            statement.executeUpdate();
        }
        return new OperationResult(jobId, OperationStatus.COMPLETED, count, null, null, false);
    }

    private OperationResult onAttemptFailure(Connection connection, OperationCommand command, SQLException exception)
            throws SQLException {
        if (SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE.equals(exception.getSQLState())) {
            String message = "Начисление Оскаров привело бы к переполнению long; фильмы не изменены";
            markFailed(connection, command.jobId(), "NUMERIC_OVERFLOW", message);
            return new OperationResult(command.jobId(), OperationStatus.FAILED, null, "NUMERIC_OVERFLOW", message, false);
        }
        log.warn("Operation attempt failed; the caller should retry with the same jobId: "
                + command.jobId(), exception);
        return new OperationResult(command.jobId(), OperationStatus.PENDING, null, "TEMPORARY_DATABASE_ERROR",
                "Временная ошибка базы данных; фильмы не изменены", true);
    }

    private void markFailed(Connection connection, UUID jobId, String errorCode, String message) throws SQLException {
        configureTimeout(connection, 1000);
        try (var statement = connection.prepareStatement("""
                UPDATE movie_operation SET status = 'FAILED', error_code = ?, error_message = ?, updated_at = now()
                WHERE job_id = ? AND status = 'PENDING'
                """)) {
            statement.setString(1, errorCode);
            statement.setString(2, message);
            statement.setObject(3, jobId);
            statement.executeUpdate();
        }
        connection.commit();
    }

    private static OperationResult result(ResultSet row) throws SQLException {
        var status = OperationStatus.valueOf(row.getString("status"));
        return new OperationResult(row.getObject("job_id", UUID.class), status,
                row.getObject("updated_count", Long.class), row.getString("error_code"), row.getString("error_message"),
                status == OperationStatus.PENDING);
    }

    private static void rollback(Connection connection, Exception exception) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            exception.addSuppressed(rollbackFailure);
        }
    }

    private static void remainingTimeout(Connection connection, long deadline) throws SQLException {
        long remainingMillis = (deadline - System.nanoTime()) / 1_000_000;
        if (remainingMillis <= 0) throw new SQLException("Operation deadline exceeded", SQLSTATE_QUERY_CANCELED);
    }

    private static void configureTimeout(Connection connection, long millis) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT set_config('statement_timeout', ?, true), set_config('lock_timeout', ?, true)")) {
            statement.setString(1, Math.max(1, millis) + "ms");
            statement.setString(2, Math.max(1, Math.min(1000, millis)) + "ms");
            statement.execute();
        }
    }

    private static ApiException unavailable() {
        return new ApiException(ErrorCode.OPERATION_UNAVAILABLE, "Результат операции пока не подтверждён; повторите запрос с тем же jobId");
    }
}

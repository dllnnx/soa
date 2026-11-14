package ru.itmo.movies.operation;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;
import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class OperationRepository {
    private static final Logger LOG = Logger.getLogger(OperationRepository.class.getName());
    private static final long OPERATIONS_LOCK = 1936681575L;

    private static final String SQLSTATE_QUERY_CANCELED = "57014";
    private static final String SQLSTATE_LOCK_NOT_AVAILABLE = "55P03";
    private static final String SQLSTATE_DEADLOCK_DETECTED = "40P01";
    private static final String SQLSTATE_SERIALIZATION_FAILURE = "40001";
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

    @Resource(lookup = "jdbc/MoviesOperationsDS")
    private DataSource source;
    private final Duration timeout;
    private final int maxAttempts;

    public OperationRepository() {
        this(null, Duration.ofMillis(Long.parseLong(System.getenv().getOrDefault("MOVIES_OPERATION_TIMEOUT_MS", "5000"))),
                Integer.parseInt(System.getenv().getOrDefault("MOVIES_OPERATION_MAX_ATTEMPTS", "8")));
    }

    public OperationRepository(DataSource source, Duration timeout, int maxAttempts) {
        if (timeout.isNegative() || timeout.isZero() || maxAttempts < 1)
            throw new IllegalArgumentException("Invalid operation limits");
        this.source = source;
        this.timeout = timeout;
        this.maxAttempts = maxAttempts;
    }

    private record RegisteredState(OperationResult outcome, int attempts) {
    }

    public OperationResult execute(OperationCommand command) {
        register(command);
        try (var connection = source.getConnection()) {
            connection.setAutoCommit(false);
            try {
                long deadline = System.nanoTime() + timeout.toNanos();
                configureTimeout(connection, timeout.toMillis());

                var state = readRegisteredState(connection, command);
                if (state.outcome().status != OperationStatus.PENDING) {
                    connection.commit();
                    return state.outcome();
                }

                acquireOperationsLock(connection, deadline);
                var outcome = applyWithSavepoint(connection, command, state.attempts(), deadline);
                outcome = persistOutcome(connection, command.jobId(), outcome);
                connection.commit();
                return outcome;
            } catch (SQLException | RuntimeException exception) {
                rollback(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            LOG.log(Level.WARNING, "Operation outcome must be retried/reconciled: " + command.jobId(), exception);
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
            LOG.log(Level.WARNING, "Cannot register operation " + command.jobId(), exception);
            throw unavailable();
        }
    }

    private RegisteredState readRegisteredState(Connection connection, OperationCommand command) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT * FROM movie_operation WHERE job_id = ? FOR UPDATE")) {
            statement.setObject(1, command.jobId());
            try (var row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Registered operation is missing");
                if (!command.type().name().equals(row.getString("type")) || !Objects.equals(command.genre(), row.getString("genre"))) {
                    throw new ApiException(ErrorCode.OPERATION_CONFLICT, "Этот jobId уже использован с другой командой");
                }
                return new RegisteredState(result(row), row.getInt("attempts"));
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

    private OperationResult applyWithSavepoint(Connection connection, OperationCommand command, int attempts, long deadline)
            throws SQLException {
        var savepoint = connection.setSavepoint();
        try {
            remainingTimeout(connection, deadline);
            long count;
            try (var statement = connection.prepareStatement(command.type() == OperationCommand.Type.REWARD_R ? REWARD_QUERY : RESET_QUERY)) {
                if (command.type() == OperationCommand.Type.RESET_BY_GENRE)
                    statement.setString(1, command.genre());
                count = statement.executeLargeUpdate();
            }
            return new OperationResult(command.jobId(), OperationStatus.COMPLETED, count, null, null, false);
        } catch (SQLException exception) {
            connection.rollback(savepoint);
            return onUpdateFailure(command, exception, attempts);
        }
    }

    private OperationResult onUpdateFailure(OperationCommand command, SQLException exception, int attempts)
            throws SQLException {
        if (transientFailure(exception)) {
            boolean exhausted = attempts + 1 >= maxAttempts;
            return new OperationResult(command.jobId(), exhausted ? OperationStatus.FAILED : OperationStatus.PENDING, null,
                    exhausted ? "RETRIES_EXHAUSTED" : "TEMPORARY_DATABASE_ERROR",
                    exhausted ? "Исчерпаны " + maxAttempts + " попыток выполнения; фильмы не изменены"
                            : "Временная ошибка или превышен лимит выполнения попытки", !exhausted);
        }
        if (SQLSTATE_NUMERIC_VALUE_OUT_OF_RANGE.equals(exception.getSQLState())) {
            return new OperationResult(command.jobId(), OperationStatus.FAILED, null, "NUMERIC_OVERFLOW",
                    "Начисление Оскаров привело бы к переполнению long; фильмы не изменены", false);
        }
        throw exception;
    }

    private OperationResult persistOutcome(Connection connection, java.util.UUID jobId, OperationResult outcome)
            throws SQLException {
        configureTimeout(connection, 1000);
        try (var statement = connection.prepareStatement("""
                UPDATE movie_operation SET attempts = attempts + 1, status = ?,
                  updated_count = ?, error_code = ?, error_message = ?, updated_at = now()
                WHERE job_id = ? RETURNING *
                """)) {
            statement.setString(1, outcome.status.name());
            statement.setObject(2, outcome.updatedCount);
            statement.setString(3, outcome.errorCode);
            statement.setString(4, outcome.errorMessage);
            statement.setObject(5, jobId);
            try (var row = statement.executeQuery()) {
                row.next();
                return result(row);
            }
        }
    }

    private static OperationResult result(ResultSet row) throws SQLException {
        var status = OperationStatus.valueOf(row.getString("status"));
        return new OperationResult(row.getObject("job_id", java.util.UUID.class), status,
                row.getObject("updated_count", Long.class), row.getString("error_code"), row.getString("error_message"), status == OperationStatus.PENDING);
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
        configureTimeout(connection, remainingMillis);
    }

    private static void configureTimeout(Connection connection, long millis) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT set_config('statement_timeout', ?, true), set_config('lock_timeout', ?, true)")) {
            statement.setString(1, Math.max(1, millis) + "ms");
            statement.setString(2, Math.max(1, Math.min(1000, millis)) + "ms");
            statement.execute();
        }
    }

    private static boolean transientFailure(SQLException exception) {
        return switch (exception.getSQLState() == null ? "" : exception.getSQLState()) {
            case SQLSTATE_QUERY_CANCELED, SQLSTATE_LOCK_NOT_AVAILABLE, SQLSTATE_DEADLOCK_DETECTED, SQLSTATE_SERIALIZATION_FAILURE -> true;
            default -> false;
        };
    }

    private static ApiException unavailable() {
        return new ApiException(ErrorCode.OPERATION_UNAVAILABLE, "Результат операции пока не подтверждён; повторите запрос с тем же jobId");
    }
}

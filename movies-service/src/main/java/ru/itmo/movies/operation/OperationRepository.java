package ru.itmo.movies.operation;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class OperationRepository {

    private static final long OPERATIONS_LOCK = 1936681575L;

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

    public record RegisteredOperation(UUID jobId, String type, String genre, OperationStatus status,
                                      Long updatedCount, String errorCode, String errorMessage) {

        public OperationResult result() {
            return new OperationResult(jobId, status, updatedCount, errorCode, errorMessage,
                    status == OperationStatus.PENDING);
        }
    }

    public Connection open() throws SQLException {
        Connection connection = source.getConnection();
        connection.setAutoCommit(false);
        return connection;
    }

    public void register(OperationCommand command) throws SQLException {
        try (var connection = open()) {
            try {
                configureTimeouts(connection, 1000);
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
        }
    }

    public RegisteredOperation readRegisteredState(Connection connection, UUID jobId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT job_id, type, genre, status, updated_count, error_code, error_message
                FROM movie_operation WHERE job_id = ? FOR UPDATE
                """)) {
            statement.setObject(1, jobId);
            try (var row = statement.executeQuery()) {
                if (!row.next()) throw new SQLException("Registered operation is missing");
                return read(row);
            }
        }
    }

    public void acquireOperationsLock(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, OPERATIONS_LOCK);
            statement.execute();
        }
    }

    public long apply(Connection connection, OperationCommand command) throws SQLException {
        try (var statement = connection.prepareStatement(
                command.type() == OperationCommand.Type.REWARD_R ? REWARD_QUERY : RESET_QUERY)) {
            if (command.type() == OperationCommand.Type.RESET_BY_GENRE)
                statement.setString(1, command.genre());
            return statement.executeLargeUpdate();
        }
    }

    public void persistCompleted(Connection connection, UUID jobId, long count) throws SQLException {
        try (var statement = connection.prepareStatement("""
                UPDATE movie_operation SET status = 'COMPLETED', updated_count = ?, updated_at = now()
                WHERE job_id = ?
                """)) {
            statement.setLong(1, count);
            statement.setObject(2, jobId);
            statement.executeUpdate();
        }
    }

    public void markFailed(Connection connection, UUID jobId, String errorCode, String message) throws SQLException {
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

    public void configureTimeouts(Connection connection, long millis) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT set_config('statement_timeout', ?, true), set_config('lock_timeout', ?, true)")) {
            statement.setString(1, Math.max(1, millis) + "ms");
            statement.setString(2, Math.max(1, Math.min(1000, millis)) + "ms");
            statement.execute();
        }
    }

    public void rollback(Connection connection, Exception exception) {
        try {
            connection.rollback();
        } catch (SQLException rollbackFailure) {
            exception.addSuppressed(rollbackFailure);
        }
    }

    private static RegisteredOperation read(ResultSet row) throws SQLException {
        return new RegisteredOperation(
                row.getObject("job_id", UUID.class),
                row.getString("type"),
                row.getString("genre"),
                OperationStatus.valueOf(row.getString("status")),
                row.getObject("updated_count", Long.class),
                row.getString("error_code"),
                row.getString("error_message"));
    }
}

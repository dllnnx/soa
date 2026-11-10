package ru.itmo.movies.operation;

import java.util.Map;
import java.util.UUID;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.model.MovieGenre;

public record OperationCommand(UUID jobId, Type type, String genre) {
    public enum Type { REWARD_R, RESET_BY_GENRE }

    public static class Request {
        public String jobId;
        public String type;
        public Map<String, String> arguments;
    }

    public static OperationCommand from(Request request) {
        try {
            if (request == null) throw new IllegalArgumentException();
            var id = UUID.fromString(request.jobId);
            if (!id.toString().equalsIgnoreCase(request.jobId)) throw new IllegalArgumentException();
            var type = Type.valueOf(request.type);
            var args = request.arguments == null ? Map.<String, String>of() : request.arguments;
            if (type == Type.REWARD_R && args.isEmpty()) return new OperationCommand(id, type, null);
            if (type == Type.RESET_BY_GENRE && args.size() == 1 && args.containsKey("genre")) {
                return new OperationCommand(id, type, MovieGenre.fromValue(args.get("genre")).name());
            }
        } catch (IllegalArgumentException | NullPointerException ignored) {
        }
        throw new ApiException(ErrorCode.INVALID_PARAMETER,
                "Нужны UUID jobId, тип REWARD_R без аргументов или RESET_BY_GENRE с аргументом genre");
    }
}

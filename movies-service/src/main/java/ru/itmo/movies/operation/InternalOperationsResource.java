package ru.itmo.movies.operation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

@Path("internal/operations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Transactional(Transactional.TxType.NOT_SUPPORTED)
public class InternalOperationsResource {
    @Inject
    private OperationRepository operations;

    @POST
    public OperationResult execute(@HeaderParam("X-Internal-Token") String token, OperationCommand.Request request) {
        var expected = System.getenv("MOVIES_INTERNAL_TOKEN");
        if (expected == null || expected.isBlank()) {
            throw new ApiException(ErrorCode.OPERATION_UNAVAILABLE, "Нет MOVIES_INTERNAL_TOKEN в env");
        }
        if (token == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(ErrorCode.INTERNAL_UNAUTHORIZED, "Неверный токен внутреннего API");
        }
        return operations.execute(OperationCommand.from(request));
    }
}

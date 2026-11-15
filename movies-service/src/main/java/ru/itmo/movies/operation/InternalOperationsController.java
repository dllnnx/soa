package ru.itmo.movies.operation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

@RestController
@RequestMapping("internal/operations")
public class InternalOperationsController {

    private final OperationService operationService;
    private final String expectedToken;

    public InternalOperationsController(OperationService operationService,
                                        @Value("${movies.internal-token:}") String expectedToken) {
        this.operationService = operationService;
        this.expectedToken = expectedToken;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public OperationResult execute(@RequestHeader(value = "X-Internal-Token", required = false) String token,
                                   @RequestBody OperationCommand.Request request) {
        if (expectedToken == null || expectedToken.isBlank()) {
            throw new ApiException(ErrorCode.OPERATION_UNAVAILABLE, "Нет MOVIES_INTERNAL_TOKEN в env");
        }

        if (token == null || !MessageDigest.isEqual(expectedToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(ErrorCode.INTERNAL_UNAUTHORIZED, "Неверный токен внутреннего API");
        }

        return operationService.execute(OperationCommand.from(request));
    }
}

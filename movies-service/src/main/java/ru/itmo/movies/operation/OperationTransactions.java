package ru.itmo.movies.operation;

import java.util.Objects;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.model.MovieGenre;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.repository.MovieRepository;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 1)
public class OperationTransactions {

    private final OperationRepository operations;
    private final MovieRepository movies;

    public void register(OperationCommand command) {
        operations.register(command.jobId(), command.type().name(), command.genre());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.SERIALIZABLE,
            timeoutString = "#{(${movies.operation-timeout-ms:5000} + 999L) / 1000L}")
    public OperationResult execute(OperationCommand command) {
        var registered = registeredState(command.jobId());
        if (command.type() != registered.getType() || !Objects.equals(command.genre(), registered.getGenre())) {
            throw new ApiException(ErrorCode.OPERATION_CONFLICT, "Этот jobId уже использован с другой командой");
        }
        if (registered.getStatus() != OperationStatus.PENDING) return registered.result();

        long count = switch (command.type()) {
            case REWARD_R -> movies.rewardByMpaaRating(MpaaRating.R);
            case RESET_BY_GENRE -> movies.resetOscarsByGenre(MovieGenre.valueOf(command.genre()));
        };
        registered.complete(count);
        return registered.result();
    }

    public OperationResult markFailed(UUID jobId, String errorCode, String message) {
        var registered = registeredState(jobId);
        if (registered.getStatus() != OperationStatus.PENDING) return registered.result();
        registered.fail(errorCode, message);
        return registered.result();
    }

    private OperationEntity registeredState(UUID jobId) {
        return operations.findByJobIdForUpdate(jobId)
                .orElseThrow(() -> new DataRetrievalFailureException("Registered operation is missing"));
    }
}

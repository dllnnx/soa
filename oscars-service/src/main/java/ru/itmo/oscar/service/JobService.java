package ru.itmo.oscar.service;

import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import ru.itmo.oscar.model.JobAccepted;
import ru.itmo.oscar.model.JobStatus;
import ru.itmo.oscar.model.JobType;
import ru.itmo.oscar.model.MovieGenre;
import ru.itmo.oscar.repository.JobRepository;

@Service
public class JobService {
    private final JobRepository jobs;

    public JobService(JobRepository jobs) {
        this.jobs = jobs;
    }

    public JobAccepted submit(JobType type, MovieGenre genre) {
        if (type == null || (type == JobType.REWARD_R && genre != null) || (type == JobType.RESET_BY_GENRE && genre == null)) {
            throw new InvalidParameterException("Неверные аргументы операции");
        }
        var id = jobs.create(type, genre == null ? Map.of() : Map.of("genre", genre.name()));
        return new JobAccepted(id, "/oscar/jobs/" + id + "/status");
    }

    public JobStatus get(UUID id) {
        return jobs.find(id).orElseThrow(() -> new JobNotFoundException(id));
    }
}

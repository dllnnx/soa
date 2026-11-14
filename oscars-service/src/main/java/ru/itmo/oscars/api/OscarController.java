package ru.itmo.oscars.api;

import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.oscars.model.JobAccepted;
import ru.itmo.oscars.model.MovieGenre;
import ru.itmo.oscars.service.InvalidParameterException;
import ru.itmo.oscars.service.JobService;
import ru.itmo.oscars.model.JobType;

@RestController
@RequestMapping("/movies")
public class OscarController {

    private final JobService jobService;

    public OscarController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping("/reward-r")
    public ResponseEntity<JobAccepted> rewardRMovies() {
        return ResponseEntity.accepted().body(jobService.submit(JobType.REWARD_R, null));
    }

    @PostMapping("/reset-oscars-by-genre/{genre}")
    public ResponseEntity<JobAccepted> resetOscarsByGenre(@PathVariable("genre") String genre) {
        var parsedGenre = parseGenre(genre);
        return ResponseEntity.accepted()
                .body(jobService.submit(JobType.RESET_BY_GENRE, parsedGenre));
    }

    private MovieGenre parseGenre(String value) {
        try {
            return MovieGenre.valueOf(value);
        } catch (IllegalArgumentException exception) {
            throw new InvalidParameterException("Жанр должен быть одним из "
                    + String.join(", ", Arrays.stream(MovieGenre.values()).map(Enum::name).toList()));
        }
    }
}

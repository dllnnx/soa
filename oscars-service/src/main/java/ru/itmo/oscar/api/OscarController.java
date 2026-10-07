package ru.itmo.oscar.api;

import java.util.Arrays;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.oscar.model.JobAccepted;
import ru.itmo.oscar.model.MovieGenre;
import ru.itmo.oscar.service.InvalidParameterException;
import ru.itmo.oscar.service.JobService;
import ru.itmo.oscar.service.OscarService;

@RestController
@RequestMapping("/movies")
public class OscarController {

    private final JobService jobService;
    private final OscarService oscarService;

    public OscarController(JobService jobService, OscarService oscarService) {
        this.jobService = jobService;
        this.oscarService = oscarService;
    }

    @PostMapping("/reward-r")
    public ResponseEntity<JobAccepted> rewardRMovies() {
        return ResponseEntity.accepted().body(jobService.submit(oscarService::rewardRMovies));
    }

    @PostMapping("/reset-oscars-by-genre/{genre}")
    public ResponseEntity<JobAccepted> resetOscarsByGenre(@PathVariable("genre") String genre) {
        var parsedGenre = parseGenre(genre);
        return ResponseEntity.accepted()
                .body(jobService.submit(() -> oscarService.resetOscarsByGenre(parsedGenre)));
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

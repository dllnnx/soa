package ru.itmo.movies.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ru.itmo.movies.model.AverageBudget;
import ru.itmo.movies.model.DeleteResult;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.model.TotalOscars;
import ru.itmo.movies.service.MovieService;

@Validated
@RestController
@RequestMapping("movies")
@AllArgsConstructor
public class MovieSpecialController {

    private final MovieService service;

    @GetMapping("average-budget")
    public AverageBudget getAverageBudget() {
        return service.getAverageBudget();
    }

    @GetMapping("total-oscars")
    public TotalOscars getTotalOscars() {
        return service.getTotalOscars();
    }

    @DeleteMapping("by-mpaa-rating")
    public DeleteResult deleteByMpaaRating(@RequestParam("mpaa-rating") @NotNull MpaaRating mpaaRating) {
        return service.deleteByMpaaRating(mpaaRating);
    }

    @DeleteMapping("by-screenwriter")
    public ResponseEntity<Void> deleteByScreenwriter(
            @RequestParam("name") @NotNull
            @Pattern(regexp = "^[a-zA-Zа-яА-ЯЁё0-9 .,:!?()&-]{1,255}$") String name) {
        service.deleteByScreenwriter(name);
        return ResponseEntity.noContent().build();
    }
}

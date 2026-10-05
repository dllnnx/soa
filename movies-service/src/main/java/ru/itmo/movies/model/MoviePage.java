package ru.itmo.movies.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoviePage {

    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<Movie> content;
}

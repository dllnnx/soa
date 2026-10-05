package ru.itmo.oscar.model;

import java.util.List;

public record MoviePage(
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<Movie> content) {
}

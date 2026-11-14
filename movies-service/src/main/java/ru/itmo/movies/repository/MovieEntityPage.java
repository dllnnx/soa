package ru.itmo.movies.repository;

import java.util.List;

public record MovieEntityPage(
        List<MovieEntity> content,
        int page,
        int size,
        long totalElements
) {
}

package ru.itmo.movies.service;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MovieService {

    public Double getAverageBudget() {
        // TODO: SELECT AVG(budget) FROM movie WHERE budget IS NOT NULL
        return 0.0;
    }
}

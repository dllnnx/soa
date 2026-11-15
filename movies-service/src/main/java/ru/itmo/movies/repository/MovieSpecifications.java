package ru.itmo.movies.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import ru.itmo.movies.model.CoordinatesFilter;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.PersonFilter;

public final class MovieSpecifications {

    private MovieSpecifications() {
    }

    public static Specification<MovieEntity> byFilter(MovieFilter filter) {
        return (root, query, builder) -> builder.and(
                predicates(filter, root, builder).toArray(Predicate[]::new));
    }

    private static List<Predicate> predicates(MovieFilter filter, Root<MovieEntity> root, CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getIds() != null && !filter.getIds().isEmpty()) {
            predicates.add(root.get("id").in(filter.getIds().stream().map(Integer::longValue).toList()));
        }
        if (notBlank(filter.getName())) predicates.add(like(builder, root.get("name"), filter.getName()));

        CoordinatesFilter coordinates = filter.getCoordinates();
        if (coordinates != null) {
            range(predicates, builder, root.get("coordinateX"), coordinates.getxFrom(), coordinates.getxTo());
            range(predicates, builder, root.get("coordinateY"), coordinates.getyFrom(), coordinates.getyTo());
        }
        lt(predicates, builder, root.get("creationDate"), filter.getCreationDateBefore());
        gt(predicates, builder, root.get("creationDate"), filter.getCreationDateAfter());
        range(predicates, builder, root.get("oscarsCount"), filter.getOscarsCountFrom(), filter.getOscarsCountTo());
        range(predicates, builder, root.get("budget"), filter.getBudgetFrom(), filter.getBudgetTo());
        if (filter.getGenres() != null && !filter.getGenres().isEmpty()) {
            predicates.add(root.get("genre").in(filter.getGenres()));
        }
        eq(predicates, builder, root, "mpaaRating", filter.getMpaaRating());

        PersonFilter screenwriter = filter.getScreenwriter();
        if (screenwriter != null) {
            if (notBlank(screenwriter.getName())) {
                predicates.add(like(builder, root.get("screenwriterName"), screenwriter.getName()));
            }
            range(predicates, builder, root.get("screenwriterHeight"),
                    screenwriter.getHeightFrom(), screenwriter.getHeightTo());
            eq(predicates, builder, root, "screenwriterEyeColor", screenwriter.getEyeColor());
            eq(predicates, builder, root, "screenwriterHairColor", screenwriter.getHairColor());
            eq(predicates, builder, root, "screenwriterNationality", screenwriter.getNationality());
            var location = screenwriter.getLocation();
            if (location != null) {
                range(predicates, builder, root.get("screenwriterLocationX"), location.getxFrom(), location.getxTo());
                range(predicates, builder, root.get("screenwriterLocationY"), location.getyFrom(), location.getyTo());
                range(predicates, builder, root.get("screenwriterLocationZ"), location.getzFrom(), location.getzTo());
            }
        }

        return predicates;
    }

    private static Predicate like(CriteriaBuilder builder, Path<String> path, String value) {
        return builder.like(builder.lower(path), "%" + value.toLowerCase(Locale.ROOT) + "%");
    }

    private static void eq(List<Predicate> predicates, CriteriaBuilder builder, Root<MovieEntity> root,
                           String attribute, Object value) {
        if (value != null) predicates.add(builder.equal(root.get(attribute), value));
    }

    private static void range(List<Predicate> predicates, CriteriaBuilder builder,
                              Expression<? extends Number> path, Number from, Number to) {
        if (from != null) predicates.add(builder.gt(path, from));
        if (to != null) predicates.add(builder.lt(path, to));
    }

    private static <T extends Comparable<? super T>> void gt(List<Predicate> predicates, CriteriaBuilder builder,
                                                             Expression<T> path, T value) {
        if (value != null) predicates.add(builder.greaterThan(path, value));
    }

    private static <T extends Comparable<? super T>> void lt(List<Predicate> predicates, CriteriaBuilder builder,
                                                             Expression<T> path, T value) {
        if (value != null) predicates.add(builder.lessThan(path, value));
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

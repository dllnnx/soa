package ru.itmo.movies.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

import ru.itmo.movies.model.CoordinatesFilter;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.PersonFilter;

public final class MovieSpecifications {

    private MovieSpecifications() {
    }

    public static Specification<MovieEntity> byFilter(MovieFilter filter) {
        return (root, query, builder) -> builder.and(predicates(filter, root, builder).toArray(Predicate[]::new));
    }

    private static List<Predicate> predicates(MovieFilter filter,
                                              jakarta.persistence.criteria.Root<MovieEntity> root,
                                              jakarta.persistence.criteria.CriteriaBuilder builder) {
        List<Predicate> predicates = new ArrayList<>();

        if (filter.getIds() != null && !filter.getIds().isEmpty()) {
            List<Long> ids = filter.getIds().stream().map(Integer::longValue).toList();
            predicates.add(root.<Long>get("id").in(ids));
        }
        if (notBlank(filter.getName())) {
            predicates.add(containsIgnoreCase(builder, root.get("name"), filter.getName()));
        }
        CoordinatesFilter coordinates = filter.getCoordinates();
        if (coordinates != null) {
            range(builder, root.get("coordinateX"), coordinates.getxFrom(), coordinates.getxTo(), predicates);
            range(builder, root.get("coordinateY"), coordinates.getyFrom(), coordinates.getyTo(), predicates);
        }
        if (filter.getCreationDateBefore() != null) {
            predicates.add(builder.lessThan(root.get("creationDate"), filter.getCreationDateBefore()));
        }
        if (filter.getCreationDateAfter() != null) {
            predicates.add(builder.greaterThan(root.get("creationDate"), filter.getCreationDateAfter()));
        }
        range(builder, root.get("oscarsCount"),
                filter.getOscarsCountFrom(), filter.getOscarsCountTo(), predicates);
        range(builder, root.get("budget"), filter.getBudgetFrom(), filter.getBudgetTo(), predicates);
        if (filter.getGenres() != null && !filter.getGenres().isEmpty()) {
            predicates.add(root.get("genre").in(filter.getGenres()));
        }
        if (filter.getMpaaRating() != null) {
            predicates.add(builder.equal(root.get("mpaaRating"), filter.getMpaaRating()));
        }

        PersonFilter screenwriter = filter.getScreenwriter();
        if (screenwriter != null) {
            if (notBlank(screenwriter.getName())) {
                predicates.add(containsIgnoreCase(
                        builder, root.get("screenwriterName"), screenwriter.getName()));
            }
            range(builder, root.get("screenwriterHeight"),
                    screenwriter.getHeightFrom(), screenwriter.getHeightTo(), predicates);
            equalEnum(builder, root, "screenwriterEyeColor", screenwriter.getEyeColor(), predicates);
            equalEnum(builder, root, "screenwriterHairColor", screenwriter.getHairColor(), predicates);
            equalEnum(builder, root, "screenwriterNationality", screenwriter.getNationality(), predicates);
            if (screenwriter.getLocation() != null) {
                range(builder, root.get("screenwriterLocationX"),
                        screenwriter.getLocation().getxFrom(), screenwriter.getLocation().getxTo(), predicates);
                range(builder, root.get("screenwriterLocationY"),
                        screenwriter.getLocation().getyFrom(), screenwriter.getLocation().getyTo(), predicates);
                range(builder, root.get("screenwriterLocationZ"),
                        screenwriter.getLocation().getzFrom(), screenwriter.getLocation().getzTo(), predicates);
            }
        }

        return predicates;
    }

    private static Predicate containsIgnoreCase(jakarta.persistence.criteria.CriteriaBuilder builder,
                                                jakarta.persistence.criteria.Path<String> path, String value) {
        return builder.like(builder.lower(path), "%" + value.toLowerCase(Locale.ROOT) + "%");
    }

    private static void range(jakarta.persistence.criteria.CriteriaBuilder builder,
                              jakarta.persistence.criteria.Expression<? extends Number> path,
                              Number from, Number to, List<Predicate> predicates) {
        if (from != null) {
            predicates.add(builder.gt(path, from));
        }
        if (to != null) {
            predicates.add(builder.lt(path, to));
        }
    }

    private static void equalEnum(jakarta.persistence.criteria.CriteriaBuilder builder,
                                  jakarta.persistence.criteria.Root<MovieEntity> root, String attribute,
                                  Enum<?> value, List<Predicate> predicates) {
        if (value != null) {
            predicates.add(builder.equal(root.get(attribute), value));
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

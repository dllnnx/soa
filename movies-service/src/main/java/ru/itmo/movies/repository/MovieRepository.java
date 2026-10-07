package ru.itmo.movies.repository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaDelete;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import ru.itmo.movies.model.CoordinatesFilter;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieGenre;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.model.PersonFilter;

@ApplicationScoped
public class MovieRepository {

    @PersistenceContext(unitName = "moviesPU")
    private EntityManager entityManager;

    public MovieEntity insert(MovieEntity entity) {
        entityManager.persist(entity);
        entityManager.flush();
        return entity;
    }

    public Optional<MovieEntity> find(int id) {
        return Optional.ofNullable(entityManager.find(MovieEntity.class, (long) id));
    }

    public void delete(MovieEntity entity) {
        entityManager.remove(entity);
    }

    public MovieEntityPage filter(MovieFilter filter, int page, int size, SortOrder sort) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();

        CriteriaQuery<Long> countCriteria = builder.createQuery(Long.class);
        Root<MovieEntity> countRoot = countCriteria.from(MovieEntity.class);
        countCriteria.select(builder.count(countRoot));
        countCriteria.where(buildPredicates(filter, builder, countRoot));
        long totalElements = entityManager.createQuery(countCriteria).getSingleResult();

        List<MovieEntity> content = List.of();
        if (totalElements > 0) {
            CriteriaQuery<MovieEntity> pageCriteria = builder.createQuery(MovieEntity.class);
            Root<MovieEntity> pageRoot = pageCriteria.from(MovieEntity.class);
            pageCriteria.select(pageRoot);
            pageCriteria.where(buildPredicates(filter, builder, pageRoot));

            var primaryOrder = sort.ascending()
                    ? builder.asc(pageRoot.get(sort.key().getAttribute()))
                    : builder.desc(pageRoot.get(sort.key().getAttribute()));
            pageCriteria.orderBy(primaryOrder, builder.asc(pageRoot.get("id")));

            content = entityManager.createQuery(pageCriteria)
                    .setFirstResult(page * size)
                    .setMaxResults(size)
                    .getResultList();
        }

        return new MovieEntityPage(content, page, size, totalElements);
    }

    public double averageBudget() {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Double> criteria = builder.createQuery(Double.class);
        Root<MovieEntity> root = criteria.from(MovieEntity.class);
        criteria.select(builder.avg(root.get("budget")))
                .where(builder.isNotNull(root.get("budget")));
        Double average = entityManager.createQuery(criteria).getSingleResult();
        return average == null ? 0 : average;
    }

    public long deleteByMpaaRating(MpaaRating rating) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaDelete<MovieEntity> criteria = builder.createCriteriaDelete(MovieEntity.class);
        Root<MovieEntity> root = criteria.from(MovieEntity.class);
        criteria.where(builder.equal(root.get("mpaaRating"), rating.name()));
        return entityManager.createQuery(criteria).executeUpdate();
    }

    public boolean deleteByScreenwriter(String name) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<MovieEntity> criteria = builder.createQuery(MovieEntity.class);
        Root<MovieEntity> root = criteria.from(MovieEntity.class);
        criteria.select(root)
                .where(builder.equal(root.get("screenwriterName"), name))
                .orderBy(builder.asc(root.get("id")));

        TypedQuery<MovieEntity> query = entityManager.createQuery(criteria).setMaxResults(1);
        List<MovieEntity> matches = query.getResultList();
        if (matches.isEmpty()) {
            return false;
        }
        entityManager.remove(matches.get(0));
        return true;
    }

    private Predicate[] buildPredicates(MovieFilter filter, CriteriaBuilder builder, Root<MovieEntity> root) {
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
            predicates.add(builder.lessThan(
                    root.get("creationDate"), filter.getCreationDateBefore()));
        }
        if (filter.getCreationDateAfter() != null) {
            predicates.add(builder.greaterThan(
                    root.get("creationDate"), filter.getCreationDateAfter()));
        }
        range(builder, root.get("oscarsCount"),
                filter.getOscarsCountFrom(), filter.getOscarsCountTo(), predicates);
        range(builder, root.get("budget"), filter.getBudgetFrom(), filter.getBudgetTo(), predicates);
        if (filter.getGenres() != null && !filter.getGenres().isEmpty()) {
            predicates.add(root.<String>get("genre")
                    .in(filter.getGenres().stream().map(MovieGenre::name).toList()));
        }
        if (filter.getMpaaRating() != null) {
            predicates.add(builder.equal(root.get("mpaaRating"), filter.getMpaaRating().name()));
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

        return predicates.toArray(Predicate[]::new);
    }

    private Predicate containsIgnoreCase(CriteriaBuilder builder, Path<String> path, String value) {
        return builder.like(builder.lower(path), "%" + value.toLowerCase(Locale.ROOT) + "%");
    }

    private void range(CriteriaBuilder builder, Expression<? extends Number> path,
                       Number from, Number to, List<Predicate> predicates) {
        if (from != null) {
            predicates.add(builder.gt(path, from));
        }
        if (to != null) {
            predicates.add(builder.lt(path, to));
        }
    }

    private void equalEnum(CriteriaBuilder builder, Root<MovieEntity> root, String attribute,
                           Enum<?> value, List<Predicate> predicates) {
        if (value != null) {
            predicates.add(builder.equal(root.get(attribute), value.name()));
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

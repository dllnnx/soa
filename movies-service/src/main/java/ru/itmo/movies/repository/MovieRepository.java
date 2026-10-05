package ru.itmo.movies.repository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.Resource;
import jakarta.enterprise.context.ApplicationScoped;

import javax.sql.DataSource;

import ru.itmo.movies.model.Country;
import ru.itmo.movies.model.Coordinates;
import ru.itmo.movies.model.CoordinatesFilter;
import ru.itmo.movies.model.EyeColor;
import ru.itmo.movies.model.Location;
import ru.itmo.movies.model.Movie;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieGenre;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.MoviePage;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.model.Person;
import ru.itmo.movies.model.PersonFilter;

@ApplicationScoped
public class MovieRepository {

    @Resource(lookup = "jdbc/MoviesDS")
    private DataSource dataSource;

    private static final String COLUMNS = """
            id, name, coord_x, coord_y, creation_date, oscars_count, budget,
            genre, mpaa_rating, sw_name, sw_height, sw_eye_color, sw_hair_color,
            sw_nationality, sw_loc_x, sw_loc_y, sw_loc_z""";

    public Movie insert(MovieInput movie) {
        return query(connection -> {
            String sql = "INSERT INTO movie (name, coord_x, coord_y, oscars_count, budget, genre, mpaa_rating, "
                    + "sw_name, sw_height, sw_eye_color, sw_hair_color, sw_nationality, sw_loc_x, sw_loc_y, sw_loc_z) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING " + COLUMNS;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindMovie(ps, movie);
                return mapOne(ps);
            }
        });
    }

    public Optional<Movie> find(int id) {
        return query(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT " + COLUMNS + " FROM movie WHERE id = ?")) {
                ps.setInt(1, id);
                ResultSet rs = ps.executeQuery();
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        });
    }

    public Movie update(int id, MovieInput movie) {
        return query(connection -> {
            String sql = "UPDATE movie SET name = ?, coord_x = ?, coord_y = ?, oscars_count = ?, budget = ?, "
                    + "genre = ?, mpaa_rating = ?, sw_name = ?, sw_height = ?, sw_eye_color = ?, sw_hair_color = ?, "
                    + "sw_nationality = ?, sw_loc_x = ?, sw_loc_y = ?, sw_loc_z = ? WHERE id = ? RETURNING " + COLUMNS;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                bindMovie(ps, movie);
                ps.setInt(16, id);
                return mapOne(ps);
            }
        });
    }

    public boolean delete(int id) {
        return query(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM movie WHERE id = ?")) {
                ps.setInt(1, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    public MoviePage filter(MovieFilter filter, int page, int size, SortOrder sort) {
        return query(connection -> {
            List<String> conditions = new ArrayList<>();
            List<Object> params = new ArrayList<>();
            appendFilters(connection, filter, conditions, params);
            String where = conditions.isEmpty() ? "" : " WHERE " + String.join(" AND ", conditions);

            long totalElements = count(connection, where, params);
            List<Movie> content = totalElements == 0
                    ? List.of()
                    : selectPage(connection, where, params, sort, page, size);

            MoviePage result = new MoviePage();
            result.setPage(page);
            result.setSize(size);
            result.setTotalElements(totalElements);
            result.setTotalPages((int) Math.ceil(totalElements / (double) size));
            result.setContent(content);
            return result;
        });
    }

    public double averageBudget() {
        return query(connection -> {
            try (PreparedStatement ps = connection.prepareStatement(
                    "SELECT COALESCE(AVG(budget), 0) FROM movie WHERE budget IS NOT NULL")) {
                ResultSet rs = ps.executeQuery();
                rs.next();
                return rs.getDouble(1);
            }
        });
    }

    public long deleteByMpaaRating(MpaaRating rating) {
        return query(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("DELETE FROM movie WHERE mpaa_rating = ?")) {
                ps.setString(1, rating.name());
                return (long) ps.executeUpdate();
            }
        });
    }

    public boolean deleteByScreenwriter(String name) {
        return query(connection -> {
            String sql = "DELETE FROM movie WHERE id = (SELECT id FROM movie WHERE sw_name = ? ORDER BY id LIMIT 1)";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, name);
                return ps.executeUpdate() > 0;
            }
        });
    }

    private void bindMovie(PreparedStatement ps, MovieInput movie) throws SQLException {
        ps.setString(1, movie.getName());
        ps.setDouble(2, movie.getCoordinates().getX());
        ps.setDouble(3, movie.getCoordinates().getY());
        ps.setLong(4, movie.getOscarsCount());
        setObject(ps, 5, movie.getBudget(), Types.INTEGER);
        ps.setString(6, movie.getGenre().name());
        ps.setString(7, movie.getMpaaRating().name());
        setPerson(ps, 8, movie.getScreenwriter());
    }

    private void setPerson(PreparedStatement ps, int first, Person person) throws SQLException {
        if (person == null) {
            ps.setNull(first, Types.VARCHAR);
            ps.setNull(first + 1, Types.DOUBLE);
            ps.setNull(first + 2, Types.VARCHAR);
            ps.setNull(first + 3, Types.VARCHAR);
            ps.setNull(first + 4, Types.VARCHAR);
            ps.setNull(first + 5, Types.REAL);
            ps.setNull(first + 6, Types.INTEGER);
            ps.setNull(first + 7, Types.BIGINT);
            return;
        }
        ps.setString(first, person.getName());
        ps.setDouble(first + 1, person.getHeight());
        ps.setString(first + 2, person.getEyeColor().name());
        ps.setString(first + 3, person.getHairColor() == null ? null : person.getHairColor().getValue());
        ps.setString(first + 4, person.getNationality().name());
        ps.setFloat(first + 5, person.getLocation().getX());
        ps.setInt(first + 6, person.getLocation().getY());
        ps.setLong(first + 7, person.getLocation().getZ());
    }

    private void appendFilters(Connection connection, MovieFilter filter,
                               List<String> conditions, List<Object> params) throws SQLException {
        if (filter.getIds() != null && !filter.getIds().isEmpty()) {
            conditions.add("id = ANY(?)");
            params.add(connection.createArrayOf("integer", filter.getIds().toArray(new Integer[0])));
        }
        if (notBlank(filter.getName())) {
            conditions.add("name ILIKE ?");
            params.add("%" + filter.getName() + "%");
        }
        CoordinatesFilter coordinates = filter.getCoordinates();
        if (coordinates != null) {
            range(conditions, params, "coord_x", coordinates.getxFrom(), coordinates.getxTo());
            range(conditions, params, "coord_y", coordinates.getyFrom(), coordinates.getyTo());
        }
        if (filter.getCreationDateBefore() != null) {
            conditions.add("creation_date < ?");
            params.add(filter.getCreationDateBefore());
        }
        if (filter.getCreationDateAfter() != null) {
            conditions.add("creation_date > ?");
            params.add(filter.getCreationDateAfter());
        }
        range(conditions, params, "oscars_count", filter.getOscarsCountFrom(), filter.getOscarsCountTo());
        range(conditions, params, "budget", filter.getBudgetFrom(), filter.getBudgetTo());
        if (filter.getGenres() != null && !filter.getGenres().isEmpty()) {
            conditions.add("genre = ANY(?)");
            params.add(connection.createArrayOf("varchar",
                    filter.getGenres().stream().map(MovieGenre::name).toArray()));
        }
        if (filter.getMpaaRating() != null) {
            conditions.add("mpaa_rating = ?");
            params.add(filter.getMpaaRating().name());
        }
        PersonFilter screenwriter = filter.getScreenwriter();
        if (screenwriter != null) {
            if (notBlank(screenwriter.getName())) {
                conditions.add("sw_name ILIKE ?");
                params.add("%" + screenwriter.getName() + "%");
            }
            range(conditions, params, "sw_height", screenwriter.getHeightFrom(), screenwriter.getHeightTo());
            eq(conditions, params, "sw_eye_color", screenwriter.getEyeColor());
            eq(conditions, params, "sw_hair_color", screenwriter.getHairColor());
            eq(conditions, params, "sw_nationality", screenwriter.getNationality());
            if (screenwriter.getLocation() != null) {
                range(conditions, params, "sw_loc_x",
                        screenwriter.getLocation().getxFrom(), screenwriter.getLocation().getxTo());
                range(conditions, params, "sw_loc_y",
                        screenwriter.getLocation().getyFrom(), screenwriter.getLocation().getyTo());
                range(conditions, params, "sw_loc_z",
                        screenwriter.getLocation().getzFrom(), screenwriter.getLocation().getzTo());
            }
        }
    }

    private long count(Connection connection, String where, List<Object> params) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM movie" + where)) {
            bind(ps, params);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getLong(1);
        }
    }

    private List<Movie> selectPage(Connection connection, String where, List<Object> params,
                                   SortOrder sort, int page, int size) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM movie" + where + " ORDER BY " + sort.key().getColumn()
                + (sort.ascending() ? " ASC" : " DESC") + ", id LIMIT ? OFFSET ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            bind(ps, params);
            ps.setInt(params.size() + 1, size);
            ps.setInt(params.size() + 2, page * size);
            ResultSet rs = ps.executeQuery();
            List<Movie> movies = new ArrayList<>();
            while (rs.next()) {
                movies.add(mapRow(rs));
            }
            return movies;
        }
    }

    private void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        int index = 1;
        for (Object param : params) {
            if (param instanceof Array array) {
                ps.setArray(index, array);
            } else {
                ps.setObject(index, param);
            }
            index++;
        }
    }

    private void range(List<String> conditions, List<Object> params, String column, Number from, Number to) {
        if (from != null) {
            conditions.add(column + " > ?");
            params.add(from);
        }
        if (to != null) {
            conditions.add(column + " < ?");
            params.add(to);
        }
    }

    private void eq(List<String> conditions, List<Object> params, String column, Enum<?> value) {
        if (value != null) {
            conditions.add(column + " = ?");
            params.add(value.name());
        }
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private Movie mapOne(PreparedStatement ps) throws SQLException {
        ResultSet rs = ps.executeQuery();
        return rs.next() ? mapRow(rs) : null;
    }

    private Movie mapRow(ResultSet rs) throws SQLException {
        Movie movie = new Movie();
        movie.setId(rs.getInt("id"));
        movie.setName(rs.getString("name"));
        Coordinates coordinates = new Coordinates();
        coordinates.setX(rs.getDouble("coord_x"));
        coordinates.setY(rs.getDouble("coord_y"));
        movie.setCoordinates(coordinates);
        movie.setCreationDate(rs.getObject("creation_date", OffsetDateTime.class));
        movie.setOscarsCount(rs.getLong("oscars_count"));
        int budget = rs.getInt("budget");
        movie.setBudget(rs.wasNull() ? null : budget);
        movie.setGenre(MovieGenre.valueOf(rs.getString("genre")));
        movie.setMpaaRating(MpaaRating.valueOf(rs.getString("mpaa_rating")));
        movie.setScreenwriter(mapPerson(rs));
        return movie;
    }

    private Person mapPerson(ResultSet rs) throws SQLException {
        String name = rs.getString("sw_name");
        if (name == null) {
            return null;
        }
        Person person = new Person();
        person.setName(name);
        person.setHeight(rs.getDouble("sw_height"));
        person.setEyeColor(EyeColor.valueOf(rs.getString("sw_eye_color")));
        String hairColor = rs.getString("sw_hair_color");
        person.setHairColor(hairColor == null ? null : Person.HairColorEnum.fromValue(hairColor));
        person.setNationality(Country.valueOf(rs.getString("sw_nationality")));
        Location location = new Location();
        location.setX(rs.getFloat("sw_loc_x"));
        location.setY(rs.getInt("sw_loc_y"));
        location.setZ(rs.getLong("sw_loc_z"));
        person.setLocation(location);
        return person;
    }

    private void setObject(PreparedStatement ps, int index, Number value, int type) throws SQLException {
        if (value == null) {
            ps.setNull(index, type);
        } else {
            ps.setObject(index, value, type);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T run(Connection connection) throws SQLException;
    }

    private <T> T query(SqlCall<T> call) {
        try (Connection connection = dataSource.getConnection()) {
            return call.run(connection);
        } catch (SQLException e) {
            throw new IllegalStateException("Ошибка обращения к базе данных", e);
        }
    }
}

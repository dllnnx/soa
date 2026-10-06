package ru.itmo.movies.validation;

import java.util.ArrayList;
import java.util.List;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.model.Coordinates;
import ru.itmo.movies.model.CoordinatesFilter;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.error.FieldError;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.Person;
import ru.itmo.movies.model.PersonFilter;

public final class MovieValidator {

    private MovieValidator() {
    }

    public static void validateForCreate(MovieInput movie) {
        fail(collect(movie));
    }

    public static void validateForUpdate(MovieInput movie) {
        fail(collect(movie));
    }

    public static void validateFilter(MovieFilter filter) {
        List<FieldError> errors = new ArrayList<>();
        string(errors, "name", filter.getName(), true);
        if (filter.getIds() != null && filter.getIds().stream().anyMatch(id -> id == null || id < 1)) {
            add(errors, "ids", "Идентификаторы должны быть целыми числами больше 0");
        }
        CoordinatesFilter c = filter.getCoordinates();
        if (c != null) {
            if (c.getxFrom() != null && c.getxFrom() <= -130) {
                add(errors, "coordinates.xFrom", "Значение поля должно быть больше -130");
            }
            if (c.getyTo() != null && c.getyTo() > 388) {
                add(errors, "coordinates.yTo", "Максимальное значение поля: 388");
            }
        }
        if (filter.getOscarsCountFrom() != null && filter.getOscarsCountFrom() < 0) {
            add(errors, "oscarsCountFrom", "Значение поля должно быть не меньше 0");
        }
        if (filter.getOscarsCountTo() != null && filter.getOscarsCountTo() < 0) {
            add(errors, "oscarsCountTo", "Значение поля должно быть не меньше 0");
        }
        if (filter.getBudgetFrom() != null && filter.getBudgetFrom() < 1) {
            add(errors, "budgetFrom", "Значение поля должно быть больше 0");
        }
        if (filter.getBudgetTo() != null && filter.getBudgetTo() < 1) {
            add(errors, "budgetTo", "Значение поля должно быть больше 0");
        }
        PersonFilter s = filter.getScreenwriter();
        if (s != null) {
            string(errors, "screenwriter.name", s.getName(), true);
            if (s.getHeightFrom() != null && s.getHeightFrom() <= 0) {
                add(errors, "screenwriter.heightFrom", "Значение поля должно быть больше 0");
            }
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.INVALID_PARAMETER, "Некорректные параметры фильтрации", errors);
        }
    }

    private static List<FieldError> collect(MovieInput movie) {
        List<FieldError> errors = new ArrayList<>();
        string(errors, "name", movie.getName(), false);

        Coordinates coordinates = movie.getCoordinates();
        if (coordinates == null) {
            add(errors, "coordinates", "Поле не может быть null");
        } else {
            if (coordinates.getX() == null) {
                add(errors, "coordinates.x", "Поле не может быть null");
            } else if (coordinates.getX() <= -130) {
                add(errors, "coordinates.x", "Значение поля должно быть больше -130");
            }
            if (coordinates.getY() == null) {
                add(errors, "coordinates.y", "Поле не может быть null");
            } else if (coordinates.getY() > 388) {
                add(errors, "coordinates.y", "Максимальное значение поля: 388");
            }
        }

        if (movie.getOscarsCount() == null) {
            add(errors, "oscarsCount", "Поле не может быть null");
        } else if (movie.getOscarsCount() < 0) {
            add(errors, "oscarsCount", "Значение поля должно быть не меньше 0");
        }

        if (movie.getBudget() != null && movie.getBudget() < 1) {
            add(errors, "budget", "Значение поля должно быть больше 0");
        }
        if (movie.getGenre() == null) {
            add(errors, "genre", "Поле не может быть null");
        }
        if (movie.getMpaaRating() == null) {
            add(errors, "mpaaRating", "Поле не может быть null");
        }
        if (movie.getScreenwriter() != null) {
            person(errors, "screenwriter", movie.getScreenwriter());
        }
        return errors;
    }

    private static void person(List<FieldError> errors, String prefix, Person person) {
        string(errors, prefix + ".name", person.getName(), false);
        if (person.getHeight() == null) {
            add(errors, prefix + ".height", "Поле не может быть null");
        } else if (person.getHeight() <= 0) {
            add(errors, prefix + ".height", "Значение поля должно быть больше 0");
        }
        if (person.getEyeColor() == null) {
            add(errors, prefix + ".eyeColor", "Поле не может быть null");
        }
        if (person.getNationality() == null) {
            add(errors, prefix + ".nationality", "Поле не может быть null");
        }
        if (person.getLocation() == null) {
            add(errors, prefix + ".location", "Поле не может быть null");
        } else {
            if (person.getLocation().getX() == null) {
                add(errors, prefix + ".location.x", "Поле не может быть null");
            }
            if (person.getLocation().getY() == null) {
                add(errors, prefix + ".location.y", "Поле не может быть null");
            }
            if (person.getLocation().getZ() == null) {
                add(errors, prefix + ".location.z", "Поле не может быть null");
            }
        }
    }

    private static void string(List<FieldError> errors, String field, String value, boolean nullable) {
        if (value == null) {
            if (!nullable) {
                add(errors, field, "Поле не может быть null");
            }
            return;
        }
        if (value.isBlank()) {
            add(errors, field, "Строка не может быть пустой");
        } else if (!Patterns.STRING.matcher(value).matches()) {
            add(errors, field, "Строка содержит недопустимые символы или длиннее 255 символов");
        }
    }

    private static void fail(List<FieldError> errors) {
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Нарушены ограничения целостности", errors);
        }
    }

    private static void add(List<FieldError> errors, String field, String message) {
        errors.add(new FieldError(field, message));
    }
}

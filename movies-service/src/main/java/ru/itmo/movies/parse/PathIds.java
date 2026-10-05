package ru.itmo.movies.parse;

import java.util.regex.Pattern;

import ru.itmo.movies.exception.ApiException;
import ru.itmo.movies.model.ErrorCode;

public final class PathIds {

    private static final Pattern MOVIE_ID = Pattern.compile("^[1-9][0-9]{0,9}$");

    private PathIds() {
    }

    public static int movieId(String raw) {
        if (raw == null || !MOVIE_ID.matcher(raw).matches() || Long.parseLong(raw) > Integer.MAX_VALUE) {
            throw new ApiException(ErrorCode.INVALID_PARAMETER,
                    "Идентификатор фильма должен быть целым числом от 1 до 2147483647, получено: \"" + raw + "\"");
        }
        return Integer.parseInt(raw);
    }
}

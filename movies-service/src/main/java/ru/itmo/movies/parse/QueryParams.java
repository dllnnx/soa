package ru.itmo.movies.parse;

import java.util.List;
import java.util.Set;

import jakarta.ws.rs.core.UriInfo;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;
import ru.itmo.movies.model.MpaaRating;
import ru.itmo.movies.repository.SortKey;
import ru.itmo.movies.repository.SortOrder;
import ru.itmo.movies.validation.Patterns;

public final class QueryParams {

    private QueryParams() {
    }

    public static void allow(UriInfo uri, String... allowed) {
        Set<String> permitted = Set.of(allowed);
        for (String name : uri.getQueryParameters().keySet()) {
            if (!permitted.contains(name)) {
                throw new ApiException(ErrorCode.UNKNOWN_PARAMETER, "Неизвестный query-параметр: " + name);
            }
        }
    }

    public static int page(UriInfo uri) {
        return range(uri, "page", 0, 100000, 0);
    }

    public static int size(UriInfo uri) {
        return range(uri, "size", 1, 100, 10);
    }

    public static SortOrder sort(UriInfo uri) {
        String raw = first(uri, "sort");
        return raw == null ? new SortOrder(SortKey.ID, true) : SortOrder.parse(raw);
    }

    public static MpaaRating mpaaRating(UriInfo uri) {
        String raw = required(uri, "mpaa-rating");
        try {
            return MpaaRating.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw invalid("mpaa-rating", raw, "одно из значений: G, PG, PG_13, R, NC_17");
        }
    }

    public static String screenwriterName(UriInfo uri) {
        String raw = required(uri, "name");
        if (!Patterns.STRING.matcher(raw).matches()) {
            throw invalid("name", raw, "строка из 1-255 допустимых символов");
        }
        return raw;
    }

    private static int range(UriInfo uri, String name, int min, int max, int fallback) {
        String raw = first(uri, name);
        if (raw == null) {
            return fallback;
        }
        try {
            int value = Integer.parseInt(raw);
            if (value >= min && value <= max) {
                return value;
            }
        } catch (NumberFormatException ignored) {
        }
        throw invalid(name, raw, "целое число от " + min + " до " + max);
    }

    private static String required(UriInfo uri, String name) {
        String raw = first(uri, name);
        if (raw == null) {
            throw new ApiException(ErrorCode.INVALID_PARAMETER, "Обязательный query-параметр \"" + name + "\" не задан");
        }
        return raw;
    }

    private static String first(UriInfo uri, String name) {
        List<String> values = uri.getQueryParameters().get(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }

    private static ApiException invalid(String name, String raw, String expected) {
        return new ApiException(ErrorCode.INVALID_PARAMETER,
                "Параметр \"" + name + "\" невалиден: \"" + raw + "\", ожидается " + expected);
    }
}

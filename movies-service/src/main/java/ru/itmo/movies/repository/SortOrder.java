package ru.itmo.movies.repository;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import ru.itmo.movies.error.ApiException;
import ru.itmo.movies.error.ErrorCode;

public record SortOrder(SortKey key, boolean ascending) {

    private static final String FIELDS = Arrays.stream(SortKey.values())
            .map(key -> key.name().toLowerCase(Locale.ROOT).replace('_', '-'))
            .collect(Collectors.joining("|"));
    private static final Pattern SORT = Pattern.compile("^(?<field>" + FIELDS + ")(?<direction>,(asc|desc))?$");

    public static SortOrder parse(String raw) {
        var matcher = SORT.matcher(raw);
        if (matcher.matches()) {
            SortKey key = SortKey.valueOf(matcher.group("field").toUpperCase(Locale.ROOT).replace('-', '_'));
            String direction = matcher.group("direction");
            boolean ascending = direction == null || !"desc".equals(direction.substring(1));
            return new SortOrder(key, ascending);
        }
        throw new ApiException(ErrorCode.INVALID_SORT_EXPRESSION,
                "Параметр \"sort\" должен иметь вид \"поле[,asc|desc]\"");
    }
}

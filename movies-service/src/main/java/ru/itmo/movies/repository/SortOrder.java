package ru.itmo.movies.repository;

import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public record SortOrder(SortKey key, boolean ascending) {

    private static final String FIELDS = Arrays.stream(SortKey.values())
            .map(key -> key.name().toLowerCase(Locale.ROOT).replace('_', '-'))
            .collect(Collectors.joining("|"));
    private static final Pattern SORT = Pattern.compile("^(?<field>" + FIELDS + ")(?<direction>,(asc|desc))?$");
}

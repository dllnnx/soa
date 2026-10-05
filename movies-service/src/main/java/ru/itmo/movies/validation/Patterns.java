package ru.itmo.movies.validation;

import java.util.regex.Pattern;

public final class Patterns {

    public static final Pattern STRING = Pattern.compile("^[a-zA-Zа-яА-ЯЁё0-9 .,:!?()&-]{1,255}$");

    private Patterns() {
    }
}

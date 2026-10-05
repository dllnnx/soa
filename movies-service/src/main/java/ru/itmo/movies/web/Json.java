package ru.itmo.movies.web;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.json.bind.adapter.JsonbAdapter;

public final class Json {

    private static final Jsonb JSONB = JsonbBuilder.create(new JsonbConfig()
            .withAdapters(new ZonedDateTimeAdapter(), new OffsetDateTimeAdapter()));

    private Json() {
    }

    public static Jsonb jsonb() {
        return JSONB;
    }

    public static String write(Object value) {
        return JSONB.toJson(value);
    }

    public static class ZonedDateTimeAdapter implements JsonbAdapter<ZonedDateTime, String> {

        @Override
        public String adaptToJson(ZonedDateTime value) {
            return DateTimeFormatter.ISO_INSTANT.format(value.toInstant());
        }

        @Override
        public ZonedDateTime adaptFromJson(String value) {
            return Instant.parse(value).atZone(ZoneOffset.UTC);
        }
    }

    public static class OffsetDateTimeAdapter implements JsonbAdapter<OffsetDateTime, String> {

        @Override
        public String adaptToJson(OffsetDateTime value) {
            return DateTimeFormatter.ISO_INSTANT.format(value.toInstant());
        }

        @Override
        public OffsetDateTime adaptFromJson(String value) {
            return OffsetDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
        }
    }
}

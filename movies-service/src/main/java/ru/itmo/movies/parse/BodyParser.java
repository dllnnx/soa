package ru.itmo.movies.parse;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import ru.itmo.movies.exception.ApiException;
import ru.itmo.movies.model.Coordinates;
import ru.itmo.movies.model.CoordinatesFilter;
import ru.itmo.movies.model.ErrorCode;
import ru.itmo.movies.model.Location;
import ru.itmo.movies.model.LocationFilter;
import ru.itmo.movies.model.MovieFilter;
import ru.itmo.movies.model.MovieInput;
import ru.itmo.movies.model.Person;
import ru.itmo.movies.model.PersonFilter;

public final class BodyParser {

    private static final Set<String> READ_ONLY_FIELDS = Set.of("id", "creationDate");
    private static final Set<Class<?>> NESTED_TYPES = Set.of(
            Coordinates.class, Person.class, Location.class,
            CoordinatesFilter.class, LocationFilter.class, PersonFilter.class);
    private static final Map<Class<?>, Map<String, Field>> FIELDS = new ConcurrentHashMap<>();

    private BodyParser() {
    }

    public static <T> T parse(String body, Class<T> type) {
        JsonObject json = readObject(body);
        check(json, type, "");
        try {
            return ru.itmo.movies.web.Json.jsonb().fromJson(body, type);
        } catch (Exception e) {
            throw new ApiException(ErrorCode.INVALID_PARAMETER, "Тело запроса не соответствует схеме");
        }
    }

    private static JsonObject readObject(String body) {
        if (body == null || body.isBlank()) {
            throw new ApiException(ErrorCode.MALFORMED_JSON, "Тело запроса отсутствует или не является JSON-объектом");
        }
        try (JsonReader reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        } catch (Exception e) {
            throw new ApiException(ErrorCode.MALFORMED_JSON, "Тело запроса не является корректным JSON");
        }
    }

    private static void check(JsonObject json, Class<?> type, String path) {
        for (String key : json.keySet()) {
            String fieldPath = path.isEmpty() ? key : path + "." + key;
            Field field = fields(type).get(key);
            if (field == null) {
                if (type == MovieInput.class && READ_ONLY_FIELDS.contains(key)) {
                    throw new ApiException(ErrorCode.READ_ONLY_FIELD, "Поле \"" + fieldPath + "\" только для чтения");
                }
                throw new ApiException(ErrorCode.UNKNOWN_PROPERTY, "Неизвестное свойство: " + fieldPath);
            }
            JsonValue value = json.get(key);
            if (value == JsonValue.NULL) {
                continue;
            }
            if (isList(field)) {
                if (!(value instanceof JsonArray array)) {
                    throw invalid(fieldPath, "ожидается массив");
                }
                for (JsonValue item : array) {
                    checkLeaf(item, leafType(field), fieldPath + "[]");
                }
            } else if (NESTED_TYPES.contains(field.getType())) {
                if (!(value instanceof JsonObject nested)) {
                    throw invalid(fieldPath, "ожидается объект");
                }
                check(nested, field.getType(), fieldPath);
            } else {
                checkLeaf(value, leafType(field), fieldPath);
            }
        }
    }

    private static void checkLeaf(JsonValue value, Class<?> leafType, String path) {
        if (leafType.isEnum()) {
            if (!(value instanceof JsonString s) || !enumContains(leafType, s.getString())) {
                throw invalid(path, "ожидается одно из значений: " + String.join(", ", enumNames(leafType)));
            }
        } else if (leafType == OffsetDateTime.class || leafType == ZonedDateTime.class) {
            if (!(value instanceof JsonString s) || !isIsoDateTime(s.getString())) {
                throw invalid(path, "ожидается дата-время в формате ISO-8601");
            }
        } else if (Number.class.isAssignableFrom(leafType)) {
            if (!(value instanceof JsonNumber)) {
                throw invalid(path, "ожидается число");
            }
        } else if (leafType == String.class && !(value instanceof JsonString)) {
            throw invalid(path, "ожидается строка");
        }
    }

    private static boolean isIsoDateTime(String raw) {
        try {
            OffsetDateTime.parse(raw);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private static boolean enumContains(Class<?> enumType, String raw) {
        return Arrays.stream(enumNames(enumType)).anyMatch(name -> name.equals(raw));
    }

    private static String[] enumNames(Class<?> enumType) {
        return Arrays.stream(enumType.getEnumConstants()).map(constant -> ((Enum<?>) constant).name())
                .toArray(String[]::new);
    }

    private static Map<String, Field> fields(Class<?> type) {
        return FIELDS.computeIfAbsent(type, t -> {
            Map<String, Field> byName = new HashMap<>();
            for (Field field : t.getDeclaredFields()) {
                byName.put(field.getName(), field);
            }
            return byName;
        });
    }

    private static boolean isList(Field field) {
        Type generic = field.getGenericType();
        return generic instanceof ParameterizedType parameterized && parameterized.getRawType() == List.class;
    }

    private static Class<?> leafType(Field field) {
        Type generic = field.getGenericType();
        if (generic instanceof ParameterizedType parameterized && parameterized.getRawType() == List.class) {
            return (Class<?>) parameterized.getActualTypeArguments()[0];
        }
        return box(field.getType());
    }

    private static Class<?> box(Class<?> type) {
        return switch (type.getName()) {
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "double" -> Double.class;
            case "float" -> Float.class;
            default -> type;
        };
    }

    private static ApiException invalid(String path, String expected) {
        return new ApiException(ErrorCode.INVALID_PARAMETER, "Свойство \"" + path + "\" невалидно: " + expected);
    }
}

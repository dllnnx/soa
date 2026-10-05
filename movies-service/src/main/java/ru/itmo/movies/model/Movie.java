package ru.itmo.movies.model;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Movie {

    private Integer id; // > 0, уникальный, генерируется автоматически
    private String name; // not null, не пустая
    private Coordinates coordinates; // not null
    private ZonedDateTime creationDate; // not null, генерируется автоматически
    private long oscarsCount; // > 0 на входе клиента; 0 допустим после обнуления сервисом /oscar
    private Integer budget; // > 0, может быть null
    private MovieGenre genre; // not null
    private MpaaRating mpaaRating; // not null
    private Person screenwriter; // может быть null
}

package ru.itmo.movies.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.itmo.movies.model.MovieGenre;

@Converter(autoApply = true)
public class MovieGenreConverter implements AttributeConverter<MovieGenre, String> {

    @Override
    public String convertToDatabaseColumn(MovieGenre attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public MovieGenre convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MovieGenre.valueOf(dbData);
    }
}

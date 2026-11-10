package ru.itmo.movies.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.itmo.movies.model.MpaaRating;

@Converter(autoApply = true)
public class MpaaRatingConverter implements AttributeConverter<MpaaRating, String> {

    @Override
    public String convertToDatabaseColumn(MpaaRating attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public MpaaRating convertToEntityAttribute(String dbData) {
        return dbData == null ? null : MpaaRating.valueOf(dbData);
    }
}

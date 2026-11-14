package ru.itmo.movies.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.itmo.movies.model.EyeColor;

@Converter(autoApply = true)
public class EyeColorConverter implements AttributeConverter<EyeColor, String> {

    @Override
    public String convertToDatabaseColumn(EyeColor attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public EyeColor convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EyeColor.valueOf(dbData);
    }
}

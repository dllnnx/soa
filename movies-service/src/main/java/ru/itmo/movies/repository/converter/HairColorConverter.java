package ru.itmo.movies.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.itmo.movies.model.HairColor;

@Converter(autoApply = true)
public class HairColorConverter implements AttributeConverter<HairColor, String> {

    @Override
    public String convertToDatabaseColumn(HairColor attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public HairColor convertToEntityAttribute(String dbData) {
        return dbData == null ? null : HairColor.valueOf(dbData);
    }
}

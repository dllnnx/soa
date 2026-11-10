package ru.itmo.movies.repository.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.itmo.movies.model.Country;

@Converter(autoApply = true)
public class CountryConverter implements AttributeConverter<Country, String> {

    @Override
    public String convertToDatabaseColumn(Country attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Country convertToEntityAttribute(String dbData) {
        return dbData == null ? null : Country.valueOf(dbData);
    }
}

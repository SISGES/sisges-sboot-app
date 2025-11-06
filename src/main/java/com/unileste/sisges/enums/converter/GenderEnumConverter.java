package com.unileste.sisges.enums.converter;

import com.unileste.sisges.enums.GenderENUM;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class GenderEnumConverter implements AttributeConverter<GenderENUM, String> {

    @Override
    public String convertToDatabaseColumn(GenderENUM gender) {
        return gender != null ? gender.getCode() : null;
    }

    @Override
    public GenderENUM convertToEntityAttribute(String code) {
        return code != null ? GenderENUM.fromCode(code) : null;
    }
}
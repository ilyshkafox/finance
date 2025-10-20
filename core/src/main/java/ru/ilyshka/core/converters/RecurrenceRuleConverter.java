package ru.ilyshka.core.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.dmfs.rfc5545.recur.RecurrenceRule;

@Converter(autoApply = true)
public class RecurrenceRuleConverter implements AttributeConverter<RecurrenceRule, String> {

    @Override
    public String convertToDatabaseColumn(RecurrenceRule attribute) {
        return attribute != null ? attribute.toString() : null;
    }

    @Override
    public RecurrenceRule convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.trim().isEmpty()) {
            return null;
        }
        try {
            return new RecurrenceRule(dbData);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid recurrence rule from DB: " + dbData, e);
        }
    }
}

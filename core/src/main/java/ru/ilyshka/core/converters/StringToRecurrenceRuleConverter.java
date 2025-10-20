package ru.ilyshka.core.converters;

import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToRecurrenceRuleConverter implements Converter<String, RecurrenceRule> {

    @Override
    public RecurrenceRule convert(String source) {
        try {
            return new RecurrenceRule(source);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid recurrence rule: " + source, e);
        }
    }
}

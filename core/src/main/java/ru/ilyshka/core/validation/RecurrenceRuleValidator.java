package ru.ilyshka.core.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.dmfs.rfc5545.recur.InvalidRecurrenceRuleException;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.springframework.util.StringUtils;

public class RecurrenceRuleValidator implements ConstraintValidator<RecurrenceRuleFormat, String> {


    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {

        if (StringUtils.hasText(value)) {
            try {
                new RecurrenceRule(value.trim());
            } catch (InvalidRecurrenceRuleException e) {
                context.disableDefaultConstraintViolation();
                context.buildConstraintViolationWithTemplate(e.getMessage()).addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}


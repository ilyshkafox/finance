package ru.ilyshka.core.recurring_expense;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.dmfs.rfc5545.recur.RecurrenceRule;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.validator.constraints.Length;
import ru.ilyshka.core.converters.RecurrenceRuleConverter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Entity
public class RecurringExpense {
    @Id
    @UuidGenerator
    private UUID id;
    @NotEmpty
    @Length(min = 3)
    private String title;
    @NotNull
    private String description;
    @NotNull
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    @Convert(converter = RecurrenceRuleConverter.class)
    private RecurrenceRule rrule;
    @NotNull
    @Min(0)
    @Digits(integer = 6, fraction = 2)
    private BigDecimal amount;
}

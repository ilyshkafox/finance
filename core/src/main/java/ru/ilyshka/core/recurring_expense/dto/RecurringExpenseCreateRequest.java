package ru.ilyshka.core.recurring_expense.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import ru.ilyshka.core.validation.RecurrenceRuleFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class RecurringExpenseCreateRequest {
    @NotEmpty
    @Length(min = 3)
    private String title;
    @NotNull
    private String description;
    @NotNull
    private OffsetDateTime startDate;
    private OffsetDateTime endDate;
    @RecurrenceRuleFormat
    private String rrule;
    @NotNull
    @Min(0)
    @Digits(integer = 6, fraction = 2)
    private BigDecimal amount;
}

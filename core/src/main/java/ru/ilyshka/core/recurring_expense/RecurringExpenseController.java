package ru.ilyshka.core.recurring_expense;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ilyshka.core.recurring_expense.dto.RecurringExpenseCreateRequest;

@RestController
@RequestMapping("/api/recurring-expense")
@RequiredArgsConstructor
public class RecurringExpenseController {
    private final RecurringExpenseService service;

    @PostMapping()
    public RecurringExpense create(@Valid @RequestBody RecurringExpenseCreateRequest recurringExpense) {
        return null;
    }

}

package ru.ilyshka.core.recurring_expense;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RecurringExpenseService {
    private final RecurringExpenseRepository repository;

    @Transactional
    public RecurringExpense create(RecurringExpense expense) {
        expense.setId(null);
        return repository.save(expense);
    }


}

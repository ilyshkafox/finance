package ru.ilyshka.temporal.finance.vtb.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Запрос на получение транзакций из VTB.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VTBFetchRequest {
    private LocalDate startDate;
    private LocalDate endDate;
}
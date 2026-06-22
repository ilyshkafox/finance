package ru.ilyshka.modules.finance;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Builder
public record Transaction(
        @Null(groups = Groups.Add.class)
        UUID id,
        @NotNull
        UUID externalId,
        @NotNull
        OffsetDateTime transactionDate,
        @NotNull
        UUID accountId,
        UUID payeeId,
        String notes,
        BigDecimal amount,
        Map<String, String> additionInformation,
        String raw
) {


}

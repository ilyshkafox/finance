package ru.ilyshka.core.ledger.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record Transaction(
        UUID id,
        String owner,
        UUID externalId,
        OffsetDateTime transactionDate,
        UUID walletId,
        UUID payeeId,
        String notes,
        BigDecimal amount,
        Map<String, String> additionInformation,
        String raw
) {
}

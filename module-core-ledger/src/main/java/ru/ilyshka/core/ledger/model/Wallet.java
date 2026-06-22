package ru.ilyshka.core.ledger.model;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record Wallet(
        UUID id,
        String owner,
        String name,
        BigDecimal amount
) {
}

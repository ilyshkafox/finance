package ru.ilyshka.dto;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record Wallet(
        String id,
        WalletType type,
        String name,
        BigDecimal money
) {
}

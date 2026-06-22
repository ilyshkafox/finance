package ru.ilyshka.modules.finance;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Payee(
        UUID id,
        String name
) {
}

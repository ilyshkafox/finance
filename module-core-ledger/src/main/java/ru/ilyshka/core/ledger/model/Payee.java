package ru.ilyshka.core.ledger.model;

import java.util.UUID;

public record Payee(
        UUID id,
        String name
) {
}

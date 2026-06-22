package ru.ilyshka.modules.finance;

import lombok.Builder;

import java.util.UUID;

@Builder
public record Account(
        UUID id,
        String owner,
        String name
) {
}

package ru.ilyshka.core.ledger.service;

import ru.ilyshka.core.ledger.model.Wallet;

import java.math.BigDecimal;

public class WalletService {
    private final
    public Wallet createWallet(Wallet create) {
        Wallet wallet = Wallet.builder()
                .owner(create.owner())
                .name(create.name())
                .amount(create.amount() == null ? BigDecimal.ZERO : create.amount())
                .build();



    }
}

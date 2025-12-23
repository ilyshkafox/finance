package ru.ilyshka.core.physical_account_bank.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.UUID;

@Data
public class AccountBank {
    private UUID id;
    private Bank bank;
    private String refId;
    private String name;
    private AccountType type;
    private BigDecimal amount;
}

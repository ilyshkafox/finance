package ru.ilyshka.core.physical_account_bank.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class Bank {
    private UUID id;
    private String name;
}

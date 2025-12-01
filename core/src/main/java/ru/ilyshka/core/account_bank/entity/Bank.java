package ru.ilyshka.core.account_bank.entity;

import lombok.Data;

import java.util.UUID;

@Data
public class Bank {
    private UUID id;
    private String name;
}

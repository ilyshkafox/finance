CREATE SCHEMA IF NOT EXISTS physical_account;

CREATE TABLE IF NOT EXISTS physical_account.bank
(
    id   UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE IF NOT EXISTS physical_account.bank_account
(
    id      UUID PRIMARY KEY,
    bank_id UUID REFERENCES physical_account.bank (id),
    refId   VARCHAR(255)   NOT NULL,
    name    VARCHAR(255)   NOT NULL,
    type    VARCHAR(255)   NOT NULL,
    amount  DECIMAL(19, 4) NOT NULL
);


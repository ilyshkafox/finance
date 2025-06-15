package ru.ilyshka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum State {
    LOGIN_PHONE(false),
    LOGIN_CMC(false),
    LOGIN_CODE(false),
    PAGE_HOME(true),
    PAGE_HISTORY(true),
    PAGE_TRANSFERS(true),
    ;
    private boolean auth;
}

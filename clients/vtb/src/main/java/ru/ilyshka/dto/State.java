package ru.ilyshka.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum State {
    LOGIN_QR(false),
    PAGE_HOME(true),
    PAGE_HISTORY(true),
    PAGE_TRANSFERS(true),
    ;
    private boolean auth;
}
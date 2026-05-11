package ru.ilyshka.temporal.finance.vtb.model;

/**
 * Статус авторизации пользователя в VTB.
 */
public enum AuthStatus {
    /**
     * Авторизация не начата.
     */
    NOT_STARTED,

    /**
     * Процесс авторизации запущен.
     */
    PENDING,

    /**
     * Ожидается действие пользователя (переход по ссылке).
     */
    AWAITING_USER_ACTION,

    /**
     * Авторизация успешно завершена.
     */
    AUTHORIZED,

    /**
     * Ошибка авторизации.
     */
    FAILED,

    /**
     * Токен авторизации истёк.
     */
    EXPIRED
}
package ru.ilyshka.temporal.finance.vtb.exception;

import lombok.experimental.StandardException;

/**
 * Исключение, выбрасываемое когда требуется авторизация в VTB.
 * Это исключение НЕ должно retry'иться в Temporal.
 */
@StandardException
public class VTBAuthException extends RuntimeException {

}
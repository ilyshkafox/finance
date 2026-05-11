package ru.ilyshka.temporal.workflow;

import io.temporal.workflow.SignalMethod;

import java.lang.annotation.*;

/**
 * Расширенная аннотация для signal методов workflow.
 * Signals позволяют отправлять события в running workflow.
 */
@SignalMethod
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WorkflowSignal {

    /**
     * Имя signal для идентификации.
     */
    String value();

    /**
     * Описание signal.
     */
    String description() default "";

    /**
     * Требуется ли обработка сигнала в определённом порядке.
     */
    boolean ordered() default false;
}
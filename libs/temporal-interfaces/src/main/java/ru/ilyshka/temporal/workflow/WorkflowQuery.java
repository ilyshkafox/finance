package ru.ilyshka.temporal.workflow;

import io.temporal.workflow.QueryMethod;

import java.lang.annotation.*;

/**
 * Расширенная аннотация для query методов workflow.
 * Queries позволяют получать состояние running workflow.
 */
@QueryMethod
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WorkflowQuery {

    /**
     * Имя query для идентификации.
     */
    String value();

    /**
     * Описание query.
     */
    String description() default "";

    /**
     * Является ли query долгосрочным (должно быть детерминированным).
     */
    boolean longRunning() default false;
}
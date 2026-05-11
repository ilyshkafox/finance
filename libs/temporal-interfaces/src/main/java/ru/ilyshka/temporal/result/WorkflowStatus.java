package ru.ilyshka.temporal.result;

/**
 * Статусы выполнения workflow.
 */
public enum WorkflowStatus {

    /**
     * Workflow запущен и выполняется.
     */
    RUNNING,

    /**
     * Workflow успешно завершён.
     */
    COMPLETED,

    /**
     * Workflow завершился с ошибкой.
     */
    FAILED,

    /**
     * Workflow был отменён.
     */
    CANCELLED,

    /**
     * Workflow ожидает внешнего сигнала.
     */
    WAITING,

    /**
     * Workflow таймаутил.
     */
    TIMED_OUT,

    /**
     * Workflow продолжается в новом run.
     */
    CONTINUED_AS_NEW
}
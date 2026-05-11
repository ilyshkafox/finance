package ru.ilyshka.temporal.workflow;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.lang.annotation.*;

/**
 * Расширенная аннотация для воркфлоу интерфейсов.
 * Добавляет метаинформацию о воркфлоу: описание, тег-очередь по умолчанию.
 */
@WorkflowInterface
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BaseWorkflowInterface {

    /**
     * Имя воркфлоу для идентификации.
     */
    String value();

    /**
     * Описание воркфлоу.
     */
    String description() default "";

    /**
     * Task queue по умолчанию для этого воркфлоу.
     */
    String taskQueue() default "default";

    /**
     * Имя основного метода workflow (обычно process или execute).
     */
    String workflowMethod() default "process";
}
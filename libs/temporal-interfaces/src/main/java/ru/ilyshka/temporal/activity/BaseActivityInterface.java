package ru.ilyshka.temporal.activity;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

import java.lang.annotation.*;

/**
 * Расширенная аннотация для activity интерфейсов.
 * Добавляет метаинформацию об activity: описание, категорию.
 */
@ActivityInterface
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface BaseActivityInterface {

    /**
     * Имя activity interface для идентификации.
     */
    String value();

    /**
     * Описание activity.
     */
    String description() default "";

    /**
     * Категория activity (payment, notification, data, etc.).
     */
    String category() default "general";

    /**
     * Task queue для этого activity.
     */
    String taskQueue() default "default";
}
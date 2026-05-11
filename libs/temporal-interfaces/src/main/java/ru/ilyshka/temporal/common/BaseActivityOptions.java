package ru.ilyshka.temporal.common;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.retry.RetryOptions;
import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Базовые опции для Activity с настройками retry и таймаутов.
 * Используется как шаблон для создания Activity stub.
 */
@Builder
@Data
public class BaseActivityOptions {

    /**
     * Максимальное время выполнения activity от начала до закрытия.
     */
    @Builder.Default
    private Duration startToCloseTimeout = Duration.ofSeconds(30);

    /**
     * Максимальное время между задачами activity.
     */
    @Builder.Default
    private Duration taskTimeout = Duration.ofSeconds(10);

    /**
     * Максимальное время polling задачи.
     */
    @Builder.Default
    private Duration taskStartToCloseTimeout = Duration.ofSeconds(15);

    /**
     * Начальный интервал повторной попытки.
     */
    @Builder.Default
    private Duration retryInitialInterval = Duration.ofMillis(1000);

    /**
     * Максимальный интервал повторной попытки.
     */
    @Builder.Default
    private Duration retryMaximumInterval = Duration.ofSeconds(30);

    /**
     * Коэффициент увеличения интервала между попытками.
     */
    @Builder.Default
    private Double retryBackoffCoefficient = 2.0;

    /**
     * Максимальное количество попыток.
     */
    @Builder.Default
    private Integer retryMaximumAttempts = 3;

    /**
     * Создаёт ActivityOptions из этих параметров.
     *
     * @return сконфигурированные ActivityOptions
     */
    public ActivityOptions toActivityOptions() {
        RetryOptions retryOptions = RetryOptions.newBuilder()
                .setInitialInterval(retryInitialInterval)
                .setMaximumInterval(retryMaximumInterval)
                .setBackoffCoefficient(retryBackoffCoefficient)
                .setMaximumAttempts(retryMaximumAttempts)
                .build();

        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(startToCloseTimeout)
                .setTaskTimeout(taskTimeout)
                .setTaskStartToCloseTimeout(taskStartToCloseTimeout)
                .setRetryPolicy(retryOptions)
                .build();
    }

    /**
     * Создаёт ActivityOptions без retry (только таймауты).
     *
     * @return ActivityOptions без повторных попыток
     */
    public static ActivityOptions withoutRetry(Duration startToClose) {
        return ActivityOptions.newBuilder()
                .setStartToCloseTimeout(startToClose)
                .build();
    }
}
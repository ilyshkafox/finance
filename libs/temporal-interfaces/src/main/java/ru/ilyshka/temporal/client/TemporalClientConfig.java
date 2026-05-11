package ru.ilyshka.temporal.client;

import lombok.Builder;
import lombok.Data;

import java.time.Duration;

/**
 * Конфигурация Temporal Client для запуска workflow.
 * Клиент используется для запуска, остановки и опроса workflow.
 */
@Builder
@Data
public class TemporalClientConfig {

    /**
     * Адрес Temporal Server.
     */
    private String host;

    /**
     * Порт Temporal Server.
     */
    @Builder.Default
    private int port = 7233;

    /**
     * Namespace для изоляции.
     */
    @Builder.Default
    private String namespace = "default";

    /**
     * Task queue для клиента.
     */
    @Builder.Default
    private String taskQueue = "default";

    /**
     * Workflow Run Timeout - максимальное время жизни workflow.
     */
    @Builder.Default
    private Duration workflowRunTimeout = Duration.ofMinutes(30);

    /**
     * Workflow Task Timeout - максимальное время на обработку одной задачи.
     */
    @Builder.Default
    private Duration workflowTaskTimeout = Duration.ofSeconds(10);

    /**
     * Получить полный адрес Temporal Server.
     */
    public String getTargetAddress() {
        return host + ":" + port;
    }

    /**
     * Создаёт конфигурацию по умолчанию для development.
     */
    public static TemporalClientConfig defaultDevConfig() {
        return TemporalClientConfig.builder()
                .host("localhost")
                .port(7233)
                .namespace("default")
                .taskQueue("default")
                .build();
    }
}
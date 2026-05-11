package ru.ilyshka.temporal.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Конфигурация Temporal Worker.
 * Определяет параметры подключения к Temporal Server и настройки worker.
 */
@Builder
@Data
public class TemporalWorkerConfig {

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
     * Task queue по умолчанию.
     */
    @Builder.Default
    private String taskQueue = "default";

    /**
     * Максимальное количество worker потоков.
     */
    @Builder.Default
    private int maxWorkerActivityTasksPerSecond = 100;

    /**
     * Максимальное количество задач worker на секунду.
     */
    @Builder.Default
    private int maxWorkerTaskQueueTasksPerSecond = 100;

    /**
     * Режим identification worker.
     */
    @Builder.Default
    private int maxConcurrentActivityExecutionSize = 100;

    /**
     * Список workflow классов для регистрации.
     */
    private List<String> workflowClasses;

    /**
     * Список activity классов для регистрации.
     */
    private List<String> activityClasses;

    /**
     * Получить полный адрес Temporal Server.
     */
    public String getTargetAddress() {
        return host + ":" + port;
    }

    /**
     * Создаёт конфигурацию по умолчанию для development.
     */
    public static TemporalWorkerConfig defaultDevConfig() {
        return TemporalWorkerConfig.builder()
                .host("localhost")
                .port(7233)
                .namespace("default")
                .taskQueue("default")
                .build();
    }
}
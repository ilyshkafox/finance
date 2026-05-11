package ru.ilyshka.temporal.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Базовый результат workflow операции.
 * Содержит статус выполнения, идентификаторы и временные метки.
 *
 * @param <T> тип полезных данных результата
 */
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowResult<T> {

    /**
     * Идентификатор workflow.
     */
    private String workflowId;

    /**
     * Идентификатор run workflow.
     */
    private String runId;

    /**
     * Статус выполнения workflow.
     */
    private WorkflowStatus status;

    /**
     * Время начала выполнения.
     */
    private Instant startTime;

    /**
     * Время завершения (если workflow завершён).
     */
    private Instant endTime;

    /**
     * Сообщение о статусе или ошибке.
     */
    private String message;

    /**
     * Полезные данные результата.
     */
    private T data;

    /**
     * Флаг успешного выполнения.
     */
    public boolean isSuccess() {
        return status == WorkflowStatus.COMPLETED;
    }

    /**
     * Флаг наличия ошибки.
     */
    public boolean hasError() {
        return status == WorkflowStatus.FAILED || status == WorkflowStatus.CANCELLED;
    }

    /**
     * Создаёт успешный результат.
     */
    public static <T> WorkflowResult<T> success(String workflowId, String runId, T data) {
        return WorkflowResult.<T>builder()
                .workflowId(workflowId)
                .runId(runId)
                .status(WorkflowStatus.COMPLETED)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .message("Workflow completed successfully")
                .data(data)
                .build();
    }

    /**
     * Создаёт результат с ошибкой.
     */
    public static <T> WorkflowResult<T> error(String workflowId, String runId, String errorMessage) {
        return WorkflowResult.<T>builder()
                .workflowId(workflowId)
                .runId(runId)
                .status(WorkflowStatus.FAILED)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .message(errorMessage)
                .build();
    }

    /**
     * Создаёт отменённый результат.
     */
    public static <T> WorkflowResult<T> cancelled(String workflowId, String runId, String reason) {
        return WorkflowResult.<T>builder()
                .workflowId(workflowId)
                .runId(runId)
                .status(WorkflowStatus.CANCELLED)
                .startTime(Instant.now())
                .endTime(Instant.now())
                .message(reason)
                .build();
    }
}
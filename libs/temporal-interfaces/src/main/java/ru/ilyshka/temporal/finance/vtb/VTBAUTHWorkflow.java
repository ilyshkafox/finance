package ru.ilyshka.temporal.finance.vtb;

import ru.ilyshka.temporal.workflow.BaseWorkflowInterface;

/**
 * Workflow интерфейс для авторизации в VTB через polling.
 * Генерирует ссылку для авторизации, отправляет её через Telegram,
 * затем опрашивает статус авторизации до завершения.
 */
@BaseWorkflowInterface(
        value = "VTBAUTHWorkflow",
        description = "Workflow для авторизации в VTB через polling",
        taskQueue = "vtb-auth-tasks",
        workflowMethod = "startAuthorize"
)
public interface VTBAUTHWorkflow {

    /**
     * Основной метод запуска процесса авторизации.
     * Генерирует ссылку, отправляет в Telegram, начинает polling.
     */
    void authorize();
}
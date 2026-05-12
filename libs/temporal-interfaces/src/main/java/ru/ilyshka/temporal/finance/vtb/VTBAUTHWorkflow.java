package ru.ilyshka.temporal.finance.vtb;

import io.temporal.workflow.WorkflowInterface;

@WorkflowInterface
public interface VTBAUTHWorkflow {

    /**
     * Основной метод запуска процесса авторизации.
     * Генерирует ссылку, отправляет в Telegram, начинает polling.
     */
    void authorize();
}
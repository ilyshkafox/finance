package ru.ilyshka.temporal.finance.vtb;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface VTBAUTHWorkflow {

    /**
     * Основной метод запуска процесса авторизации.
     * Генерирует ссылку, отправляет в Telegram, начинает polling.
     */
    @WorkflowMethod
    void authorize();
}
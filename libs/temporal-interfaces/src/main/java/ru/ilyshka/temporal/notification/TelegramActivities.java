package ru.ilyshka.temporal.notification;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import ru.ilyshka.temporal.activity.BaseActivityInterface;

/**
 * Activity интерфейс для отправки уведомлений через Telegram.
 * Общий интерфейс, может использоваться разными сервисами (VTB, Payment и др.).
 */
@BaseActivityInterface(
        value = "TelegramActivities",
        description = "Activity для отправки уведомлений через Telegram",
        category = "notification",
        taskQueue = "notification-tasks"
)
@WorkflowInterface
public interface TelegramActivities {

    /**
     * Отправить текстовое сообщение пользователю.
     *
     * @param userId идентификатор пользователя (Telegram chat_id)
     * @param text   текст сообщения
     */
    @WorkflowMethod
    void sendMessage(String userId, String text);
    
}
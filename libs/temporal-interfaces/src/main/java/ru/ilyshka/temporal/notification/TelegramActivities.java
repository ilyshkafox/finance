package ru.ilyshka.temporal.notification;

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
public interface TelegramActivities {

    /**
     * Отправить текстовое сообщение пользователю.
     *
     * @param userId идентификатор пользователя (Telegram chat_id)
     * @param text   текст сообщения
     */
    void sendMessage(String userId, String text);

    /**
     * Отправить сообщение с разметкой.
     *
     * @param userId    идентификатор пользователя
     * @param text      текст сообщения
     * @param parseMode тип разметки (HTML, MarkdownV2)
     */
    void sendMessageWithFormat(String userId, String text, String parseMode);

    /**
     * Отправить сообщение с кнопкой-ссылкой.
     *
     * @param userId     идентификатор пользователя
     * @param text       текст сообщения
     * @param buttonText текст на кнопке
     * @param url        URL ссылки
     */
    void sendMessageWithButton(String userId, String text, String buttonText, String url);

    /**
     * Отправить сообщение для авторизации (с кнопкой перехода).
     *
     * @param userId  идентификатор пользователя
     * @param title   заголовок
     * @param authUrl ссылка для авторизации
     */
    void sendAuthMessage(String userId, String title, String authUrl);
}
package ru.ilyshka.finance.vtbstub;

import ru.ilyshka.temporal.activity.ActivityImplementation;
import ru.ilyshka.temporal.notification.TelegramActivities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Заглушка для Telegram Activities.
 * TODO: Заменить на реальную интеграцию с Telegram Bot API.
 */
@ActivityImplementation
public class TelegramStubActivitiesImpl implements TelegramActivities {

    private static final Logger log = LoggerFactory.getLogger(TelegramStubActivitiesImpl.class);

    @Override
    public void sendMessage(String userId, String text) {
        log.info("[STUB-TELEGRAM] Отправка сообщения пользователю {}: {}", userId, text);
    }

    @Override
    public void sendMessageWithFormat(String userId, String text, String parseMode) {
        log.info("[STUB-TELEGRAM] Отправка сообщения с форматом {} пользователю {}: {}",
                parseMode, userId, text);
    }

    @Override
    public void sendMessageWithButton(String userId, String text, String buttonText, String url) {
        log.info("[STUB-TELEGRAM] Отправка сообщения с кнопкой пользователю {}: {} (URL: {})",
                userId, text, url);
    }

    @Override
    public void sendAuthMessage(String userId, String title, String authUrl) {
        String message = String.format(
                "🔐 %s\n\n%s\n\nАвторизуйтесь, затем мы автоматически загрузим ваши данные.",
                title, authUrl
        );
        log.info("[STUB-TELEGRAM] Отправка сообщения для авторизации пользователю {}: {} (URL: {})",
                userId, title, authUrl);
        sendMessage(userId, message);
    }
}
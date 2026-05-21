package ru.ilyshka.temporal.stub;

import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ilyshka.temporal.notification.TelegramActivities;

@Slf4j
@Component
@ActivityImpl(taskQueues = "vtb")
@RequiredArgsConstructor
public class TelegramStubActivitiesImpl implements TelegramActivities {

    @Override
    public void sendMessage(String userId, String text) {
        log.info("[STUB-TELEGRAM] Отправка сообщения пользователю {}: {}", userId, text);
    }
}
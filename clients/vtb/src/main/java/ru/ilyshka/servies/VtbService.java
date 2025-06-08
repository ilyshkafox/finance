package ru.ilyshka.servies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ilyshka.libs.messages.FinanceEventService;
import ru.ilyshka.libs.messages.dto.HealthCheckMessage;

@Slf4j
@Component
@RequiredArgsConstructor
public class VtbService {
    private final VtbSeleniumClient client;
    private final FinanceEventService eventService;
    // Обновить heathcheck

    // Каждый час делать Обновление информации о счете.
    // Проверять какие были новые операции в течении дня
    // Сохранять в БД инфомрацию о просканированных данных


    @Scheduled(cron = "${app.check-cron}")
    void checkActivity() {
        log.info("Checking activity...");
        client.checkActivity();
        try {
            switch (client.getState()) {
                case PAGE_HOME, PAGE_HISTORY, PAGE_TRANSFERS -> eventService.pushHatchCheck(HealthCheckMessage.Status.OK, "OK");
                default -> eventService.pushHatchCheck(HealthCheckMessage.Status.WARNING, "Возможно не пройдена авторизация");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            eventService.pushHatchCheck(HealthCheckMessage.Status.ERROR, e.getMessage());
        }
    }

}


package ru.ilyshka.servies;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ilyshka.dto.Wallet;
import ru.ilyshka.libs.messages.FinanceEventService;
import ru.ilyshka.libs.messages.dto.HealthCheckMessage;

import java.time.LocalDate;
import java.util.List;

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
            if (client.getState().isAuth()) {
                eventService.pushHatchCheck(HealthCheckMessage.Status.OK, "OK");
            } else {
                eventService.pushHatchCheck(HealthCheckMessage.Status.WARNING, "Требуеться авторизация");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            eventService.pushHatchCheck(HealthCheckMessage.Status.ERROR, e.getMessage());
        }
    }

    @Scheduled(cron = "${app.action-cron}")
    public void startActions() {
        List<Wallet> wallets = client.getWallets();
        wallets.forEach(wallet -> log.info("{}", wallet));
        client.getHistory(LocalDate.now().minusDays(30), LocalDate.now());
    }


}


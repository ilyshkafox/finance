package ru.ilyshka.servies;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.ilyshka.libs.messages.FinanceEventService;
import ru.ilyshka.libs.messages.dto.HealthCheckMessage;

import java.time.YearMonth;

@Slf4j
@Component
@RequiredArgsConstructor
public class VtbService {
    private final VtbAuthService authService;
    private final VtbDataService dataService;
    private final FinanceEventService eventService;
    // Обновить heathcheck

    // Каждый час делать Обновление информации о счете.
    // Проверять какие были новые операции в течении дня
    // Сохранять в БД инфомрацию о просканированных данных


    @Scheduled(cron = "${app.check-cron}")
    void checkActivity() {
        log.info("Checking activity...");
        authService.checkActivity();
        try {
            if (authService.getState().isAuth()) {
                eventService.pushHatchCheck(HealthCheckMessage.Status.OK, "OK");
            } else {
                eventService.pushHatchCheck(HealthCheckMessage.Status.WARNING, "Требуеться авторизация");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            eventService.pushHatchCheck(HealthCheckMessage.Status.ERROR, e.getMessage());
        }
    }

    @SneakyThrows
    public void startActions() {
        log.info("Start Actions...");

        YearMonth startMonth = YearMonth.of(2023, 10);
        YearMonth currentMonth = YearMonth.now();

        for (YearMonth month = currentMonth; !month.isBefore(startMonth); month = month.minusMonths(1)) {
            log.info("Processing month: {}", month);
            dataService.getHistory(month);
            int randomNum = (int) (Math.random() * 1000);
            Thread.sleep(1000 + randomNum);
        }
    }


}
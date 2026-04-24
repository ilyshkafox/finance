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
    private final VtbDataService dataService;

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
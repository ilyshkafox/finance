package ru.ilyshka.temporal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.spring.boot.ActivityImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ilyshka.servies.VtbAuthService;
import ru.ilyshka.servies.VtbDataService;
import ru.ilyshka.temporal.finance.vtb.VTBActivities;
import ru.ilyshka.temporal.finance.vtb.exception.VTBAuthException;
import ru.ilyshka.temporal.finance.vtb.model.AuthStatus;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Реализация VTB Activities для Temporal workflow.
 * Интегрирует существующий VTB клиент (Selenium-based) с Temporal.
 */
@Service
@Slf4j
@ActivityImpl(taskQueues = "vtb")
@RequiredArgsConstructor
public class VTBActivitiesImpl implements VTBActivities {

    private final ObjectMapper objectMapper;
    private final VtbAuthService authService;
    private final VtbDataService dataService;
//    private final TelegramActivities telegramActivities;


    @Override
    public List<String> fetchTransactions(VTBFetchRequest request) {
        List<Map<String, Object>> operations = dataService.getHistoryRaw(request.getStartDate(), request.getEndDate());

        // Конвертируем в List<String>
        return operations.stream()
                .map(op -> {
                    try {
                        return objectMapper.writeValueAsString(op);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();

    }

    @Override
    public String startAuth() {
        return authService.startAuthorization();
    }

    @Override
    public void waitAuth() {
        while (true) {
            AuthStatus authStatus = getAuthStatus();
            if (Objects.requireNonNull(authStatus) == AuthStatus.AUTHORIZED) return;
            if (authStatus == AuthStatus.UNAUTHORIZED) throw new VTBAuthException("Авторизация не запущена!");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public AuthStatus getAuthStatus() {
        return authService.getAuthStatus();
    }
}
package ru.ilyshka.temporal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.temporal.spring.boot.WorkflowImpl;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ilyshka.servies.VtbAuthService;
import ru.ilyshka.servies.VtbDataService;
import ru.ilyshka.temporal.finance.vtb.VTBActivities;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;
import ru.ilyshka.temporal.notification.TelegramActivities;

import java.util.List;
import java.util.Map;

/**
 * Реализация VTB Activities для Temporal workflow.
 * Интегрирует существующий VTB клиент (Selenium-based) с Temporal.
 */
@Slf4j
@Component
@WorkflowImpl(taskQueues = "vtb-tx-tasks")
@RequiredArgsConstructor
public class VTBActivitiesImpl implements VTBActivities {

    private final ObjectMapper objectMapper;
    private final VtbAuthService authService;
    private final VtbDataService dataService;
    private final TelegramActivities telegramActivities;

    @Override
    @SneakyThrows
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
}
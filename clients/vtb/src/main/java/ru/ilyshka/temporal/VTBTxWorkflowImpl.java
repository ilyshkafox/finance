package ru.ilyshka.temporal;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import lombok.extern.slf4j.Slf4j;
import ru.ilyshka.temporal.finance.vtb.VTBActivities;
import ru.ilyshka.temporal.finance.vtb.VTBTxWorkflow;
import ru.ilyshka.temporal.finance.vtb.exception.VTBAuthException;
import ru.ilyshka.temporal.finance.vtb.model.AuthStatus;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

import java.time.Duration;
import java.util.List;

/**
 * Реализация VTBTxWorkflow.
 * Основной workflow для получения транзакций VTB с автоматической обработкой авторизации.
 * <p>
 * Workflow логика:
 * 1. Попытка получить транзакции
 * 2. Если ошибка авторизации → запускаем VTBAUTHWorkflow
 * 3. Повторная попытка получить транзакции
 * <p>
 * Повторные запросы транзакций происходят по интервалу (scheduled).
 */
@Slf4j
@WorkflowImpl(taskQueues = "vtb")
public class VTBTxWorkflowImpl implements VTBTxWorkflow {
    private final VTBActivities vtbActivities =
            Workflow.newActivityStub(
                    VTBActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(60))
                            .setTaskQueue("vtb")
                            .setRetryOptions(RetryOptions.newBuilder().setDoNotRetry(
                                    VTBAuthException.class.getName()
                            ).build())
                            .build()
            );


    @Override
    public List<String> fetchTransactions(VTBFetchRequest request) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        log.info("[VTBTxWorkflow] Начало получения транзакций, workflow: {}", workflowId);

        if (vtbActivities.getAuthStatus() != AuthStatus.AUTHORIZED) {
            log.info("[VTBTxWorkflow] Запуск процесса авторизации...");
            String urlToken = vtbActivities.startAuth();
            log.info("[VTBTxWorkflow] Получена ссылка для авторизации: {}", urlToken);
            vtbActivities.waitAuth();
        }

        try {
            log.info("[VTBTxWorkflow] Попытка получить транзакции...");
            return vtbActivities.fetchTransactions(request);
        } catch (Exception e) {
            log.error("[VTBTxWorkflow] Ошибка при получении транзакций: {}", e.getMessage(), e);
            throw e;
        }
    }
}

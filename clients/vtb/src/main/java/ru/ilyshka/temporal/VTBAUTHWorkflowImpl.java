package ru.ilyshka.temporal;

import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.ilyshka.temporal.finance.vtb.VTBAUTHWorkflow;
import ru.ilyshka.temporal.finance.vtb.VTBActivities;
import ru.ilyshka.temporal.finance.vtb.VTBTxWorkflow;
import ru.ilyshka.temporal.finance.vtb.exception.VTBAuthException;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

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
@Service
@Slf4j
@WorkflowImpl(taskQueues = "vtb")
@RequiredArgsConstructor
public class VTBAUTHWorkflowImpl implements VTBAUTHWorkflow {
    @Override
    public void authorize() {
        log.info("[VTBAUTHWorkflow] Авторизация...");
    }
}

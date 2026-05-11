package ru.ilyshka.temporal.finance.vtb;

import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;
import ru.ilyshka.temporal.workflow.BaseWorkflowInterface;
import ru.ilyshka.temporal.workflow.WorkflowSignal;

import java.util.List;

/**
 * Workflow интерфейс для получения транзакций VTB с обработкой авторизации.
 * При ошибке авторизации автоматически запускает процесс авторизации.
 */
@BaseWorkflowInterface(
        value = "VTBTxWorkflow",
        description = "Workflow для получения транзакций VTB с обработкой авторизации",
        taskQueue = "vtb-tx-tasks",
        workflowMethod = "fetchTransactions"
)
public interface VTBTxWorkflow {

    /**
     * Основной метод получения транзакций.
     * При ошибке авторизации запускает процесс авторизации.
     *
     * @param request запрос на получение транзакций
     */
    List<String> fetchTransactions(VTBFetchRequest request);

}
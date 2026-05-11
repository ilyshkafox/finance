package ru.ilyshka.temporal.finance.vtb;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

import java.util.List;


@WorkflowInterface()
public interface VTBTxWorkflow {

    /**
     * Основной метод получения транзакций.
     * При ошибке авторизации запускает процесс авторизации.
     *
     * @param request запрос на получение транзакций
     */
    @WorkflowMethod
    List<String> fetchTransactions(VTBFetchRequest request);

}
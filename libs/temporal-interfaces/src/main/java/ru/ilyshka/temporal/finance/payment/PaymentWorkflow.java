package ru.ilyshka.temporal.finance.payment;

import ru.ilyshka.temporal.result.WorkflowResult;
import ru.ilyshka.temporal.workflow.BaseWorkflowInterface;
import ru.ilyshka.temporal.workflow.WorkflowQuery;
import ru.ilyshka.temporal.workflow.WorkflowSignal;

/**
 * Workflow интерфейс для обработки финансовых платежей.
 * Определяет основной процесс оплаты с проверками и уведомлениями.
 */
@BaseWorkflowInterface(
        value = "PaymentWorkflow",
        description = "Workflow для обработки финансовых платежей",
        taskQueue = "finance-payment-tasks",
        workflowMethod = "processPayment"
)
public interface PaymentWorkflow {

    /**
     * Основной метод обработки платежа.
     *
     * @param paymentId идентификатор платежа
     * @param amount    сумма
     * @param currency  валюта
     */
    void processPayment(String paymentId, double amount, String currency);

    /**
     * Метод для отмены платежа (signal).
     */
    @WorkflowSignal(value = "cancel-payment", description = "Сигнал для отмены платежа")
    void cancelPayment(String paymentId, String reason);

    /**
     * Метод для запроса статуса платежа (query).
     */
    @WorkflowQuery(value = "get-payment-status", description = "Запрос статуса платежа")
    String getPaymentStatus(String paymentId);

    /**
     * Метод для запроса текущей стадии платежа (query).
     */
    @WorkflowQuery(value = "get-payment-stage", description = "Запрос текущей стадии платежа")
    String getPaymentStage(String paymentId);
}
package ru.ilyshka.temporal.finance.payment;

import ru.ilyshka.temporal.workflow.BaseWorkflowInterface;

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
    void cancelPayment(String paymentId, String reason);

    /**
     * Метод для запроса статуса платежа (query).
     */
    String getPaymentStatus(String paymentId);

    /**
     * Метод для запроса текущей стадии платежа (query).
     */
    String getPaymentStage(String paymentId);
}
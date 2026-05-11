package ru.ilyshka.temporal.finance.payment;

import ru.ilyshka.temporal.activity.BaseActivityInterface;
import ru.ilyshka.temporal.common.BaseActivityOptions;

/**
 * Activity интерфейс для финансовых операций.
 * Определяет контракты для платежей, проверок мошенничества и уведомлений.
 */
@BaseActivityInterface(
        value = "PaymentActivities",
        description = "Activities для обработки финансовых платежей",
        category = "payment",
        taskQueue = "finance-payment-tasks"
)
public interface PaymentActivities {

    /**
     * Проверка на мошенничество для платежа.
     *
     * @param paymentId идентификатор платежа
     * @return true если проверка пройдена
     */
    boolean checkFraud(String paymentId);

    /**
     * Проверка доступности средств.
     *
     * @param paymentId идентификатор платежа
     * @param amount    сумма
     * @return true если средств достаточно
     */
    boolean checkFunds(String paymentId, double amount);

    /**
     * Обработка платежа через платёжный шлюз.
     *
     * @param paymentId идентификатор платежа
     * @param amount    сумма
     * @param currency  валюта
     */
    void processPayment(String paymentId, double amount, String currency);

    /**
     * Создание транзакции в системе.
     *
     * @param paymentId идентификатор платежа
     * @param amount    сумма
     * @return идентификатор транзакции
     */
    String createTransaction(String paymentId, double amount);

    /**
     * Отправка уведомления о платеже.
     *
     * @param paymentId идентификатор платежа
     * @param message   текст уведомления
     * @param type      тип уведомления (email, sms, push)
     */
    void sendNotification(String paymentId, String message, String type);

    /**
     * Отмена платежа.
     *
     * @param paymentId идентификатор платежа
     * @param reason    причина отмены
     */
    void cancelPayment(String paymentId, String reason);

    /**
     * Создаёт ActivityOptions для payment операций по умолчанию.
     */
    static BaseActivityOptions defaultPaymentOptions() {
        return BaseActivityOptions.builder()
                .startToCloseTimeout(java.time.Duration.ofSeconds(60))
                .taskTimeout(java.time.Duration.ofSeconds(15))
                .retryInitialInterval(java.time.Duration.ofMillis(1000))
                .retryMaximumInterval(java.time.Duration.ofSeconds(30))
                .retryBackoffCoefficient(2.0)
                .retryMaximumAttempts(5)
                .build();
    }

    /**
     * Создаёт ActivityOptions для fast операций (без внешних вызовов).
     */
    static BaseActivityOptions fastOptions() {
        return BaseActivityOptions.builder()
                .startToCloseTimeout(java.time.Duration.ofSeconds(10))
                .taskTimeout(java.time.Duration.ofSeconds(5))
                .retryMaximumAttempts(2)
                .build();
    }
}
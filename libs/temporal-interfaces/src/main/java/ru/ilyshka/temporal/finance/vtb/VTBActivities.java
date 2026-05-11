package ru.ilyshka.temporal.finance.vtb;

import ru.ilyshka.temporal.activity.BaseActivityInterface;
import ru.ilyshka.temporal.common.BaseActivityOptions;
import ru.ilyshka.temporal.finance.vtb.model.AuthStatus;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

import java.time.Duration;
import java.util.List;

/**
 * Activity интерфейс для работы с VTB API.
 * Определяет контракты для получения транзакций и управления авторизацией.
 */
@BaseActivityInterface(
        value = "VTBActivities",
        description = "Activities для работы с VTB API",
        category = "vtb",
        taskQueue = "vtb-tx-tasks"
)
public interface VTBActivities {

    /**
     * Получить транзакции пользователя за указанный период.
     *
     * @param request запрос на получение транзакций
     * @return список транзакций
     * @throws ru.ilyshka.temporal.finance.vtb.exception.VTBAuthException если требуется авторизация
     */
    List<String> fetchTransactions(VTBFetchRequest request);



    /**
     * Создаёт ActivityOptions для VTB операций по умолчанию.
     */
    static BaseActivityOptions defaultVtbOptions() {
        return BaseActivityOptions.builder()
                .startToCloseTimeout(Duration.ofSeconds(30))
                .taskTimeout(Duration.ofSeconds(10))
                .retryInitialInterval(Duration.ofMillis(1000))
                .retryMaximumInterval(Duration.ofSeconds(30))
                .retryBackoffCoefficient(2.0)
                .retryMaximumAttempts(3)
                .build();
    }

    /**
     * Создаёт ActivityOptions для операций авторизации.
     */
    static BaseActivityOptions authOptions() {
        return BaseActivityOptions.builder()
                .startToCloseTimeout(Duration.ofSeconds(60))
                .taskTimeout(Duration.ofSeconds(15))
                .retryInitialInterval(Duration.ofMillis(2000))
                .retryMaximumInterval(Duration.ofSeconds(60))
                .retryBackoffCoefficient(2.0)
                .retryMaximumAttempts(5)
                .build();
    }
}
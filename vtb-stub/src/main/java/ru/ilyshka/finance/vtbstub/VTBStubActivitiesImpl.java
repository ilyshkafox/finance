package ru.ilyshka.finance.vtbstub;

import ru.ilyshka.temporal.activity.ActivityImplementation;
import ru.ilyshka.temporal.finance.vtb.VTBActivities;
import ru.ilyshka.temporal.finance.vtb.exception.VTBAuthException;
import ru.ilyshka.temporal.finance.vtb.model.AuthStatus;
import ru.ilyshka.temporal.finance.vtb.model.Transaction;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Заглушка для VTB Activities.
 * Эмулирует API VTB с возможностью симуляции состояния авторизации.
 * TODO: Заменить на реальную интеграцию с VTB API.
 */
@ActivityImplementation
public class VTBStubActivitiesImpl implements VTBActivities {

    private static final Logger log = LoggerFactory.getLogger(VTBStubActivitiesImpl.class);

    // Хранение состояния авторизации пользователей (для демо)
    private final Map<String, AuthStatus> authStates = new ConcurrentHashMap<>();
    private final Map<String, String> authLinks = new ConcurrentHashMap<>();

    @Override
    public List<Transaction> fetchTransactions(VTBFetchRequest request) {
        String userId = request.getUserId();
        AuthStatus status = authStates.getOrDefault(userId, AuthStatus.NOT_STARTED);

        // Если авторизация не выполнена — выбрасываем исключение
        if (status == AuthStatus.NOT_STARTED || status == AuthStatus.EXPIRED) {
            log.warn("[STUB-VTB] Пользователь {} не авторизован", userId);
            throw new VTBAuthException("VTB API: Требуется авторизация для пользователя " + userId);
        }

        // Если авторизация в процессе — ждём
        if (status == AuthStatus.PENDING || status == AuthStatus.AWAITING_USER_ACTION) {
            log.warn("[STUB-VTB] Пользователь {} авторизация в процессе: {}", userId, status);
            throw new VTBAuthException("VTB API: Авторизация в процессе для пользователя " + userId);
        }

        log.info("[STUB-VTB] Получение транзакций для пользователя {}", userId);
        return generateDummyTransactions(request);
    }

    @Override
    public void saveTransactions(List<Transaction> transactions) {
        log.info("[STUB-VTB] Сохранение {} транзакций", transactions.size());
        // TODO: Реальная сохранение в БД
    }

    @Override
    public List<Transaction> fetchTransactionsAfterAuth(String userId, VTBFetchRequest request) {
        log.info("[STUB-VTB] Получение транзакций после авторизации для пользователя {}", userId);
        return generateDummyTransactions(request);
    }

    @Override
    public String generateAuthLink(String userId) {
        String link = "https://vtb.ru/auth?session=" + UUID.randomUUID().toString();
        authLinks.put(userId, link);
        authStates.put(userId, AuthStatus.AWAITING_USER_ACTION);
        log.info("[STUB-VTB] Сгенерирована ссылка для пользователя {}: {}", userId, link);
        return link;
    }

    @Override
    public AuthStatus checkAuthStatus(String userId) {
        AuthStatus status = authStates.get(userId);
        log.info("[STUB-VTB] Проверка статуса авторизации для {}: {}", userId, status);
        return status != null ? status : AuthStatus.NOT_STARTED;
    }

    @Override
    public void refreshTokenAfterAuth(String userId) {
        authStates.put(userId, AuthStatus.AUTHORIZED);
        log.info("[STUB-VTB] Токен обновлён для пользователя {}", userId);
    }

    /**
     * Симуляция завершения авторизации (вызывается после действия пользователя).
     * Для демо можно вызывать вручную или по сигналу.
     */
    public void completeAuth(String userId) {
        authStates.put(userId, AuthStatus.AUTHORIZED);
        log.info("[STUB-VTB] Авторизация завершена для пользователя {}", userId);
    }

    /**
     * Симуляция истечения токена.
     */
    public void expireAuth(String userId) {
        authStates.put(userId, AuthStatus.EXPIRED);
        log.info("[STUB-VTB] Токен истёк для пользователя {}", userId);
    }

    private List<Transaction> generateDummyTransactions(VTBFetchRequest request) {
        LocalDate date = request.getStartDate();
        return List.of(
                createTransaction("1", request.getAccountId(), date.plusDays(1),
                        "Покупка в магазине", new BigDecimal("-1500.00"), "RUB", "Покупки"),
                createTransaction("2", request.getAccountId(), date.plusDays(2),
                        "Зачисление зарплаты", new BigDecimal("50000.00"), "RUB", "Доход"),
                createTransaction("3", request.getAccountId(), date.plusDays(3),
                        "Перевод", new BigDecimal("-5000.00"), "RUB", "Переводы")
        );
    }

    private Transaction createTransaction(String id, String accountId, LocalDate date,
                                           String description, BigDecimal amount,
                                           String currency, String category) {
        Transaction t = new Transaction();
        t.setId(id);
        t.setAccountId(accountId);
        t.setTransactionDate(date);
        t.setDescription(description);
        t.setAmount(amount);
        t.setCurrency(currency);
        t.setCategory(category);
        t.setCreatedAt(Instant.now());
        return t;
    }
}
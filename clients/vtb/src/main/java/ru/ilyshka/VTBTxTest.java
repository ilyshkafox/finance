package ru.ilyshka;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import lombok.extern.slf4j.Slf4j;
import ru.ilyshka.temporal.finance.vtb.VTBTxWorkflow;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Тестовый класс для демонстрации работы VTBTxWorkflow.
 * Подключается к существующему Temporal Server и запускает workflow.
 * <p>
 * Для запуска необходимо:
 * 1. Temporal Server запущен на 192.168.2.100:30376
 * 2. Spring Boot приложение запущено (регистрация workflow и activities)
 * 3. Запустить этот main-класс
 */
@Slf4j
public class VTBTxTest {

    // Адрес Temporal Server (через port-forward Windows: localhost:30376 -> 192.168.2.100:30376 -> 7233)
    // Для настройки port-forward (от имени администратора):
    // netsh interface portproxy add v4tov4 listenport=30376 connectaddress=192.168.2.100 connectport=30376
    private static final String TEMPORAL_HOST = "192.168.2.100";
    private static final int TEMPORAL_PORT = 30376;

    public static void main(String[] args) {
        log.info("Подключение к Temporal Server: {}:{}", TEMPORAL_HOST, TEMPORAL_PORT);

        WorkflowServiceStubs service = null;
        try {
            // Создаем options без health check timeout (deprecated)
            WorkflowServiceStubsOptions stubsOptions = WorkflowServiceStubsOptions.newBuilder()
                    .setTarget(TEMPORAL_HOST + ":" + TEMPORAL_PORT)
                    .setEnableHttps(false)

                    .build();

            // Используем newServiceStubs для lazy connection (без immediate health check)
            // newConnectedServiceStubs() пытается сразу подключиться и упадет если сервер недоступен
            service = WorkflowServiceStubs.newServiceStubs(stubsOptions);

            // 2. Создаем WorkflowClient (используем сервис напрямую)
            WorkflowClient workflowClient = WorkflowClient.newInstance(service);

            // 3. Настраиваем WorkflowOptions
            WorkflowOptions workflowOptions = WorkflowOptions.newBuilder()
                    .setTaskQueue("vtb-tx-tasks")
                    .setWorkflowRunTimeout(Duration.ofMinutes(5))
                    .setWorkflowTaskTimeout(Duration.ofSeconds(10))
                    .build();

            // 4. Получаем workflow stub
            VTBTxWorkflow txWorkflow = workflowClient.newWorkflowStub(VTBTxWorkflow.class, workflowOptions);

            // 5. Создаем запрос на получение транзакций
            VTBFetchRequest request = new VTBFetchRequest();
            request.setStartDate(LocalDate.of(2026, 4, 1));
            request.setEndDate(LocalDate.of(2026, 5, 1));

            log.info("Запуск workflow для периода: {} - {}",
                    request.getStartDate(), request.getEndDate());

            // 6. Запускаем workflow (async)
            List<String> strings = txWorkflow.fetchTransactions(request);
            for (String string : strings) {
                log.info(string);
            }

            log.info("Тест завершен успешно!");

        } catch (Exception e) {
            log.error("Ошибка при запуске теста: {}", e.getMessage(), e);
            System.exit(1);
        }
        // WorkflowServiceStubs не требует явного закрытия — gRPC channel управляется internally
    }
}

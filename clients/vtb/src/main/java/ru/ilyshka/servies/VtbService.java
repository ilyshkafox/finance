package ru.ilyshka.servies;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ilyshka.temporal.finance.vtb.VTBTxWorkflow;

@Slf4j
@Component
@RequiredArgsConstructor
public class VtbService {
    private final WorkflowClient client;


//    private final VtbDataService dataService;

    @SneakyThrows
    public void sync() {

        // Получить последнюю дату (Статус processed, последняя запись или дата начала)
        // Интервал месяц.
        // Обновлять данные о чеках если есть обновление. (статус)
        //


//
        VTBTxWorkflow workflow =
                client.newWorkflowStub(
                        VTBTxWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue("vtb")
                                .build());


        WorkflowClient.start(workflow::sync, true);


    }


}
package ru.ilyshka.servies;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.ilyshka.controllers.dto.GetReceipt;
import ru.ilyshka.temporal.finance.vtb.VTBTxWorkflow;
import ru.ilyshka.temporal.finance.vtb.model.VTBFetchRequest;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class VtbService {
    private final WorkflowClient client;


//    private final VtbDataService dataService;

    @SneakyThrows
    public void startActions(GetReceipt getReceipt) {
        VTBTxWorkflow workflow =
                client.newWorkflowStub(
                        VTBTxWorkflow.class,
                        WorkflowOptions.newBuilder().setTaskQueue("vtb").build());


        new Thread(() -> {
            List<String> strings = workflow.fetchTransactions(VTBFetchRequest.builder()
                    .startDate(getReceipt.from())
                    .endDate(getReceipt.to())
                    .build());

            for (String string : strings) {
                log.info(string);
            }
        }).start();


//        YearMonth startMonth = YearMonth.of(2023, 10);

//        log.info("Processing month: {}", month);
//        dataService.getHistory(month);
//        int randomNum = (int) (Math.random() * 1000);
//        Thread.sleep(1000 + randomNum);

    }


}
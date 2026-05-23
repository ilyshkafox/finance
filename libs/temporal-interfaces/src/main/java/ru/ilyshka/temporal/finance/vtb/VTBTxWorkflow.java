package ru.ilyshka.temporal.finance.vtb;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;


@WorkflowInterface()
public interface VTBTxWorkflow {


    @WorkflowMethod
    void sync(boolean approved);

    @SignalMethod
    void userApproved();

}
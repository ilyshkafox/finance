---
name: Temporal
description: 'Generate Temporal workflow code in Java. Activate when user mentions: workflow, activity, worker, SAGA pattern, scheduled task, cron, retry policy, timeout, timer, durable execution, state machine, payment processing, order fulfillment, or any long-running multi-step process that needs fault tolerance. Use when building resilient background jobs, distributed transactions, or orchestrated business processes with Temporal.io.'
---

# Temporal Skill

Generate production-ready Temporal Java workflow code with proper deterministic patterns, activity management, retry
policies, and Spring Boot integration.

## When to Use

- Creating new workflow definitions and implementations
- Adding activities for external service integration
- Setting up Temporal workers and clients
- Implementing distributed transaction patterns (SAGA)
- Creating scheduled/cron-like background jobs
- Building state machines with durable execution
- Adding human-in-the-loop workflows

## Temporal Concepts Quick Reference

| Concept        | Description                                                                                             |
|----------------|---------------------------------------------------------------------------------------------------------|
| **Workflow**   | Long-running, deterministic orchestrator. Must be deterministic — no direct calls to external services. |
| **Activity**   | Non-deterministic unit of work (API calls, DB writes). Called from workflows via proxies.               |
| **Worker**     | Process that polls task queues and executes workflow/activity code.                                     |
| **Task Queue** | Named queue connecting clients, workers, and Temporal server.                                           |
| **Namespace**  | Isolation boundary for workflows (default: `default`).                                                  |

## Version Reference

- **Temporal SDK**: `1.32.1` (or latest stable)
- **Java**: Minimum 17
- **Spring Boot**: `3.2.x` (if using Spring)
- **Temporal Server**: `1.26+` (dev server: `temporalio/temporal:latest`)

---

## Steps

### 1. Add Dependencies

Add to `pom.xml`:

```xml

<properties>
    <temporal.version>1.32.1</temporal.version>
</properties>

<dependencies>
<dependency>
    <groupId>io.temporal</groupId>
    <artifactId>temporal-sdk</artifactId>
    <version>${temporal.version}</version>
</dependency>
<dependency>
    <groupId>io.temporal</groupId>
    <artifactId>temporal-spring-boot-autoconfigure</artifactId>
    <version>${temporal.version}</version>
</dependency>
<dependency>
    <groupId>io.temporal</groupId>
    <artifactId>temporal-testing</artifactId>
    <version>${temporal.version}</version>
    <scope>test</scope>
</dependency>
</dependencies>
```

### 2. Create Activity Interface

Activities define the contract for units of work. Always use `@ActivityInterface`.

```java
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface PaymentActivities {

    @ActivityMethod
    void processPayment(String paymentId, double amount);

    @ActivityMethod
    boolean checkFraud(String paymentId);

    @ActivityMethod
    String sendNotification(String paymentId, String message);
}
```

### 3. Create Activity Implementation

Implement activities as Spring `@Singleton` beans. Handle exceptions properly.

```java
import io.temporal.activity.Activity;
import io.temporal.failure.ActivityTimeoutFailure;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PaymentActivitiesImpl implements PaymentActivities {

    @Override
    public void processPayment(String paymentId, double amount) {
        // External API call — wrap in try/catch for proper error handling
        try {
            // TODO: Call payment gateway
            System.out.println("Processing payment: " + paymentId + " amount: " + amount);
        } catch (Exception e) {
            throw new RuntimeException("Payment processing failed for " + paymentId, e);
        }
    }

    @Override
    public boolean checkFraud(String paymentId) {
        // Fraud check logic
        return true;
    }
}
```

### 4. Create Workflow Interface

Workflows define the orchestrator contract. Use `@WorkflowInterface`.

```java
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PaymentWorkflow {

    @WorkflowMethod
    void processPayment(String paymentId, double amount);
}
```

### 5. Create Workflow Implementation

**CRITICAL**: Workflow code MUST be deterministic. Never:

- Call external services directly
- Use `Math.random()` or `System.currentTimeMillis()`
- Use non-deterministic collections (HashMap iteration order)
- Make network calls

Use Activities for all non-deterministic operations.

```java
import io.temporal.workflow.Workflow;
import io.temporal.workflow.ActivityOptions;
import io.temporal.common.retry.RetryOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class PaymentWorkflowImpl implements PaymentWorkflow {

    @Override
    public void processPayment(String paymentId, double amount) {
        // Create activity proxy with timeout and retry settings
        PaymentActivities activities = Workflow.newActivityStub(
                PaymentActivities.class,
                ActivityOptions.newBuilder()
                        .setStartToCloseTimeout(Duration.ofSeconds(30))
                        .setRetryPolicy(RetryOptions.newBuilder()
                                .setInitialInterval(Duration.ofMillis(1000))
                                .setMaximumInterval(Duration.ofSeconds(30))
                                .setMaximumAttempts(3)
                                .build())
                        .build()
        );

        // Workflow logic — deterministic, orchestrated by Temporal
        // Fraud check with retry
        boolean fraudPassed = activities.checkFraud(paymentId);
        if (!fraudPassed) {
            throw new RuntimeException("Fraud check failed for payment: " + paymentId);
        }

        // Process payment
        activities.processPayment(paymentId, amount);

        // Send notification
        activities.sendNotification(paymentId, "Payment processed successfully");
    }
}
```

### 6. Create Worker Configuration

Workers poll task queues and execute workflow/activity code.

```java
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.client.Client;
import io.temporal.client.ClientOptions;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TemporalWorker {

    @Value("${temporal.host:localhost}")
    private String temporalHost;

    @Value("${temporal.port:7233}")
    private int temporalPort;

    @Value("${temporal.namespace:default}")
    private String namespace;

    @Value("${temporal.task-queue:default}")
    private String taskQueue;

    private WorkerFactory factory;
    private Worker worker;

    @PostConstruct
    public void startWorker() {
        Client client = Client.newClient(
                ClientOptions.newBuilder()
                        .setTargetAddress(temporalHost + ":" + temporalPort)
                        .setNamespace(namespace)
                        .build()
        );

        factory = WorkerFactory.newInstance(client);
        worker = factory.newWorker(taskQueue);

        // Register workflows and activities
        worker.registerWorkflowImplementationTypes(PaymentWorkflowImpl.class);
        worker.registerActivitiesImplementationTypes(PaymentActivitiesImpl.class);

        factory.start();
        System.out.println("Temporal worker started on task queue: " + taskQueue);
    }

    @PreDestroy
    public void shutdown() {
        if (factory != null) {
            factory.shutdown();
        }
    }
}
```

### 7. Create Client for Starting Workflows

```java
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
public class PaymentClient {

    private final WorkflowClient workflowClient;
    private final String taskQueue;

    public PaymentClient(
            @org.springframework.beans.factory.annotation.Value("${temporal.host:localhost}") String host,
            @org.springframework.beans.factory.annotation.Value("${temporal.port:7233}") int port,
            @org.springframework.beans.factory.annotation.Value("${temporal.namespace:default}") String ns,
            @org.springframework.beans.factory.annotation.Value("${temporal.task-queue:default}") String tq
    ) {
        this.workflowClient = WorkflowClient.newInstance(
                WorkflowClient.Options.newBuilder()
                        .setTargetAddress(host + ":" + port)
                        .setNamespace(ns)
                        .build()
        );
        this.taskQueue = tq;
    }

    /**
     * Start workflow synchronously (waits for completion)
     */
    public void startPaymentSync(String paymentId, double amount) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId("payment-" + paymentId)
                .setWorkflowRunTimeout(Duration.ofMinutes(5))
                .setWorkflowTaskTimeout(Duration.ofSeconds(10))
                .build();

        PaymentWorkflow workflow = workflowClient.newWorkflowStub(PaymentWorkflow.class, options);
        workflow.processPayment(paymentId, amount);
    }

    /**
     * Start workflow asynchronously (fire and forget)
     */
    public String startPaymentAsync(String paymentId, double amount) {
        String workflowId = "payment-" + paymentId + "-" + UUID.randomUUID().toString().substring(0, 8);

        WorkflowOptions options = WorkflowOptions.newBuilder()
                .setTaskQueue(taskQueue)
                .setWorkflowId(workflowId)
                .setWorkflowRunTimeout(Duration.ofMinutes(10))
                .build();

        PaymentWorkflow workflow = workflowClient.newWorkflowStub(PaymentWorkflow.class, options);
        workflow.processPayment(paymentId, amount);

        return workflowId;
    }

    /**
     * Query workflow status
     */
    public String getWorkflowStatus(String workflowId) {
        WorkflowStub stub = workflowClient.newUntypedWorkflowStub(PaymentWorkflow.class,
                WorkflowOptions.newBuilder().setWorkflowId(workflowId).build()
        );
        return stub.describe().getStatus().toString();
    }
}
```

### 8. Add Configuration (application.yml)

```yaml
temporal:
  host: localhost
  port: 7233
  namespace: default
  task-queue: finance-tasks

management:
  endpoints:
    web:
      exposure:
        include: health,info
```

---

## Common Patterns

### Retry with Exponential Backoff

```java
ActivityOptions options = ActivityOptions.newBuilder()
        .setStartToCloseTimeout(Duration.ofSeconds(30))
        .setRetryPolicy(RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofMillis(500))
                .setMaximumInterval(Duration.ofSeconds(60))
                .setBackoffCoefficient(2.0)
                .setMaximumAttempts(5)
                .build())
        .build();
```

### Timers (Wait for External Events)

```java
// Wait 30 days for user upgrade
Workflow.sleep(Duration.ofDays(30));

// Or with deadline
        Workflow.

await(Duration.ofHours(1), ()->

someConditionMet());
```

### Schedules (Cron-like Jobs)

```java
import io.temporal.client.ScheduleClient;
import io.temporal.client.ScheduleOptions;
import io.temporal.client.WorkflowOptions;

ScheduleClient scheduleClient = ScheduleClient.newClient(client);
ScheduleOptions scheduleOptions = ScheduleOptions.newBuilder()
        .setSpec(ScheduleSpec.newBuilder()
                .setCalendarList(
                        ScheduleCalendarSpec.newBuilder()
                                .setHour(20)
                                .setMinute(30)
                                .setDayOfWeek("WEDNESDAY")
                                .build()
                )
                .build())
        .build();

scheduleClient.

create("cleanup-schedule",scheduleOptions, ...);
```

### SAGA Pattern (Distributed Transactions)

```java
// Forward actions
activities.createOrder(orderId);
activities.

reserveInventory(orderId);
activities.

processPayment(orderId);

// Compensation (on failure)
// Use Workflow.onCancel() or try/catch with compensating activities
```

### Human in the Loop

```java
// Wait for external signal
String decision = Workflow.awaitSignal(
                SignalClient.newSignalClient(client),
                "approval-signal",
                Duration.ofDays(7)
        );
```

---

## Testing

### Unit Test with Temporal Testing

```java
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PaymentWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private PaymentWorkflow workflow;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();

        // Register implementations
        testEnv.registerWorkflowImplementationTypes(PaymentWorkflowImpl.class);
        testEnv.registerActivitiesImplementationTypes(new PaymentActivitiesImpl());

        // Create workflow stub
        workflow = testEnv.newWorkflowStub(PaymentWorkflow.class);
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    @Test
    void testSuccessfulPayment() {
        workflow.processPayment("test-123", 100.0);
        // Assertions — workflow completed without exception
    }

    @Test
    void testFraudDetection() {
        // Modify activity to return fraud = true
        assertThrows(RuntimeException.class, () -> {
            workflow.processPayment("test-456", 500.0);
        });
    }
}
```

---

## Best Practices

1. **Workflows must be deterministic** — No direct external calls, random values, or timestamps
2. **Use Activities for side effects** — All I/O, API calls, and DB operations go in activities
3. **Set appropriate timeouts** — Workflow run timeout > activity timeout
4. **Use retry policies** — Configure retry for transient failures
5. **Use unique workflow IDs** — Prevent duplicate executions
6. **Handle cancellation gracefully** — Implement `Workflow.onCancel()`
7. **Keep workflows focused** — One workflow per business process
8. **Log with workflow context** — Include workflowId in logs for tracing
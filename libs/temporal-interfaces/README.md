# Temporal Interfaces Library

Библиотека базовых интерфейсов и аннотаций для работы с Temporal Workflow в проекте Finance.

## Структура модуля

```
temporal-interfaces/
├── src/main/java/ru/ilyshka/temporal/
│   ├── common/              # Общие интерфейсы
│   │   ├── WorkflowIdGenerator.java
│   │   └── BaseActivityOptions.java
│   ├── workflow/            # Workflow аннотации
│   │   ├── BaseWorkflowInterface.java
│   │   ├── WorkflowSignal.java
│   │   └── WorkflowQuery.java
│   ├── activity/            # Activity аннотации
│   │   └── BaseActivityInterface.java
│   ├── result/              # Результаты workflow
│   │   ├── WorkflowResult.java
│   │   └── WorkflowStatus.java
│   ├── config/              # Конфигурации
│   │   ├── TemporalWorkerConfig.java
│   │   └── TemporalClientConfig.java
│   └── finance/payment/     # Примеры финансовых интерфейсов
│       ├── PaymentActivities.java
│       └── PaymentWorkflow.java
```

## Зависимости

```xml
<dependency>
    <groupId>ru.ilyshka</groupId>
    <artifactId>temporal-interfaces</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

## Использование

### 1. Создание Activity интерфейса

```java
@BaseActivityInterface(
    value = "MyActivities",
    description = "Мои активности",
    category = "general"
)
public interface MyActivities {

    @ActivityMethod
    void doSomething(String id);

    @ActivityMethod
    String process(String input);
}
```

### 2. Создание Workflow интерфейса

```java
@BaseWorkflowInterface(
    value = "MyWorkflow",
    description = "Мой workflow",
    taskQueue = "my-tasks"
)
public interface MyWorkflow {

    @WorkflowMethod
    void execute(String input);

    @WorkflowSignal(value = "cancel", description = "Отмена")
    void cancel(String reason);

    @WorkflowQuery(value = "get-status", description = "Статус")
    String getStatus();
}
```

### 3. Использование конфигураций

```java
// Worker конфигурация
TemporalWorkerConfig workerConfig = TemporalWorkerConfig.builder()
    .host("localhost")
    .port(7233)
    .namespace("my-namespace")
    .taskQueue("my-tasks")
    .build();

// Client конфигурация
TemporalClientConfig clientConfig = TemporalClientConfig.builder()
    .host("localhost")
    .port(7233)
    .namespace("my-namespace")
    .workflowRunTimeout(Duration.ofMinutes(10))
    .build();
```

### 4. Использование Activity Options

```java
BaseActivityOptions options = PaymentActivities.defaultPaymentOptions();
ActivityOptions temporalOptions = options.toActivityOptions();
```

### 5. Работа с результатами

```java
WorkflowResult<String> result = WorkflowResult.success(
    workflowId,
    runId,
    "processed data"
);

if (result.isSuccess()) {
    // обработка
}
```

## Версии

- Temporal SDK: 1.26.2
- Java: 21
- Spring Boot: 3.5.0
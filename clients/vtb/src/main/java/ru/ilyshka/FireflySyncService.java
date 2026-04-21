package ru.ilyshka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FireflySyncService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ======================== КОНФИГУРАЦИЯ (замените под себя) ========================
    private static final String FIREFLY_BASE_URL = "http://192.168.2.100:30105";
    private static final String FIREFLY_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIwMTlkYjE1NS1kMzlmLTcyOGQtODE2YS1kNmM5MzM0OWFmZGYiLCJqdGkiOiJhZjkxN2UxZmI0NzI0MGU0ZjVkZDljMjAyYWFkNzJkOTk2YjE5ZDZjODliODYwYzgwNjNlNDUxNzIxZDNlMGFjY2Q4OGY0ZWRmYTRmYmJiNCIsImlhdCI6MTc3Njc5NzQxMi41NDQxODQsIm5iZiI6MTc3Njc5NzQxMi41NDQxODYsImV4cCI6MTgwODMzMzQxMi41MDYwNjYsInN1YiI6IjEiLCJzY29wZXMiOltdfQ.Av0KwAL1Yum0cVOudPWSoaYWyRJROMKjfZp3EVHAM7vTGaKJqBrCY69i47TwrJuGZMPfo-OExsedWnQNTFWgr5q96VO9Wx8UAg8y2bq0TkJPCr4Acvsx1fRxoaVOLK3Fjq8FQw_5OMJfFuiEIHdsYkCV_YnOw5TdJFl9y5rokHc0VDibdwESeUvx8C6yOpwSp7355mL_Kd5ldfxPsNDkK5rjYp0mf1yLqQe-ZIxdbJSf0YqhYqDYl8Rro8EUyjV15wwwJHHlMJeXFhgMZQXouujqXE9KRypUTHtSshq0XhGe9Ky0yZojxiqSAg9Z3VxOolp0ros_mMS3ejAsBL7FKYYGVjaDkZ0XqHNRfnyvM6Tz6h6SGxiDrqwn3ls6umjiKX41IWbuaaTepWS1MSh__PT4lWi2MZCRyEEzMChONp7K9RUJVi4uyuLsnvQ-nmSG3kOk7QaNPvF0SmhMqhjfDKgQt4iEx9iGIt1TQ-rTZxkSXsWgAoLwHvD5rg_cNE1vSq7YqwVGNvt-z-yZjBu1zeZKkO7NmeP4bPxsj-BXXx8i2b7TXEFDUAn9APiUMYEg4seYPBdWTIdATppz8iFQKE1yi0Fz8mw4e0wpES1ZDEAbY_RhuSGm6FT3djT_WmOy3vMUg9Ay-IgrBByGB__hV6aA30kWbZ2cGUHLvVNz6CI";

    // Названия счетов в Firefly III (должны существовать)
    private static final String ACCOUNT_MAIN = "ВТБ";
    private static final String ACCOUNT_SAVINGS = "Сберегательный счёт в ВТБ";
    private static final String ACCOUNT_MORTGAGE = "Ипотека СЖ с господдержкой 2020 для IT.";
    private static final String ACCOUNT_UNKNOWN = "Неизвестный счёт";

    // Маски счетов ВТБ (как приходят в поле "account")
    private static final String MASK_MAIN = "*3314";
    private static final String MASK_SAVINGS = "*0502";
    private static final String MASK_MORTGAGE = "*0891";

    private final HttpClient httpClient;
    private String mainAccountId;
    private String savingsAccountId;
    private String mortgageAccountId;
    private String unknownAccountId;

    private final Map<String, String> maskToId = new HashMap<>();

    public FireflySyncService() {
        this.httpClient = HttpClient.newBuilder().build();
    }

    public static void main(String[] args) {
        try {
            new FireflySyncService().sync("E:\\Данные");
        } catch (Exception e) {
            System.err.println("Ошибка синхронизации: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void sync(String jsonFolderPath) throws IOException, InterruptedException {
        initializeAccountIds();
        Path inputPath = Paths.get(jsonFolderPath);
        List<Path> jsonFiles = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.json")) {
            stream.forEach(jsonFiles::add);
        }

        for (Path file : jsonFiles) {
            System.out.println("\n📄 Обработка файла: " + file.getFileName());
            processFile(file);
        }
        System.out.println("\n✅ Синхронизация завершена!");
    }

    private void initializeAccountIds() throws IOException, InterruptedException {
        System.out.println("Получение списка счетов из Firefly III...");
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FIREFLY_BASE_URL + "/api/v1/accounts"))
                .header("Authorization", "Bearer " + FIREFLY_TOKEN)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Не удалось получить счета: " + response.body());
        }

        JsonNode root = mapper.readTree(response.body());
        JsonNode accounts = root.path("data");

        for (JsonNode account : accounts) {
            String name = account.path("attributes").path("name").asText("");
            String id = account.path("id").asText("");

            if (name.equals(ACCOUNT_MAIN)) {
                mainAccountId = id;
                maskToId.put(MASK_MAIN, id);
            } else if (name.equals(ACCOUNT_SAVINGS)) {
                savingsAccountId = id;
                maskToId.put(MASK_SAVINGS, id);
            } else if (name.equals(ACCOUNT_MORTGAGE)) {
                mortgageAccountId = id;
                maskToId.put(MASK_MORTGAGE, id);
            } else if (name.equals(ACCOUNT_UNKNOWN)) {
                unknownAccountId = id;
            }
            System.out.println("Найден счет: " + name + " (ID: " + id + ")");
        }

        if (mainAccountId == null || savingsAccountId == null || mortgageAccountId == null || unknownAccountId == null) {
            throw new RuntimeException("Не удалось найти все необходимые счета! " +
                    "main=" + mainAccountId + ", savings=" + savingsAccountId +
                    ", mortgage=" + mortgageAccountId + ", unknown=" + unknownAccountId);
        }
        System.out.println("ID счетов успешно получены.\n");
    }

    private void processFile(Path file) throws IOException, InterruptedException {
        List<String> lines = Files.readAllLines(file);
        int lineNumber = 0;
        for (String line : lines) {
            lineNumber++;
            line = line.trim();
            if (line.isEmpty()) continue;

            JsonNode op;
            try {
                op = mapper.readTree(line);
            } catch (Exception e) {
                System.err.printf("⚠️ Ошибка парсинга строки %d в файле %s: %s%n",
                        lineNumber, file.getFileName(), line);
                continue;
            }

            if (op.has("operations") && op.get("operations").isArray()) {
                for (JsonNode subOp : op.get("operations")) {
                    processOperation(subOp);
                }
            } else {
                processOperation(op);
            }
        }
    }

    private void processOperation(JsonNode op) throws IOException, InterruptedException {
        String date = extractDate(op);
        String uniqueId = getUniqueId(op);
        double amount = op.path("operationAmount").path("sum").asDouble();
        boolean isDebet = op.path("debet").asBoolean(true);
        String accountMask = op.path("account").asText("");
        String notes = generateNotes(op);
        String status = op.path("status").asText("");
        String operationType = op.path("operationType").asText("");
        String fullName = op.path("fullOperationName").asText("");

        // 1. Пропускаем отклонённые транзакции
        if ("Declined".equalsIgnoreCase(status)) {
            System.out.printf("⏭️ Пропущена отклонённая операция: external_id=%s, статус=%s, дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    uniqueId, status, date, amount, notes);
            return;
        }

        // 2. Для переводов между своими счетами обрабатываем только сторону списания (debet=true)
        if ("Между своими счетами".equals(operationType) && !isDebet) {
            System.out.printf("⏭️ Пропущена сторона зачисления при переводе между счетами: external_id=%s, статус=%s, дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    uniqueId, status, date, amount, notes);
            return;
        }

        // 3. Для кредитных операций (погашение) обрабатываем только списание с основного счёта
        if (("Платеж по кредиту".equals(operationType) || "Операции по кредитам".equals(operationType) ||
                fullName.contains("Погашение обязательств по кредитному договору")) && !isDebet) {
            System.out.printf("⏭️ Пропущена сторона кредитной операции (зачисление на кредитный счёт): external_id=%s, статус=%s, дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    uniqueId, status, date, amount, notes);
            return;
        }

        String fireflyAccountId = getFireflyAccountIdByMask(accountMask);

        String type;
        String sourceId = null;
        String destinationId = null;

        if (!isDebet) {
            // Доход (зачисление)
            type = "deposit";
            destinationId = fireflyAccountId;
        } else {
            // Расход
            // Погашение кредита (списание с основного на ипотечный)
            if ("Операции по кредитам".equals(operationType) || "Платеж по кредиту".equals(operationType) ||
                    fullName.contains("Погашение обязательств по кредитному договору")) {
                type = "transfer";
                sourceId = mainAccountId;
                destinationId = mortgageAccountId;
            } else if (fireflyAccountId.equals(mortgageAccountId)) {
                // Погашение ипотеки по маске счёта (резерв)
                type = "transfer";
                sourceId = mainAccountId;
                destinationId = mortgageAccountId;
            } else if ("Между своими счетами".equals(operationType)) {
                if (fireflyAccountId.equals(savingsAccountId)) {
                    type = "transfer";
                    sourceId = savingsAccountId;
                    destinationId = mainAccountId;
                } else if (fireflyAccountId.equals(mainAccountId)) {
                    type = "transfer";
                    sourceId = mainAccountId;
                    destinationId = savingsAccountId;
                } else {
                    type = "transfer";
                    sourceId = unknownAccountId;
                    destinationId = mainAccountId;
                    System.out.printf("ℹ️ Перевод с неизвестного счёта '%s' на основной: %s%n", accountMask, notes);
                }
            } else {
                // Обычный расход
                type = "withdrawal";
                sourceId = fireflyAccountId;
            }
        }

        if (transactionExistsByExternalId(uniqueId)) {
            System.out.printf("⚠️ Пропущено (дубликат по external_id=%s): дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    uniqueId, date, amount, notes);
            return;
        }

        createTransaction(type, date, amount, notes, sourceId, destinationId, uniqueId);
        System.out.printf("✅ Создана транзакция: external_id=%s, дата=%s, сумма=%.2f руб., тип=%s, описание=\"%s\"%n",
                uniqueId, date, amount, type, notes);
    }

    private String extractDate(JsonNode op) {
        String dateStr = op.path("transactionDate").asText(null);
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = op.path("operationDate").asText("");
        }
        if (!dateStr.isEmpty()) {
            try {
                return LocalDate.parse(dateStr.substring(0, 10)).format(dateFormatter);
            } catch (Exception e) {
                return LocalDate.now().format(dateFormatter);
            }
        }
        return LocalDate.now().format(dateFormatter);
    }

    private String generateNotes(JsonNode op) {
        StringBuilder sb = new StringBuilder();

        String operationType = op.path("operationType").asText("");
        if (!operationType.isEmpty()) {
            sb.append(operationType);
        } else {
            sb.append("Неизвестный тип");
        }

        String fullName = op.path("fullOperationName").asText("");
        String operationName = op.path("operationName").asText("");
        if (!fullName.isEmpty() && !fullName.equals(operationType) && !fullName.equals(operationName)) {
            sb.append(": ").append(fullName);
        } else if (!operationName.isEmpty() && !operationName.equals(operationType)) {
            sb.append(": ").append(operationName);
        }

        JsonNode merchant = op.path("merchant");
        String merchantTitle = merchant.path("title").asText("");
        if (!merchantTitle.isEmpty() && !"Оплата по СБП".equals(operationType)) {
            sb.append(" | Магазин: ").append(merchantTitle);
            String mcc = merchant.path("mcc").asText("");
            if (!mcc.isEmpty()) {
                sb.append(" (MCC:").append(mcc).append(")");
            }
        }

        String rrn = op.path("rrn").asText("");
        if (!rrn.isEmpty()) {
            sb.append(" | RRN: ").append(rrn);
        }

        switch (operationType) {
            case "Исходящий перевод СБП":
            case "Входящий перевод СБП":
                extractSbpTransferDetails(op, sb);
                break;
            case "Мобильная связь":
                extractMobileDetails(op, sb);
                break;
            case "Между своими счетами":
                extractOwnTransferDetails(op, sb);
                break;
            case "Коммунальные платежи":
                extractUtilityDetails(op, sb);
                break;
            case "Оплата по СБП":
                extractSbpPaymentDetails(op, sb);
                break;
            default:
                String message = op.path("message").asText("");
                if (!message.isEmpty()) {
                    sb.append(" | ").append(message);
                }
                break;
        }

        return sb.toString();
    }

    private void extractSbpTransferDetails(JsonNode op, StringBuilder sb) {
        JsonNode props = op.path("properties");
        for (JsonNode prop : props) {
            String key = prop.path("key").asText();
            if ("ExternalFIO".equals(key)) {
                sb.append(" | Получатель: ").append(prop.path("value").asText(""));
            } else if ("PhoneNumber".equals(key)) {
                sb.append(" | Тел.: ").append(prop.path("value").asText(""));
            } else if ("FPSBankName".equals(key)) {
                sb.append(" | Банк: ").append(prop.path("value").asText(""));
            }
        }
    }

    private void extractMobileDetails(JsonNode op, StringBuilder sb) {
        String message = op.path("message").asText("");
        if (!message.isEmpty()) {
            sb.append(" | ").append(message);
        } else {
            JsonNode props = op.path("properties");
            for (JsonNode prop : props) {
                if ("Phone".equals(prop.path("key").asText())) {
                    sb.append(" | Номер: ").append(prop.path("value").asText(""));
                    break;
                }
            }
        }
    }

    private void extractOwnTransferDetails(JsonNode op, StringBuilder sb) {
        String fromAccount = op.path("account").asText("");
        String toAccount = null;
        JsonNode props = op.path("properties");
        for (JsonNode prop : props) {
            if ("receiverAccount".equals(prop.path("key").asText())) {
                toAccount = prop.path("value").asText("");
                break;
            }
        }
        if (fromAccount != null && !fromAccount.isEmpty()) {
            sb.append(" | Счёт списания: ").append(fromAccount);
        }
        if (toAccount != null && !toAccount.isEmpty()) {
            sb.append(" → Счёт зачисления: ").append(toAccount);
        }
    }

    private void extractUtilityDetails(JsonNode op, StringBuilder sb) {
        String message = op.path("message").asText("");
        if (!message.isEmpty()) {
            sb.append(" | ").append(message);
        } else {
            JsonNode props = op.path("properties");
            String address = "", account = "", period = "";
            for (JsonNode prop : props) {
                String key = prop.path("key").asText();
                if ("PAYERADDRESS".equals(key)) address = prop.path("value").asText("");
                else if ("PHONE".equals(key)) account = prop.path("value").asText("");
                else if ("PAYMPERIOD".equals(key)) period = prop.path("value").asText("");
            }
            if (!address.isEmpty()) sb.append(" | Адрес: ").append(address);
            if (!account.isEmpty()) sb.append(" | Лиц. счёт: ").append(account);
            if (!period.isEmpty()) sb.append(" | Период: ").append(period);
        }
    }

    private void extractSbpPaymentDetails(JsonNode op, StringBuilder sb) {
        JsonNode merchant = op.path("merchant");
        String merchantTitle = merchant.path("title").asText("");
        if (!merchantTitle.isEmpty()) {
            sb.append(" | Магазин: ").append(merchantTitle);
            String mcc = merchant.path("mcc").asText("");
            if (!mcc.isEmpty()) {
                sb.append(" (MCC:").append(mcc).append(")");
            }
        }
        String message = op.path("message").asText("");
        if (!message.isEmpty()) {
            sb.append(" | ").append(message);
        }
        JsonNode props = op.path("properties");
        for (JsonNode prop : props) {
            if ("documentId".equals(prop.path("key").asText())) {
                sb.append(" | Док.: ").append(prop.path("value").asText(""));
                break;
            }
        }
    }

    private String getFireflyAccountIdByMask(String vtbAccountMask) {
        String id = maskToId.get(vtbAccountMask);
        if (id != null) return id;
        System.err.println("⚠️ Неизвестная маска счёта '" + vtbAccountMask + "', используется счёт 'Неизвестный счёт'.");
        return unknownAccountId;
    }

    private String getUniqueId(JsonNode op) {
        String internalId = op.path("internalId").asText("");
        if (!internalId.isEmpty()) return internalId;

        String rrn = op.path("rrn").asText("");
        if (!rrn.isEmpty()) return rrn;

        String chainId = op.path("chainId").asText("");
        if (!chainId.isEmpty()) return chainId;

        JsonNode transactions = op.path("transactions");
        if (transactions.isArray() && transactions.size() > 0) {
            String txId = transactions.get(0).path("transactionId").asText("");
            if (!txId.isEmpty()) return txId;
        }

        return String.format("%s_%s_%s",
                extractDate(op),
                op.path("operationAmount").path("sum").asText("0"),
                op.path("account").asText(""));
    }

    private boolean transactionExistsByExternalId(String externalId) throws IOException, InterruptedException {
        if (externalId == null || externalId.isEmpty()) return false;

        UriComponents query = UriComponentsBuilder.fromHttpUrl(FIREFLY_BASE_URL + "/api/v1/search/transactions")
                .queryParam("query", String.format("external_id:%s", externalId))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(query.toUri())
                .header("Authorization", "Bearer " + FIREFLY_TOKEN)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) return false;

        JsonNode root = mapper.readTree(response.body());
        return root.path("data").size() > 0;
    }

    private void createTransaction(String type, String date, double amount, String description,
                                   String sourceId, String destinationId, String externalId) throws IOException, InterruptedException {
        ObjectNode transaction = mapper.createObjectNode();
        if (sourceId != null) transaction.put("source_id", sourceId);
        if (destinationId != null) transaction.put("destination_id", destinationId);
        transaction.put("type", type);
        transaction.put("date", date);
        transaction.put("amount", String.valueOf(amount));
        transaction.put("description", description);
        transaction.put("external_id", externalId);
        transaction.put("error_if_duplicate_hash", true);

        ObjectNode root = mapper.createObjectNode();
        root.set("transactions", mapper.createArrayNode().add(transaction));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FIREFLY_BASE_URL + "/api/v1/transactions"))
                .header("Authorization", "Bearer " + FIREFLY_TOKEN)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(root)))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int statusCode = response.statusCode();
        if (statusCode != 200) {
            String responseBody = response.body();
            // При 302/301 выводим больше деталей для диагностики
            if (statusCode == 302 || statusCode == 301) {
                String location = response.headers().firstValue("Location").orElse("неизвестно");
                throw new RuntimeException(String.format(
                        "Редирект %d. Проверьте URL и токен. Location: %s. Тело ответа: %s",
                        statusCode, location, responseBody));
            }
            throw new RuntimeException("Ошибка создания транзакции (код " + statusCode + "): " + responseBody);
        }
    }
}
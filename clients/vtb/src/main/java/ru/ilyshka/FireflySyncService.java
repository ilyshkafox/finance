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
import java.util.List;

public class FireflySyncService {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ======================== КОНФИГУРАЦИЯ (замените под себя) ========================
    private static final String FIREFLY_BASE_URL = "http://192.168.2.100:30105";
    private static final String FIREFLY_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIwMTlkYjE1NS1kMzlmLTcyOGQtODE2YS1kNmM5MzM0OWFmZGYiLCJqdGkiOiJhZjkxN2UxZmI0NzI0MGU0ZjVkZDljMjAyYWFkNzJkOTk2YjE5ZDZjODliODYwYzgwNjNlNDUxNzIxZDNlMGFjY2Q4OGY0ZWRmYTRmYmJiNCIsImlhdCI6MTc3Njc5NzQxMi41NDQxODQsIm5iZiI6MTc3Njc5NzQxMi41NDQxODYsImV4cCI6MTgwODMzMzQxMi41MDYwNjYsInN1YiI6IjEiLCJzY29wZXMiOltdfQ.Av0KwAL1Yum0cVOudPWSoaYWyRJROMKjfZp3EVHAM7vTGaKJqBrCY69i47TwrJuGZMPfo-OExsedWnQNTFWgr5q96VO9Wx8UAg8y2bq0TkJPCr4Acvsx1fRxoaVOLK3Fjq8FQw_5OMJfFuiEIHdsYkCV_YnOw5TdJFl9y5rokHc0VDibdwESeUvx8C6yOpwSp7355mL_Kd5ldfxPsNDkK5rjYp0mf1yLqQe-ZIxdbJSf0YqhYqDYl8Rro8EUyjV15wwwJHHlMJeXFhgMZQXouujqXE9KRypUTHtSshq0XhGe9Ky0yZojxiqSAg9Z3VxOolp0ros_mMS3ejAsBL7FKYYGVjaDkZ0XqHNRfnyvM6Tz6h6SGxiDrqwn3ls6umjiKX41IWbuaaTepWS1MSh__PT4lWi2MZCRyEEzMChONp7K9RUJVi4uyuLsnvQ-nmSG3kOk7QaNPvF0SmhMqhjfDKgQt4iEx9iGIt1TQ-rTZxkSXsWgAoLwHvD5rg_cNE1vSq7YqwVGNvt-z-yZjBu1zeZKkO7NmeP4bPxsj-BXXx8i2b7TXEFDUAn9APiUMYEg4seYPBdWTIdATppz8iFQKE1yi0Fz8mw4e0wpES1ZDEAbY_RhuSGm6FT3djT_WmOy3vMUg9Ay-IgrBByGB__hV6aA30kWbZ2cGUHLvVNz6CI";
    private static final String ACCOUNT_MAIN = "ВТБ";
    private static final String ACCOUNT_SAVINGS = "Сберегательный счёт в ВТБ";
    private static final String ACCOUNT_MORTGAGE = "Ипотека СЖ с господдержкой 2020 для IT.";

    // Маски счетов ВТБ (как приходят в поле "account")
    private static final String MASK_MAIN = "*3314";
    private static final String MASK_SAVINGS = "*0502";
    private static final String MASK_MORTGAGE = "*0891";

    private final HttpClient httpClient;
    private String mainAccountId;
    private String savingsAccountId;
    private String mortgageAccountId;

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

            if (name.equals(ACCOUNT_MAIN)) mainAccountId = id;
            else if (name.equals(ACCOUNT_SAVINGS)) savingsAccountId = id;
            else if (name.equals(ACCOUNT_MORTGAGE)) mortgageAccountId = id;

            System.out.println("Найден счет: " + name + " (ID: " + id + ")");
        }

        if (mainAccountId == null || savingsAccountId == null || mortgageAccountId == null) {
            throw new RuntimeException("Не удалось найти все необходимые счета в Firefly III! " +
                    "main=" + mainAccountId + ", savings=" + savingsAccountId + ", mortgage=" + mortgageAccountId);
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

            // Если строка содержит массив "operations", обрабатываем каждый элемент
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
        double amount = op.path("operationAmount").path("sum").asDouble();
        boolean isDebet = op.path("debet").asBoolean(true);
        String accountMask = op.path("account").asText("");
        String notes = generateNotes(op);
        String status = op.path("status").asText("");
        String operationType = op.path("operationType").asText("");

        // 1. Пропускаем отклонённые транзакции
        if ("Declined".equalsIgnoreCase(status)) {
            System.out.printf("⏭️ Пропущена отклонённая операция: статус=%s, дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    status, date, amount, notes);
            return;
        }

        // 2. Для переводов между своими счетами обрабатываем только сторону списания (debet=true)
        if ("Между своими счетами".equals(operationType) && !isDebet) {
            System.out.printf("⏭️ Пропущена сторона зачисления при переводе между счетами: %s%n", notes);
            return;
        }

        // Определяем ID счёта в Firefly по маске
        String vtbAccountFireflyId = getFireflyAccountIdByMask(accountMask);

        String type;
        String sourceId = null;
        String destinationId = null;

        if (!isDebet) {
            // Доход (зачисление)
            type = "deposit";
            destinationId = vtbAccountFireflyId;
        } else {
            // Расход / перевод
            if (vtbAccountFireflyId.equals(savingsAccountId)) {
                type = "transfer";
                sourceId = savingsAccountId;
                destinationId = mainAccountId;
            } else if (vtbAccountFireflyId.equals(mortgageAccountId)) {
                type = "transfer";
                sourceId = mainAccountId;
                destinationId = mortgageAccountId;
            } else {
                type = "withdrawal";
                sourceId = vtbAccountFireflyId;
            }
        }

        // Проверка дубликата по уникальному external_id
        String uniqueId = getUniqueId(op);
        if (transactionExistsByExternalId(uniqueId)) {
            System.out.printf("⚠️ Пропущено (дубликат по external_id=%s): дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    uniqueId, date, amount, notes);
            return;
        }

        // Создаём транзакцию
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

    /**
     * Формирует подробное описание транзакции, начиная с operationType.
     */
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

        // Магазин (для оплат, не для СБП-платежей)
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

        // Детали по типам операций
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

        double amount = op.path("operationAmount").path("sum").asDouble(0);
        String date = extractDate(op);
        sb.append(String.format(" | %.2f руб. | %s", amount, date));

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
        if (vtbAccountMask.equals(MASK_MAIN)) return mainAccountId;
        if (vtbAccountMask.equals(MASK_SAVINGS)) return savingsAccountId;
        if (vtbAccountMask.equals(MASK_MORTGAGE)) return mortgageAccountId;
        System.err.println("⚠️ Неизвестная маска счёта '" + vtbAccountMask + "', используется основной счёт.");
        return mainAccountId;
    }

    /**
     * Генерирует уникальный идентификатор транзакции для external_id.
     * Приоритет: internalId -> rrn -> chainId -> первый transactionId -> резерв.
     */
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

        // Резерв: дата+сумма+маска счёта (для старых данных)
        return String.format("%s_%s_%s",
                extractDate(op),
                op.path("operationAmount").path("sum").asText("0"),
                op.path("account").asText(""));
    }

    /**
     * Проверяет, существует ли уже транзакция с таким external_id в Firefly III.
     */
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

    /**
     * Создаёт транзакцию в Firefly III с указанным external_id.
     */
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
        if (response.statusCode() != 200) {
            throw new RuntimeException("Ошибка создания транзакции: " + response.body());
        }
    }
}
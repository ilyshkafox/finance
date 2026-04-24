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

    // ======================== КОНФИГУРАЦИЯ ========================
    private static final String FIREFLY_BASE_URL = "http://192.168.2.100:30105";
    private static final String FIREFLY_TOKEN = "eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiJ9.eyJhdWQiOiIwMTlkYjE1NS1kMzlmLTcyOGQtODE2YS1kNmM5MzM0OWFmZGYiLCJqdGkiOiJmOTA4Y2NkZjFhZGVmOTQ2OTJhZGNjODRiY2NkNjU2ZWUxYzlmNDI4OTA5ZTgzZWIwY2Y4ZmE4NzYyZDJiYjcyYTI3NDgzYmNhYTFiYzU0MCIsImlhdCI6MTc3NjgwNzI2NS4yNTcwODMsIm5iZiI6MTc3NjgwNzI2NS4yNTcwODYsImV4cCI6MTgwODM0MzI2NS4yMjE3MDUsInN1YiI6IjEiLCJzY29wZXMiOltdfQ.YS_pGMPesUmi1IEguB3KoBKUDkDnVd50oMCUUmwpL82g1RtIpZpMt2Iuvbms5qSN6fHd0qjG8NLg2sParayuFBqWMmp7kh-80URiFOeQ5tJO-MKDbu8DlmRg82_w5oKiPGgWIVwhisj1EdhqzJEk6XPL0NOKa_VBD0PdFJaBs3iCZkO5pJ_lR3-V0-UaHUIW9sUZvHmi9Dpw8wyYbU0p28COaeZqJU38MrcU_BxG1nHZNbCOuXec1liuAxTdIleu5GJ11ter-b5tXg9zCjPhxhcSj4JeSJRZHdKD0KKIZg78CrMvSn1fbU1zOkeybDjVtZcCoLrMmeVEP9F_QjzLC12AIyvhhHnwznQh5dO-ii9C5WSKcahyn2PeWvlhqCdjrEjPY5jnLhL-9CtUfIA3P29g6mEN4D-wkLvZx5ORWlfH0qZ5fbQXDFOl18bJ3BE74ND2aB0mkZwNYz5nIMh5E44aJlaKcJOjmFo5ZtkjB7vQNU84cGZEP3RGlctzYS_ru8ko9fBBzCUl3G2LpNQt1ahZCY8-l34MTrAky0ajQ3mRloZfAHKAOOzVLfOlJ2viLx3Yl2ab0l7EiIhb0Vg5Odar7ZIks-DpvCSwUBbcU9du6v_ZvXFFbN1VmXn_nt3IaV7EL1t_Roy2LjMvPOOUTu1OTQMEMJjsZja_1E6s1Vo";

    private static final String ACCOUNT_MAIN = "ВТБ";
    private static final String ACCOUNT_SAVINGS = "Сберегательный счёт в ВТБ";
    private static final String ACCOUNT_MORTGAGE = "Ипотека СЖ с господдержкой 2020 для IT.";
    private static final String ACCOUNT_UNKNOWN = "Неизвестный счёт";
    private static final String ACCOUNT_INTEREST = "Проценты по кредиту";

    private static final String MASK_MAIN = "*3314";
    private static final String MASK_SAVINGS = "*0502";
    private static final String MASK_MORTGAGE = "*0891";

    private final HttpClient httpClient;
    private String mainAccountId;
    private String savingsAccountId;
    private String mortgageAccountId;
    private String unknownAccountId;
    private String interestAccountId;

    private final Map<String, String> maskToId = new HashMap<>();

    public FireflySyncService() {
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
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
            System.out.println("Найден счет: '" + name + "' (ID: " + id + ")");

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
            } else if (name.equals(ACCOUNT_INTEREST)) {
                interestAccountId = id;
            }
        }

        if (mainAccountId == null || savingsAccountId == null || mortgageAccountId == null || unknownAccountId == null) {
            throw new RuntimeException("Не найдены обязательные счета: " +
                    "main=" + mainAccountId + ", savings=" + savingsAccountId +
                    ", mortgage=" + mortgageAccountId + ", unknown=" + unknownAccountId);
        }
        if (interestAccountId == null) {
            System.err.println("⚠️ Счёт '" + ACCOUNT_INTEREST + "' не найден. Проценты по кредиту не будут учитываться!");
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
        double totalAmount = op.path("operationAmount").path("sum").asDouble();
        boolean isDebet = op.path("debet").asBoolean(true);
        String accountMask = op.path("account").asText("");
        String notes = generateNotes(op);
        String status = op.path("status").asText("");
        String operationType = op.path("operationType").asText("");
        String fullName = op.path("fullOperationName").asText("");

        // 1. Пропускаем отклонённые
        if ("Declined".equalsIgnoreCase(status)) {
            System.out.printf("⏭️ Пропущена отклонённая операция: статус=%s, дата=%s, сумма=%.2f руб., описание=\"%s\"%n",
                    status, date, totalAmount, notes);
            return;
        }

        // 2. Переводы между счетами – только списание
        if ("Между своими счетами".equals(operationType) && !isDebet) {
            System.out.printf("⏭️ Пропущена сторона зачисления при переводе между счетами: %s%n", notes);
            return;
        }

        // 3. Кредитные операции
        if (("Платеж по кредиту".equals(operationType) || "Операции по кредитам".equals(operationType) ||
                fullName.contains("Погашение обязательств по кредитному договору"))) {
            if (isDebet) {
                // Списание – пропускаем (оно не содержит разбивки)
                System.out.printf("⏭️ Пропущена сторона списания по кредиту: %s%n", notes);
                return;
            } else {
                // Зачисление – содержит разбивку на проценты и основной долг
                double interest = 0;
                double principal = totalAmount;
                JsonNode props = op.path("properties");
                for (JsonNode prop : props) {
                    String key = prop.path("key").asText();
                    if ("summa[0]".equals(key)) {
                        interest = prop.path("value").asDouble();
                    } else if ("summa[1]".equals(key)) {
                        principal = prop.path("value").asDouble();
                    }
                }
                if (Double.isNaN(interest) || Double.isInfinite(interest)) interest = 0;
                if (Double.isNaN(principal) || Double.isInfinite(principal)) principal = totalAmount - interest;

                String uniqueIdBase = getUniqueId(op);

                // Основной долг -> на ипотечный счёт
                String uniqueIdPrincipal = uniqueIdBase + "_principal";
                if (!transactionExistsByExternalId(uniqueIdPrincipal)) {
                    createTransaction("withdrawal", date, principal,
                            notes + " (основной долг)", mainAccountId, mortgageAccountId, uniqueIdPrincipal);
                    System.out.printf("✅ Создано погашение основного долга: %.2f руб.\n", principal);
                } else {
                    System.out.printf("⚠️ Пропущен основной долг (дубликат): %s\n", uniqueIdPrincipal);
                }

                // Проценты -> на счёт расходов (если есть)
                if (interest > 0 && interestAccountId != null) {
                    String uniqueIdInterest = uniqueIdBase + "_interest";
                    if (!transactionExistsByExternalId(uniqueIdInterest)) {
                        createTransaction("withdrawal", date, interest,
                                notes + " (проценты)", mainAccountId, interestAccountId, uniqueIdInterest);
                        System.out.printf("✅ Созданы проценты по кредиту: %.2f руб.\n", interest);
                    } else {
                        System.out.printf("⚠️ Пропущены проценты (дубликат): %s\n", uniqueIdInterest);
                    }
                } else if (interest > 0 && interestAccountId == null) {
                    System.err.printf("⚠️ Проценты %.2f руб. не сохранены – отсутствует счёт '%s'\n", interest, ACCOUNT_INTEREST);
                }
                return;
            }
        }

        // 4. Обычные операции (не кредит)
        String fireflyAccountId = getFireflyAccountIdByMask(accountMask);
        String type;
        String sourceId = null;
        String destinationId = null;

        if (!isDebet) {
            type = "deposit";
            destinationId = fireflyAccountId;
        } else {
            if (fireflyAccountId.equals(mortgageAccountId)) {
                type = "withdrawal";
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
                }
            } else {
                type = "withdrawal";
                sourceId = fireflyAccountId;
            }
        }

        String uniqueId = getUniqueId(op);
        if (transactionExistsByExternalId(uniqueId)) {
            System.out.printf("⚠️ Пропущено (дубликат по external_id=%s): %s\n", uniqueId, notes);
            return;
        }

        createTransaction(type, date, totalAmount, notes, sourceId, destinationId, uniqueId);
        System.out.printf("✅ Создана транзакция: external_id=%s, сумма=%.2f руб., тип=%s\n", uniqueId, totalAmount, type);
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
        if (!operationType.isEmpty()) sb.append(operationType);
        else sb.append("Неизвестный тип");

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
            if (!mcc.isEmpty()) sb.append(" (MCC:").append(mcc).append(")");
        }

        String rrn = op.path("rrn").asText("");
        if (!rrn.isEmpty()) sb.append(" | RRN: ").append(rrn);

        String message = op.path("message").asText("");
        if (!message.isEmpty()) sb.append(" | ").append(message);

        return sb.toString();
    }

    private String getFireflyAccountIdByMask(String mask) {
        String id = maskToId.get(mask);
        if (id != null) return id;
        System.err.println("⚠️ Неизвестная маска '" + mask + "', используется 'Неизвестный счёт'.");
        return unknownAccountId;
    }

    private String getUniqueId(JsonNode op) {
        String internalId = op.path("internalId").asText("");
        if (!internalId.isEmpty()) return internalId;
        String rrn = op.path("rrn").asText("");
        if (!rrn.isEmpty()) return rrn;
        String chainId = op.path("chainId").asText("");
        if (!chainId.isEmpty()) return chainId;
        JsonNode tx = op.path("transactions");
        if (tx.isArray() && tx.size() > 0) {
            String txId = tx.get(0).path("transactionId").asText("");
            if (!txId.isEmpty()) return txId;
        }
        return String.format("%s_%s_%s", extractDate(op),
                op.path("operationAmount").path("sum").asText("0"),
                op.path("account").asText(""));
    }

    private boolean transactionExistsByExternalId(String externalId) throws IOException, InterruptedException {
        if (externalId == null || externalId.isEmpty()) return false;
        UriComponents query = UriComponentsBuilder.fromHttpUrl(FIREFLY_BASE_URL + "/api/v1/search/transactions")
                .queryParam("query", "external_id:" + externalId)
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
        if (sourceId == null && destinationId == null)
            throw new RuntimeException("Нет sourceId и destinationId");
        if (type.equals("withdrawal") && sourceId == null)
            throw new RuntimeException("Для withdrawal нужен sourceId");
        if (type.equals("deposit") && destinationId == null)
            throw new RuntimeException("Для deposit нужен destinationId");
        if (type.equals("transfer") && (sourceId == null || destinationId == null))
            throw new RuntimeException("Для transfer нужны sourceId и destinationId");

        ObjectNode transaction = mapper.createObjectNode();
        if (sourceId != null) transaction.put("source_id", sourceId);
        if (destinationId != null) transaction.put("destination_id", destinationId);
        transaction.put("type", type);
        transaction.put("date", date);
        transaction.put("amount", amount);
        transaction.put("description", description);
        transaction.put("external_id", externalId);
        transaction.put("error_if_duplicate_hash", true);

        ObjectNode root = mapper.createObjectNode();
        root.set("transactions", mapper.createArrayNode().add(transaction));

        String bodyJson = mapper.writeValueAsString(root);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FIREFLY_BASE_URL + "/api/v1/transactions"))
                .header("Authorization", "Bearer " + FIREFLY_TOKEN)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code != 200) {
            throw new RuntimeException("Ошибка создания транзакции (код " + code + "): " + response.body());
        }
    }
}
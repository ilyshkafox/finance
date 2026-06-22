package ru.ilyshka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.*;

public class VtbJsonToCsvConverter {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static void main(String[] args) throws IOException {
        String inputDir = args.length > 0 ? args[0] : "E:\\Данные";
        Path inputPath = Paths.get(inputDir);
        Path outputDir = inputPath;

//        if (!Files.exists(inputPath)) {
//            System.err.println("Папка не существует: " + inputPath);
//            return;
//        }

        List<Path> jsonFiles = Arrays.asList(
                Paths.get("E:\\Данные\\input.json")
        );
//        if (Files.isDirectory(inputPath)) {
//            try (DirectoryStream<Path> stream = Files.newDirectoryStream(inputPath, "*.json")) {
//                stream.forEach(jsonFiles::add);
//            }
//        } else {
//            jsonFiles.add(inputPath);
//        }

//        jsonFiles.add(inputPath.resolve("2026-05.json"));
//        jsonFiles.add(inputPath.resolve("2026-04.json"));

        if (jsonFiles.isEmpty()) {
            System.err.println("Не найдено JSON-файлов в " + inputPath);
            return;
        }

        Map<String, List<JsonNode>> operationsByAccount = new LinkedHashMap<>();

        for (Path file : jsonFiles) {
            System.out.println("Обработка файла: " + file.getFileName());
            collectOperations(file, operationsByAccount);
        }

        for (Map.Entry<String, List<JsonNode>> entry : operationsByAccount.entrySet()) {
            String account = entry.getKey();
            writeCsvForAccount(outputDir, account, entry.getValue());
        }

        System.out.println("Готово! Файлы сохранены в " + outputDir.toAbsolutePath());
    }

    private static void collectOperations(Path file, Map<String, List<JsonNode>> opsByAccount) throws IOException {
        List<String> lines = Files.readAllLines(file);

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;

            JsonNode root;
            try {
                root = mapper.readTree(line);
            } catch (Exception e) {
                System.err.println("Ошибка парсинга строки в файле " + file.getFileName() + ": " + line);
                continue;
            }

            List<JsonNode> operations = new ArrayList<>();

            if ("Declined".equalsIgnoreCase(root.get("status").asText())) {
                continue;
            }

            if (root.has("operations") && root.get("operations").isArray()) {
                root.get("operations").forEach(operations::add);
            } else if (root.isArray()) {
                root.forEach(operations::add);
            } else {
                operations.add(root);
            }

            for (JsonNode op : operations) {
                String account = op.path("account").asText("");
                if (account.isEmpty()) {
                    account = "UNKNOWN";
                }
                opsByAccount.computeIfAbsent(account, k -> new ArrayList<>()).add(op);
            }
        }
    }

    private static void writeCsvForAccount(Path outputDir, String account, List<JsonNode> operations) throws IOException {
        String safeAccount = account.replaceAll("[*\\\\/:?\"<>|]", "_");
        Path csvFile = outputDir.resolve("operations_" + safeAccount + ".csv");

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(csvFile))) {
            writer.println("date;deposit;payment;notes");

            for (JsonNode op : operations) {
                writeOperation(op, writer);
            }
        }
        System.out.println("Создан файл: " + csvFile.getFileName());
    }

    private static void writeOperation(JsonNode op, PrintWriter writer) {
        // Дата
        String dateStr = op.path("transactionDate").asText(null);
        if (dateStr == null || dateStr.isEmpty()) {
            dateStr = op.path("operationDate").asText(null);
        }
        String date = "";
        if (dateStr != null && !dateStr.isEmpty()) {
            try {
                date = LocalDate.parse(dateStr.substring(0, 10)).toString();
            } catch (Exception e) {
                date = dateStr;
            }
        }

        // Сумма с обработкой отсутствия числа
        JsonNode amountNode = op.path("operationAmount").path("sum");
        double amount = amountNode.isNumber() ? amountNode.asDouble() : 0.0;
        boolean isDebet = op.path("debet").asBoolean(true);

        String deposit = isDebet ? "0.00" : String.format("%.2f", amount);
        String payment = isDebet ? String.format("%.2f", amount) : "0.00";

        // Заметки
        String notes = generateNotes(op);
        notes = notes.replace("\"", "\"\"");

        writer.printf("%s;%s;%s;\"%s\"%n", date, deposit, payment, notes);
    }

    private static String generateNotes(JsonNode op) {
        StringBuilder sb = new StringBuilder();

        // Тип операции
        String opType = op.path("operationType").asText("");
        if (!opType.isEmpty()) {
            sb.append(opType);
        } else {
            String parentCat = op.path("parentCategory").path("name").asText("");
            if (!parentCat.isEmpty()) {
                sb.append(parentCat);
            } else {
                sb.append(op.path("operationName").asText(""));
            }
        }

        // Название операции
        String name = op.path("operationName").asText("");
        if (name.isEmpty()) {
            name = op.path("fullOperationName").asText("");
        }
        if (!name.isEmpty()) {
            sb.append(": ").append(name);
        }

        // Детали из properties и merchant
        JsonNode props = op.path("properties");
        String externalFio = null;
        String phone = null;
        String bankName = null;
        String merchantTitle = null;

        if (props.isArray()) {
            for (JsonNode prop : props) {
                String key = prop.path("key").asText("");
                String value = prop.path("value").asText("");
                if (value.isEmpty()) continue;

                switch (key) {
                    case "ExternalFIO":
                        externalFio = value;
                        break;
                    case "PhoneNumberF":
                    case "PhoneNumber":
                        phone = value;
                        break;
                    case "FPSBankName":
                        bankName = value;
                        break;
                }
            }
        }

        JsonNode merchant = op.path("merchant");
        if (!merchant.isMissingNode()) {
            merchantTitle = merchant.path("title").asText("");
            if (merchantTitle.isEmpty()) {
                merchantTitle = merchant.path("originalName").asText("");
            }
        }

        String card = op.path("card").asText("");

        List<String> details = new ArrayList<>();
        if (externalFio != null) details.add(externalFio);
        if (phone != null) details.add(phone);
        if (bankName != null) details.add(bankName);
        if (merchantTitle != null) details.add(merchantTitle);
        if (!card.isEmpty()) details.add("карта " + card);

        if (!details.isEmpty()) {
            sb.append(" (").append(String.join(", ", details)).append(")");
        }

        if (sb.length() == 0) {
            sb.append(op.path("operationName").asText(""));
        }

        return sb.toString();
    }
}
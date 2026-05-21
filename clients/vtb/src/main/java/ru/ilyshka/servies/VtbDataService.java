package ru.ilyshka.servies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebElement;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.ilyshka.configuration.VpbProperties;
import ru.ilyshka.dto.Wallet;
import ru.ilyshka.dto.WalletType;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VtbDataService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final VtbAuthService authService;
    private final VpbProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    @SneakyThrows
    public synchronized List<Wallet> getWallets() {
        String authToken = authService.getAuthToken();
        String userFingerprint = authService.getUserFingerprint();

        final String url = "https://online.vtb.ru/msa/api-gw/private/portfolio/portfolio-main-page/portfolios/active" +
                "?requestProductTypes=ACCOUNT%2CSAVING%2CLOAN";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", "USER_FINGERPRINT=" + userFingerprint);
        headers.add("Authorization", "Bearer " + authToken);
        headers.add("referer", "https://online.vtb.ru/home");

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                requestEntity,
                String.class
        );


        Map<String, Object> result = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<>() {
        });
        Map<String, Map<String, Object>> accounts = (Map<String, Map<String, Object>>) result.get("accounts");
        Map<String, Map<String, Object>> deposits = (Map<String, Map<String, Object>>) result.get("deposits");
        Map<String, Map<String, Object>> loans = (Map<String, Map<String, Object>>) result.get("loans");

        return Stream.of(
                        accounts.values(),
                        deposits.values(),
                        loans.values()
                ).flatMap(Collection::stream)
                .map(walletMap -> {
                    String publicId = (String) walletMap.get("publicId");
                    String type = (String) walletMap.get("type");
                    String name = (String) walletMap.get("name");
                    BigDecimal money = new BigDecimal(String.valueOf(((Map<String, Object>) walletMap.get("balance")).get("amount")));

                    return Wallet.builder()
                            .id(publicId)
                            .type(switch (type) {
                                case "MASTER_ACCOUNT" -> WalletType.DEBIT;
                                case "DEPOSIT" -> WalletType.DEPOSIT;
                                case "SAVING_ACCOUNT" -> WalletType.SAVING;
                                case "CREDIT" -> WalletType.CREDIT;
                                default -> throw new RuntimeException("Неизвестный тип накоплений: " + type);
                            })
                            .name(name)
                            .money(money)
                            .build();
                })
                .toList();

    }

    @SneakyThrows
    public synchronized void getHistory(YearMonth yearMonth) {
        // Получаем сырые данные
        LocalDate fromDateTime = yearMonth.atDay(1);
        LocalDate toDateTime = yearMonth.atEndOfMonth();
        List<Map<String, Object>> operations = getHistoryRaw(fromDateTime, toDateTime);
        
        // Сохраняем в файл
        writeMapsAsJsonLines(operations, Path.of("E:\\Данные").resolve(yearMonth + ".json"));
    }

    /**
     * Получить сырые транзакции из VTB API без сохранения в файл.
     * Используется в Temporal workflow.
     */
    @SneakyThrows
    public synchronized List<Map<String, Object>> getHistoryRaw(LocalDate fromDateTime, LocalDate toDateTime) {
        log.info("getHistoryRaw");
        String authToken = authService.getAuthToken();
        String userFingerprint = authService.getUserFingerprint();

        final String url = "https://online.vtb.ru/msa/api-gw/private/history-hub/history-hub-homer/v1/history/byUser" +
                "?dateFrom=" + fromDateTime.toString() + "T00:00:00.000%2B03:00&dateTo=" + toDateTime.toString() + "T23:59:59.999%2B03:00";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Cookie", "USER_FINGERPRINT=" + userFingerprint);
        headers.add("Authorization", "Bearer " + authToken);
        headers.add("referer", "https://online.vtb.ru/history");

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                URI.create(url),
                HttpMethod.GET,
                requestEntity,
                String.class
        );

        Map<String, Object> result = OBJECT_MAPPER.readValue(response.getBody(), new TypeReference<>() {
        });

        return (List<Map<String, Object>>) result.get("operations");
    }


    public static void writeMapsAsJsonLines(List<Map<String, Object>> data, Path filePath) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        try (BufferedWriter writer = Files.newBufferedWriter(filePath)) {
            for (Map<String, Object> map : data) {
                String jsonLine = mapper.writeValueAsString(map);
                writer.write(jsonLine);
                writer.newLine();
            }
        }
    }
}
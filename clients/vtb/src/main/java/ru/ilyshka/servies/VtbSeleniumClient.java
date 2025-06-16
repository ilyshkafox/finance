package ru.ilyshka.servies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import ru.ilyshka.configuration.VpbProperties;
import ru.ilyshka.dto.State;
import ru.ilyshka.dto.Wallet;
import ru.ilyshka.dto.WalletType;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VtbSeleniumClient implements AutoCloseable {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PAGE_LOGIN = "https://online.vtb.ru/login";
    private static final String PAGE_HOME = "https://online.vtb.ru/home";
    private static final String PAGE_HISTORY = "https://online.vtb.ru/history";
    private static final String PAGE_TRANSFERS = "https://online.vtb.ru/transfers";
    private static final String PAGE_FAVORITES = "https://online.vtb.ru/favorites";
    private static final String PAGE_AUTOPAYMENTS = "https://online.vtb.ru/autopayments";
    private final VpbProperties properties;
    private final NotifyService notifyService;
    private final RestTemplate restTemplate = new RestTemplate();
    private WebDriver driver;
    private Wait<WebDriver> wait;
    private WebStorage webStorage;

    @SneakyThrows
    @PostConstruct
    void init() {
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--no-sandbox");
        options.addArguments("disable-infobars");
        options.addArguments("--detach=true");

//        driver = new RemoteWebDriver(URI.create("http://localhost:4444/wd/hub").toURL(), options);
        driver = new ChromeDriver(options);
        driver.get("https://online.vtb.ru");
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        webStorage = (WebStorage) new Augmenter().augment(driver);
        checkActivity();
    }


    public synchronized void checkActivity() {
        try {
            switch (getState()) {
                case LOGIN_PHONE -> login();
                case LOGIN_CMC -> notifyService.notifyRequiredCmcCode();
                case LOGIN_CODE -> notifyService.notifyRequiredSecretCode();
                default -> {
                    checkCloseDialog();
                    checkContinueWorking();
                }
            }
        } catch (Exception e) {
            String url = driver.getCurrentUrl();
            String img = driver.findElement(By.xpath("\\")).getScreenshotAs(OutputType.BASE64);
            notifyService.notifyError(e.getMessage(), url, img);
        }
    }

    private void login() {
        if (getState() != State.LOGIN_PHONE) {
            log.warn("Не обнаружена страница входа");
            return;
        }
        sleep();
        log.info("Login phone: {}", properties.phone());
        WebElement main = driver.findElement(By.tagName("main"));
        WebElement input = main.findElement(By.tagName("input"));
        input.sendKeys(properties.phone());
        sleep();
        WebElement button = main.findElement(By.cssSelector("form"))
                .findElement(By.cssSelector("button[type=submit]"));
        button.click();

        sleep();
        if (getState() != State.LOGIN_PHONE) {
            checkActivity();
        }
    }

    private void checkCloseDialog() {
        log.info("Проверка на баннер диалог");
        try {
            driver.findElement(By.xpath("//div[@role='dialog']//button[@aria-label='Закрыть']"))
                    .click();
            log.info("Мы закрыли баннер.");
        } catch (Exception ignore) {
        }
    }

    private void checkContinueWorking() {
        log.info("Проверка на прерывание активности ");
        try {
            driver.findElement(By.xpath("//button[text() = 'Продолжить работу']"))
                    .click();
            sleep();
            log.info("Прожали 'Продолжить работу'");
        } catch (Exception ignore) {
        }
    }

    public void writeSmsCode(String code) {
        if (getState() != State.LOGIN_CMC) {
            log.warn("Не обнаружена страница ввода смс кода");
            return;
        }
        log.info("Write sms code: ******");
        WebElement main = driver.findElement(By.tagName("main"));
        WebElement otpInput = main.findElement(By.name("otpInput"));
        otpInput.sendKeys(code);

        sleep();
        checkActivity();
    }

    public void writeAccessCode(String code) {
        if (getState() != State.LOGIN_CODE) {
            log.warn("Не обнаружена страница ввода кода доступа");
            return;
        }
        log.info("Write access code: ****");
        WebElement main = driver.findElement(By.tagName("main"));
        WebElement otpInput = main.findElement(By.name("codeInput"));
        otpInput.sendKeys(code);

        sleep();
        checkActivity();
    }

    public State getState() {
        switch (driver.getCurrentUrl()) {
            case PAGE_LOGIN:
                String mainText = driver.findElement(By.tagName("main")).getText();
                if (mainText.contains("Введите номер телефона"))
                    return State.LOGIN_PHONE;
                if (mainText.contains("Введите СМС-код"))
                    return State.LOGIN_CMC;
                if (mainText.contains("Введите код доступа"))
                    return State.LOGIN_CODE;
                break;
            case PAGE_HOME:
                return State.PAGE_HOME;
            case PAGE_HISTORY:
                return State.PAGE_HISTORY;
            case PAGE_TRANSFERS:
                return State.PAGE_TRANSFERS;
            case null:
            default:
        }
        throw new RuntimeException("Неизвестная страница");
    }

    @Override
    public void close() throws Exception {
        driver.quit();
        driver.close();
    }


    @SneakyThrows
    private void sleep() {
        Thread.sleep(1000);
    }

    @SneakyThrows
    public synchronized List<Wallet> getWallets() {
        String authToken = getAuthToken();
        String userFingerprint = getUserFingerprint();

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
    public synchronized void getHistory(LocalDate from, LocalDate to) {
        String authToken = getAuthToken();
        String userFingerprint = getUserFingerprint();
        
        final String url = "https://online.vtb.ru/msa/api-gw/private/history-hub/history-hub-homer/v1/history/byUser" +
                "?dateFrom="+from.toString()+"T00:00:00&dateTo="+to.toString()+"T23:59:59";

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
    }


    private Map<String, String> readDataActiveElement() {
        WebElement webElement = driver.switchTo().activeElement();
        System.out.println();

        return Map.of();
    }


    @SneakyThrows
    private String getAuthToken() {
        if (!getState().isAuth()) throw new RuntimeException("Не пройдена авторизация");
        Map<String, String> auth = OBJECT_MAPPER.readValue(webStorage.getSessionStorage().getItem("@vtb/auth"), new TypeReference<>() {
        });
        return auth.get("idToken");
    }

    @SneakyThrows
    private String getUserFingerprint() {
        if (!getState().isAuth()) throw new RuntimeException("Не пройдена авторизация");
        return driver.manage().getCookieNamed("USER_FINGERPRINT").getValue();
    }


    private void openPage(State pageType) {
        switch (pageType) {
            case LOGIN_CMC, LOGIN_CODE, LOGIN_PHONE:
                throw new RuntimeException("Не возможно октрыть страницу " + pageType.name());
            case PAGE_HOME: {
                try {
                    driver.findElement(By.xpath("//a[@href='/home']")).click();
                } catch (Exception e) {
                    driver.get(PAGE_HOME);
                }
                waitFindElement(By.xpath("//p[starts-with(.,'Мастер-счет в рублях')]"));
                break;
            }
            case PAGE_HISTORY: {
                try {
                    driver.findElement(By.xpath("//a[@href='/history']")).click();
                } catch (Exception e) {
                    driver.get(PAGE_HISTORY);
                }
                waitFindElement(By.xpath("//h1[starts-with(.,'История')]"));
                break;
            }
            case PAGE_TRANSFERS: {
                try {
                    driver.findElement(By.xpath("//a[@href='/transfers']")).click();
                } catch (Exception e) {
                    driver.get(PAGE_TRANSFERS);
                }
                waitFindElement(By.xpath("//h1[starts-with(.,'Платежи')]"));
                break;
            }
        }

        sleep();
        checkActivity();
    }


    private void waitFindElement(By element) {
        wait.until(webDriver -> {
            try {
                driver.findElements(element);
                return true;
            } catch (NotFoundException ignore) {
                return false;
            }
        });
    }
}

package ru.ilyshka.servies;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import ru.ilyshka.configuration.VpbProperties;
import ru.ilyshka.dto.State;
import ru.ilyshka.dto.Wallet;
import ru.ilyshka.dto.WalletType;
import ru.ilyshka.utils.DateUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VtbSeleniumClient implements AutoCloseable {
    private static final String PAGE_LOGIN = "https://online.vtb.ru/login";
    private static final String PAGE_HOME = "https://online.vtb.ru/home";
    private static final String PAGE_HISTORY = "https://online.vtb.ru/history";
    private static final String PAGE_TRANSFERS = "https://online.vtb.ru/transfers";
    private static final String PAGE_FAVORITES = "https://online.vtb.ru/favorites";
    private static final String PAGE_AUTOPAYMENTS = "https://online.vtb.ru/autopayments";
    private final VpbProperties properties;
    private final NotifyService notifyService;
    private WebDriver driver;
    private Wait<WebDriver> wait;

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

    public synchronized List<Wallet> getWallets() {
        State state = getState();
        if (!state.isAuth()) {
            throw new RuntimeException("Не пройдена авторизация");
        }
        openPage(State.PAGE_HOME);

        List<Wallet> wallets = new ArrayList<>();
        WebElement masterElement = driver.findElement(By.xpath("//p[starts-with(.,'Мастер-счет в рублях')]/../../../../.."));
        String masterId = masterElement.getDomAttribute("data-id");
        WebElement element = masterElement.findElement(By.xpath("div/div[1]"));
        String masterName = element.findElement(By.xpath("div[2]/div/p[1]")).getText();
        String masterMoneyStr = element.findElement(By.xpath("div[3]")).getText()
                .replaceAll("[^0-9.,]", "")
                .replace(',', '.');
        wallets.add(Wallet.builder()
                .id(masterId)
                .type(WalletType.DEBIT)
                .name(masterName)
                .money(new BigDecimal(masterMoneyStr))
                .build()
        );

        List<WebElement> savings = driver.findElements(By.xpath("//p[starts-with(.,'Открыть счет или вклад')]/../../../div[1]/button"));
        savings.forEach(saving -> wallets.add(getDataWallet(WalletType.SAVING, saving)));

        List<WebElement> credits = driver.findElements(By.xpath("//p[starts-with(.,'Выбрать кредит')]/../../../div[1]/button"));
        credits.forEach(credit -> wallets.add(getDataWallet(WalletType.CREDIT, credit)));

        return wallets;
    }

    public synchronized void getHistory(OffsetDateTime from) {
        openPage(State.PAGE_HISTORY);
        WebElement body = driver.findElement(By.xpath("//body"));


        // Передвинуть активные элемент на первую дату
        while (true) {
            try {
                WebElement webElement = driver.switchTo().activeElement().findElement(By.xpath(".."));
                if (webElement.getDomAttribute("data-id") != null) {
                    break;
                }
                body.sendKeys(Keys.TAB);
            } catch (NotFoundException e) {
                body.sendKeys(Keys.TAB);
            }
        }

        WebElement webElement = driver.switchTo().activeElement();
        String text = webElement.getAttribute("innerHTML");
        text = text.substring(text.indexOf(">") + 1);
        text = text.substring(0, text.indexOf("<"));
        LocalDate firstDate = DateUtils.parseDate(text);





        body.sendKeys(Keys.TAB);
        // Мы находимя на первом элементе
        Map<String, String> stringStringMap = readDataActiveElement();


        System.out.println("");
    }


    private Map<String, String> readDataActiveElement() {
        WebElement webElement = driver.switchTo().activeElement();
        System.out.println();

        return Map.of();
    }


    private Wallet getDataWallet(WalletType type, WebElement element) {
        String id = element.getDomAttribute("data-id");
        WebElement detailElement = element.findElement(By.xpath("div[2]/div/div[1]/div"));
        String name = detailElement.findElement(By.xpath("div[1]/div[1]")).getText();
        String moneyStr = detailElement.findElement(By.xpath("div[2]/p")).getText()
                .replaceAll("[^0-9.,]", "")
                .replace(',', '.');

        return Wallet.builder()
                .id(id)
                .type(type)
                .name(name)
                .money(new BigDecimal(moneyStr))
                .build();
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

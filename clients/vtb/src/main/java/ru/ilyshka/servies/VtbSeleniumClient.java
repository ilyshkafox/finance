package ru.ilyshka.servies;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import ru.ilyshka.configuration.VpbProperties;
import ru.ilyshka.dto.State;
import ru.ilyshka.dto.Wallet;
import ru.ilyshka.dto.WalletType;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class VtbSeleniumClient implements AutoCloseable {
    private final VpbProperties properties;
    private WebDriver driver;
    private Wait<WebDriver> wait;

    @SneakyThrows
    @PostConstruct
    void init() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox");
        driver = new RemoteWebDriver(URI.create("http://localhost:4444/wd/hub").toURL(), options);
        driver.get("https://online.vtb.ru");
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        login();
    }

    public void login() {
        if (getState() != State.LOGIN_PHONE) {
            log.warn("Не обнаружена страница входа");
            return;
        }
        log.info("Login phone: {}", properties.phone());
        WebElement main = driver.findElement(By.tagName("main"));
        WebElement input = main.findElement(By.tagName("input"));
        input.sendKeys(properties.phone());
        sleep();
        WebElement button = main.findElement(By.cssSelector("form"))
                .findElement(By.cssSelector("button[type=submit]"));
        button.click();
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
    }


    public State getState() {
        switch (driver.getCurrentUrl()) {
            case "https://online.vtb.ru/login":
                String mainText = driver.findElement(By.tagName("main")).getText();
                if (mainText.contains("Введите номер телефона"))
                    return State.LOGIN_PHONE;
                if (mainText.contains("Введите СМС-код"))
                    return State.LOGIN_CMC;
                if (mainText.contains("Введите код доступа"))
                    return State.LOGIN_CODE;
                break;
            case "https://online.vtb.ru/home":
                return State.PAGE_HOME;
            case "https://online.vtb.ru/history":
                return State.PAGE_HISTORY;
            case "https://online.vtb.ru/transfers":
                return State.PAGE_TRANSFERS;
            case null:
            default:
                break;
        }
        throw new RuntimeException("Неизвестная страница");
    }


    @Override
    public void close() throws Exception {
        driver.quit();
        driver.close();
    }

    @SneakyThrows
    public void checkActivity() {
        log.info("Проверка на прерывание активности ");
        try {
            driver.findElement(By.xpath("//p[starts-with(.,'Продолжить работу')]"))
                    .click();
            Thread.sleep(1000);
            log.info("Прожали 'Продолжить работу'");
        } catch (Exception ignore) {
        }
    }


    @SneakyThrows
    private void sleep() {
        Thread.sleep(1000);
    }


    public void startActions() {
        List<Wallet> wallets = getWallets();
        wallets.forEach(System.out::println);
    }

    private List<Wallet> getWallets() {
        if (!Objects.equals(driver.getCurrentUrl(), "https://online.vtb.ru/home")) {
            driver.get("https://online.vtb.ru");
            wait.until(webDriver -> {
                try {
                    driver.findElements(By.xpath("//p[starts-with(.,'Мастер-счет в рублях')]"));
                    return true;
                } catch (NotFoundException ignore) {
                    return false;
                }
            });
        }
        List<Wallet> wallets = new ArrayList<>();

        WebElement masterElement = driver.findElement(By.xpath("//p[equals(.,'Мастер-счет в рублях')]/../../../../.."));
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

        List<WebElement> savings = driver.findElements(By.xpath("//p[equals(.,'Открыть счет или вклад')]/../../../div[1]/button"));
        savings.forEach(saving -> wallets.add(getDataWallet(WalletType.SAVING, saving)));

        List<WebElement> credits = driver.findElements(By.xpath("//p[equals(.,'Выбрать кредит')]/../../../div[1]/button"));
        credits.forEach(credit -> wallets.add(getDataWallet(WalletType.CREDIT, credit)));
        return wallets;
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

}

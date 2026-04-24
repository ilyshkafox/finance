package ru.ilyshka.servies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.Getter;
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
import org.springframework.stereotype.Service;
import ru.ilyshka.configuration.VpbProperties;
import ru.ilyshka.dto.State;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class VtbAuthService implements AutoCloseable {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String PAGE_LOGIN = "https://online.vtb.ru/login";
    private static final String PAGE_HOME = "https://online.vtb.ru/home";
    private static final int QR_TIMEOUT = 300000;
    private static final Pattern PNG_PATTERN = Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)");

    private final VpbProperties properties;
    private final NotifyService notifyService;
    private WebDriver driver;
    private Wait<WebDriver> wait;
    private WebStorage webStorage;
    private String qrCodeUrl;
    private String lastUrl;
    private String lastScreenshot;
    private final AtomicBoolean qrInProgress = new AtomicBoolean(false);
    
    @Getter
    private String savedAuthToken;
    
    @Getter
    private String savedUserFingerprint;

    private void initDriver() {
        log.info("[INIT] Запуск VTB аутентификации");
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--no-sandbox", "--disable-dev-shm-usage", "--disable-gpu",
                "--disable-webrtc", "--hide-scrollbars", "--disable-notifications",
                "disable-infobars", "--detach=true",
                "user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 YaBrowser/25.12.0.0 Safari/537.36");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        webStorage = (WebStorage) new Augmenter().augment(driver);
        log.info("[INIT] Браузер запущен, переход на vtb.ru");
        driver.get("https://online.vtb.ru");
        lastUrl = driver.getCurrentUrl();
        waitRootNotEmpty();
        log.info("[INIT] Страница загружена");
    }

    private void waitRootNotEmpty() {
        wait.until(d -> {
            WebElement el = d.findElement(By.id("root"));
            return !el.getText().trim().isEmpty() || !el.findElements(By.xpath("./*")).isEmpty() ? el : null;
        });
    }

    private String getScreenshot() {
        if (driver == null) return "N/A";
        return ((TakesScreenshot) new Augmenter().augment(driver)).getScreenshotAs(OutputType.BASE64);
    }

    public synchronized void checkActivity() {
        try {
            if (driver != null && getState() == State.LOGIN_QR) handleQrLogin();
        } catch (Exception e) {
            log.error("[CHECK] Ошибка проверки активности: {}", e.getMessage());
            notifyService.notifyError(e.getMessage(), lastUrl, lastScreenshot);
        }
    }

    public State getState() {
        // Если браузер закрыт после успешной авторизации — возвращаем AUTH
        if (driver == null) {
            return savedAuthToken != null ? State.AUTH : State.LOGIN_QR;
        }
        String fullUrl = driver.getCurrentUrl();
        int qIndex = fullUrl.indexOf('?');
        String url = qIndex >= 0 ? fullUrl.substring(0, qIndex) : fullUrl;
        if (url.endsWith("/")) url = url.substring(0, url.length() - 1);
        if (url.equals(PAGE_LOGIN) || driver.getCurrentUrl().contains("/login")) return State.LOGIN_QR;
        if (url.equals(PAGE_HOME) || driver.getCurrentUrl().contains("/home")) return State.PAGE_HOME;
        throw new RuntimeException("Unknown page: " + driver.getCurrentUrl());
    }

    @Override
    public void close() throws Exception {
        if (driver != null) {
            driver.quit();
            driver = null;
            wait = null;
            webStorage = null;
        }
    }

    @SneakyThrows
    public String getAuthToken() {
        // Если браузер закрыт (после QR авторизации), возвращаем сохраненный токен
        if (savedAuthToken != null) {
            return savedAuthToken;
        }
        // Иначе пытаемся получить из sessionStorage (когда браузер еще открыт)
        if (webStorage == null) {
            throw new IllegalStateException("WebStorage недоступен. Авторизация завершена, используйте savedAuthToken");
        }
        try {
            String authItem = webStorage.getSessionStorage().getItem("@vtb/auth");
            if (authItem == null) {
                log.warn("[AUTH] Токен не найден в sessionStorage");
                return null;
            }
            Map<String, String> auth = MAPPER.readValue(authItem, new TypeReference<>() {});
            return auth.get("idToken");
        } catch (Exception e) {
            log.error("[AUTH] Ошибка получения токена: {}", e.getMessage());
            return null;
        }
    }

    @SneakyThrows
    public String getUserFingerprint() {
        // Если браузер закрыт (после QR авторизации), возвращаем сохраненный fingerprint
        if (savedUserFingerprint != null) {
            return savedUserFingerprint;
        }
        // Иначе пытаемся получить из cookie (когда браузер еще открыт)
        if (driver == null) {
            throw new IllegalStateException("Driver недоступен. Авторизация завершена, используйте savedUserFingerprint");
        }
        try {
            return driver.manage().getCookieNamed("USER_FINGERPRINT").getValue();
        } catch (Exception e) {
            log.error("[FINGERPRINT] Ошибка получения: {}", e.getMessage());
            return null;
        }
    }

    // ==================== QR авторизация ====================

    /**
     * Начало QR авторизации. Запускает браузер и инициализирует процесс.
     * Возвращает QR код URL для отображения.
     */
    @SneakyThrows
    public String startAuthorization() {
        if (driver != null) {
            log.warn("[AUTH] Браузер уже запущен, завершаю предыдущий сеанс");
            driver.quit();
            driver = null;
        }
        initDriver();
        handleQrLogin();
        return qrCodeUrl;
    }

    @SneakyThrows
    private void startQrLogin() {
        log.info("[QR.1] Переход на страницу логина");
        driver.get(PAGE_LOGIN);
        lastUrl = driver.getCurrentUrl();
        waitRootNotEmpty();
        Thread.sleep(1000);

        log.info("[QR.2] Нажатие кнопки QR входа");
        try { driver.findElement(By.cssSelector("[data-test-id='auth-by-qr-button']")).click(); }
        catch (Exception e) { driver.findElement(By.xpath("//button[contains(text(), 'QR')]")).click(); }
        Thread.sleep(1000);

        log.info("[QR.3] Ожидание страницы QR-кода");
        wait.until(d -> { try { String t = d.findElement(By.tagName("main")).getText(); return t.contains("QR"); } catch (Exception e) { return false; } });
        log.info("[QR.3] Страница QR-кода загружена");

        log.info("[QR.4] Извлечение QR изображения");
        String qrBase64 = extractQrBase64();
        if (qrBase64 == null) { log.error("[QR.4] Не удалось извлечь QR"); return; }
        log.debug("[QR.4] QR изображение извлечено, размер: {} символов", qrBase64.length());

        log.info("[QR.5] Парсинг QR кода из SVG");
        String qrContent = parseQrFromSvg(qrBase64);
        if (qrContent == null) { log.error("[QR.5] Не удалось распарсить QR код"); return; }
        log.info("[QR.5] QR код успешно распарсен");

        log.info("[QR.6] QR CODE URL: {}", qrContent);
        notifyService.notifyQrCodeParsed(qrContent);
        qrCodeUrl = qrContent;
    }

    private String extractQrBase64() {
        try {
            WebElement img = wait.until(d -> {
                try { WebElement e = d.findElement(By.cssSelector("img[alt=\"QR-код\"]")); return e.isDisplayed() ? e : null; } catch (Exception ignore) {}
                try { WebElement e = d.findElement(By.cssSelector("img[src*=\"qr\"]")); return e.isDisplayed() ? e : null; } catch (Exception ignore) {}
                return null;
            });
            if (img != null) { String src = img.getAttribute("src"); return (src != null && src.startsWith("data:image")) ? src : getScreenshot(); }
        } catch (Exception e) { log.warn("[QR.4] Не удалось найти QR элемент: {}", e.getMessage()); }
        return null;
    }

    @SneakyThrows
    private String parseQrFromSvg(String qrBase64) {
        try {
            String b64 = qrBase64.contains(",") ? qrBase64.substring(qrBase64.indexOf(",") + 1) : qrBase64;
            byte[] svgBytes = Base64.getDecoder().decode(b64);
            log.debug("[QR.5] SVG размер: {} байт", svgBytes.length);
            
            String svg = new String(svgBytes, "UTF-8");
            Matcher m = PNG_PATTERN.matcher(svg);
            if (!m.find()) { log.error("[QR.5] PNG не найден в SVG"); return null; }
            log.debug("[QR.5] Embedded PNG найден, длина base64: {} символов", m.group(1).length());
            
            byte[] pngBytes = Base64.getDecoder().decode(m.group(1));
            log.debug("[QR.5] PNG декодирован: {} байт", pngBytes.length);
            
            BufferedImage png = ImageIO.read(new ByteArrayInputStream(pngBytes));
            if (png == null) { log.error("[QR.5] PNG не прочитан"); return null; }
            log.debug("[QR.5] PNG прочитан: {}x{}", png.getWidth(), png.getHeight());

            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(png)));
            return new MultiFormatReader().decode(bitmap).getText();
        } catch (Exception e) { log.error("[QR.5] Ошибка парсинга QR: {}", e.getMessage(), e); return null; }
    }

    @SneakyThrows
    public boolean waitForQrLogin() {
        log.info("[QR.WAIT] Ожидание сканирования QR кода (timeout: {} мс)", QR_TIMEOUT);
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < QR_TIMEOUT) {
            if (driver == null) {
                log.warn("[QR.WAIT] Браузер закрыт во время ожидания");
                return false;
            }
            String url = driver.getCurrentUrl();
            lastUrl = url;
            lastScreenshot = getScreenshot();
            if (!url.contains("/login") && !url.contains("/auth")) {
                log.info("[QR.WAIT] URL изменен - пользователь перешел по ссылке");
                
                // Получаем и сохраняем OAuth токен
                String token = extractAuthToken();
                if (token != null) {
                    savedAuthToken = token;
                    log.info("[AUTH] OAuth токен успешно получен и сохранен: {}...", token.substring(0, Math.min(20, token.length())));
                } else {
                    log.warn("[AUTH] Не удалось получить OAuth токен");
                }
                
                // Сохраняем fingerprint перед закрытием браузера
                try {
                    String fingerprint = driver.manage().getCookieNamed("USER_FINGERPRINT").getValue();
                    if (fingerprint != null) {
                        savedUserFingerprint = fingerprint;
                        log.info("[AUTH] User fingerprint сохранен: {}", fingerprint);
                    }
                } catch (Exception e) {
                    log.warn("[AUTH] Не удалось сохранить fingerprint: {}", e.getMessage());
                }
                
                // Закрываем браузер после успешной авторизации
                log.info("[AUTH] Закрытие браузера после успешной авторизации");
                close();
                
                qrInProgress.set(false);
                return true;
            }
            try { String t = driver.findElement(By.tagName("main")).getText(); if (t.contains("Добро пожаловать")) { log.info("[QR.WAIT] Успешный вход обнаружен"); qrInProgress.set(false); return true; } } catch (Exception ignore) {}
            Thread.sleep(2000);
        }
        log.warn("[QR.WAIT] Таймаут ожидания QR кода");
        qrInProgress.set(false);
        return false;
    }

    @SneakyThrows
    private String extractAuthToken() {
        if (webStorage == null) {
            log.warn("[AUTH] WebStorage недоступен (браузер закрыт)");
            return null;
        }
        try {
            Map<String, String> auth = MAPPER.readValue(webStorage.getSessionStorage().getItem("@vtb/auth"), new TypeReference<>() {});
            return auth.get("idToken");
        } catch (Exception e) {
            log.warn("[AUTH] Ошибка получения токена: {}", e.getMessage());
            return null;
        }
    }

    private void handleQrLogin() {
        if (!qrInProgress.get()) {
            qrInProgress.set(true);
            log.info("[QR.HANDLER] Запуск QR авторизации");
            try {
                startQrLogin();
                notifyService.notifyQrLoginRequired(qrCodeUrl);
                lastUrl = driver.getCurrentUrl();
                lastScreenshot = getScreenshot();
                new Thread(() -> {
                    try {
                        if (waitForQrLogin()) {
                            log.info("[QR.HANDLER] QR авторизация успешна!");
                            if (savedAuthToken != null) {
                                log.info("[QR.HANDLER] Сохраненный токен: {}...", savedAuthToken.substring(0, Math.min(20, savedAuthToken.length())));
                            }
                        } else {
                            log.warn("[QR.HANDLER] QR авторизация отменена");
                            cancelQrLogin();
                            notifyService.notifyQrLoginExpired();
                        }
                    } catch (Exception e) {
                        log.error("[QR.HANDLER] Ошибка QR авторизации: {}", e.getMessage());
                        cancelQrLogin();
                        notifyService.notifyError("QR failed: " + e.getMessage(), lastUrl, lastScreenshot);
                    } finally { qrInProgress.set(false); }
                }).start();
            } catch (Exception e) {
                log.error("[QR.HANDLER] Ошибка запуска QR: {}", e.getMessage());
                qrInProgress.set(false);
                cancelQrLogin();
            }
        }
    }

    @SneakyThrows
    public void cancelQrLogin() {
        log.info("[QR.CANCEL] Отмена QR авторизации");
        qrInProgress.set(false);
        qrCodeUrl = null;
        if (driver != null) {
            lastUrl = driver.getCurrentUrl();
            lastScreenshot = getScreenshot();
            driver.get(PAGE_LOGIN);
            waitRootNotEmpty();
        }
    }
}
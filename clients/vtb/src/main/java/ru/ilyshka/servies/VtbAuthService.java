package ru.ilyshka.servies;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.WebStorage;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Service;
import ru.ilyshka.configuration.VpbProperties;
import ru.ilyshka.dto.QrLoginData;
import ru.ilyshka.dto.State;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
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
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String PAGE_LOGIN = "https://online.vtb.ru/login";
    private static final String PAGE_HOME = "https://online.vtb.ru/home";
    
    // Константы авторизации по QR-коду
    private static final int QR_MAX_WAIT_MS = 300000; // 5 минут
    private static final int PAGE_LOAD_CHECK_INTERVAL = 2000; // 2 секунды между проверками загрузки
    // Regex для извлечения embedded PNG из SVG
    private static final Pattern PNG_FROM_SVG_PATTERN = Pattern.compile("data:image/png;base64,([A-Za-z0-9+/=]+)");

    private final VpbProperties properties;
    private final NotifyService notifyService;

    private WebDriver driver;
    private Wait<WebDriver> wait;
    private WebStorage webStorage;
    
    // URL из QR-кода для ожидания авторизации
    private String qrAuthUrl;
    private AtomicBoolean qrLoginInProgress;

    @SneakyThrows
    @PostConstruct
    public void init() {
        qrLoginInProgress = new AtomicBoolean(false);
        
        ChromeOptions options = new ChromeOptions();

        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--disable-webrtc");
        options.addArguments("--hide-scrollbars");
        options.addArguments("--disable-notifications");
        options.addArguments("disable-infobars");
        options.addArguments("--detach=true");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 YaBrowser/25.12.0.0 Safari/537.36");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
        webStorage = (WebStorage) new Augmenter().augment(driver);
        driver.get("https://online.vtb.ru");
        waitRootNotEmpty();
        handleQrLogin();
    }

    public void waitRootNotEmpty() {
        wait.until(d -> {
            log.debug("Ожидание пока элемент root будет заполнен");
            WebElement element = d.findElement(By.id("root"));
            String text = element.getText();
            if (!text.trim().isEmpty()) {
                return element;
            }
            if (!element.findElements(By.xpath("./*")).isEmpty()) {
                return element;
            }
            return null;
        });
        log.debug("Root элемент не пустой");
    }

    private String getScreenshotBase64() {
        TakesScreenshot takesScreenshot = (TakesScreenshot) new Augmenter().augment(driver);
        return takesScreenshot.getScreenshotAs(OutputType.BASE64);
    }

    public synchronized void checkActivity() {
        try {
            switch (getState()) {
                case LOGIN_QR -> handleQrLogin();
            }
        } catch (Exception e) {
            String url = driver.getCurrentUrl();
            String img = getScreenshotBase64();
            notifyService.notifyError(e.getMessage(), url, img);
        }
    }

    public State getState() {
        String currentUrl = driver.getCurrentUrl();
        if (currentUrl == null) {
            throw new RuntimeException("Неизвестная страница: null URL");
        }
        
        String normalizedUrl = currentUrl.split("\\?")[0];
        if (normalizedUrl.endsWith("/")) {
            normalizedUrl = normalizedUrl.substring(0, normalizedUrl.length() - 1);
        }
        
        if (normalizedUrl.equals(PAGE_LOGIN) || currentUrl.contains("/login")) {
            try {
                String mainText = driver.findElement(By.tagName("main")).getText();
                if (mainText.contains("Отсканируйте") || mainText.contains("QR") || mainText.contains("qr"))
                    return State.LOGIN_QR;
            } catch (Exception e) {
                log.debug("Не удалось получить текст main элемента: {}", e.getMessage());
            }
            return State.LOGIN_QR;
        }
        
        if (normalizedUrl.equals(PAGE_HOME) || currentUrl.contains("/home"))
            return State.PAGE_HOME;
        
        throw new RuntimeException("Неизвестная страница: " + currentUrl);
    }

    @Override
    public void close() throws Exception {
        if (driver != null) {
            driver.quit();
            driver = null;
        }
    }

    @SneakyThrows
    private void sleep() {
        Thread.sleep(1000);
    }

    @SneakyThrows
    public String getAuthToken() {
        if (!getState().isAuth()) throw new RuntimeException("Не пройдена авторизация");
        TypeReference<Map<String, String>> ref = new TypeReference<>() {};
        Map<String, String> auth = OBJECT_MAPPER.readValue(
                webStorage.getSessionStorage().getItem("@vtb/auth"), ref);
        return auth.get("idToken");
    }

    @SneakyThrows
    public String getUserFingerprint() {
        if (!getState().isAuth()) throw new RuntimeException("Не пройдена авторизация");
        return driver.manage().getCookieNamed("USER_FINGERPRINT").getValue();
    }
    
    @SneakyThrows
    public QrLoginData startQrLogin() {
        log.info("Начало QR авторизации");
        
        driver.get(PAGE_LOGIN);
        waitRootNotEmpty();
        sleep();
        
        try {
            WebElement qrButton = driver.findElement(By.cssSelector("[data-test-id='auth-by-qr-button']"));
            qrButton.click();
            log.info("Нажата кнопка QR входа");
        } catch (Exception e) {
            log.warn("Не удалось найти кнопку QR, пробуем альтернативный способ");
            try {
                WebElement qrButton = driver.findElement(By.xpath("//button[contains(text(), 'QR')]"));
                qrButton.click();
                log.info("Нажата кнопка QR входа (по тексту)");
            } catch (Exception ex) {
                throw new RuntimeException("Кнопка QR не найдена, возможно страница уже показывает QR");
            }
        }
        sleep();
        
        wait.until(d -> {
            try {
                WebElement main = d.findElement(By.tagName("main"));
                String text = main.getText();
                return text.contains("QR-код") || text.contains("Войти по QR") || text.contains("Отсканируйте");
            } catch (Exception e) {
                return false;
            }
        });
        log.info("Страница QR-кода загружена");
        
        String qrBase64 = null;
        try {
            WebElement qrImageElement = wait.until(d -> {
                try {
                    WebElement img = d.findElement(By.cssSelector("img[alt=\"QR-код\"]"));
                    if (img.isDisplayed()) return img;
                } catch (Exception ignore) {}
                
                try {
                    WebElement img = d.findElement(By.cssSelector("img[src*=\"qr\"], img[src*=\"QR\"]"));
                    if (img.isDisplayed()) return img;
                } catch (Exception ignore) {}
                
                return null;
            });
            
            if (qrImageElement != null) {
                String qrSrc = qrImageElement.getAttribute("src");
                
                if (qrSrc != null && !qrSrc.isEmpty() && qrSrc.startsWith("data:image")) {
                    qrBase64 = qrSrc;
                    log.info("Извлечено base64 изображение QR-кода из src атрибута");
                } else {
                    TakesScreenshot takesScreenshot = (TakesScreenshot) new Augmenter().augment(driver);
                    qrBase64 = takesScreenshot.getScreenshotAs(OutputType.BASE64);
                    log.warn("Data URL не найден, используем скриншот страницы");
                }
            }
        } catch (Exception e) {
            log.warn("Не удалось найти элемент QR-кода: {}", e.getMessage());
        }
        
        if (qrBase64 == null || qrBase64.isEmpty()) {
            log.error("Не удалось извлечь base64 изображение QR-кода");
            return new QrLoginData(null, null, null);
        }
        log.info("Извлечено base64 изображение QR-кода");
        
        String qrContent = parseQrCodeFromBase64(qrBase64);
        
        if (qrContent == null || qrContent.isEmpty()) {
            log.warn("Не удалось распарсить QR код из изображения");
            return new QrLoginData(null, null, qrBase64);
        }
        log.info("QR код успешно распарсен: {}", qrContent);
        
        notifyService.notifyQrCodeParsed(qrContent);
        
        qrAuthUrl = qrContent;
        log.info("🔗 QR CODE URL (ссылка для авторизации): {}", qrAuthUrl);
        
        waitForHomePage();
        
        log.info("QR авторизация инициализирована, ожидаем переход на главную страницу");
        
        return new QrLoginData(null, null, qrContent);
    }
    
    @SneakyThrows
    public boolean waitForQrLogin() {
        log.info("Ожидание авторизации через QR (URL: {})", qrAuthUrl);
        log.info("Ожидание сканирования QR кода (timeout: {} ms)", QR_MAX_WAIT_MS);
        qrLoginInProgress.set(true);
        
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < QR_MAX_WAIT_MS) {
            try {
                String currentUrl = driver.getCurrentUrl();
                
                if (!currentUrl.contains("/login") && !currentUrl.contains("/auth")) {
                    log.info("Обнаружена смена URL - пользователь авторизуется");
                    sleep();
                    waitRootNotEmpty();
                    qrLoginInProgress.set(false);
                    return true;
                }
                
                try {
                    WebElement main = driver.findElement(By.tagName("main"));
                    String mainText = main.getText();
                    if (mainText.contains("Добро пожаловать") || mainText.contains("Личный кабинет")) {
                        log.info("Обнаружено сообщение о успешном входе");
                        qrLoginInProgress.set(false);
                        return true;
                    }
                } catch (Exception e) {
                    log.debug("Не удалось проверить main элемент: {}", e.getMessage());
                }
            } catch (Exception e) {
                log.debug("Ошибка проверки статуса QR: {}", e.getMessage());
            }
            Thread.sleep(PAGE_LOAD_CHECK_INTERVAL);
        }
        
        log.warn("Таймаут ожидания QR кода");
        qrLoginInProgress.set(false);
        return false;
    }
    
    private void handleQrLogin() {
        if (!qrLoginInProgress.get()) {
            qrLoginInProgress.set(true);
            log.info("Запуск QR авторизации из checkActivity");
            try {
                QrLoginData qrData = startQrLogin();
                notifyService.notifyQrLoginRequired(qrData.getQrCodeData());
                
                new Thread(() -> {
                    try {
                        if (waitForQrLogin()) {
                            log.info("QR авторизация успешна!");
                        } else {
                            cancelQrLogin();
                            notifyService.notifyQrLoginExpired();
                        }
                    } catch (Exception e) {
                        log.error("Ошибка QR авторизации: {}", e.getMessage());
                        cancelQrLogin();
                        notifyService.notifyError("QR авторизация failed: " + e.getMessage(), 
                                driver.getCurrentUrl(), 
                                getScreenshotBase64());
                    } finally {
                        qrLoginInProgress.set(false);
                    }
                }).start();
            } catch (Exception e) {
                log.error("Ошибка инициализации QR: {}", e.getMessage());
                qrLoginInProgress.set(false);
                cancelQrLogin();
            }
        }
    }
    
    @SneakyThrows
    public void cancelQrLogin() {
        log.info("Отмена QR авторизации");
        qrLoginInProgress.set(false);
        qrAuthUrl = null;
        driver.get(PAGE_LOGIN);
        waitRootNotEmpty();
    }
    
    private void openPage(State pageType) {
        if (pageType == State.PAGE_HOME) {
            try {
                driver.findElement(By.xpath("//a[@href='/home']")).click();
            } catch (Exception e) {
                driver.get(PAGE_HOME);
            }
            waitFindElement(By.xpath("//p[starts-with(.,'Мастер-счет в рублях')]"));
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
    
    @SneakyThrows
    private String parseQrCodeFromBase64(String base64Image) {
        try {
            String mimeType = "image/png";
            String base64Data = base64Image;
            if (base64Image.contains(",")) {
                String header = base64Image.substring(0, base64Image.indexOf(","));
                if (header.contains("svg")) {
                    mimeType = "image/svg+xml";
                } else if (header.contains("jpeg") || header.contains("jpg")) {
                    mimeType = "image/jpeg";
                } else if (header.contains("png")) {
                    mimeType = "image/png";
                }
                base64Data = base64Image.substring(base64Image.indexOf(",") + 1);
            }
            
            log.debug("Формат QR изображения: {}, длина base64: {} символов", mimeType, base64Data.length());
            
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            log.debug("Декодировано {} байт", imageBytes.length);
            log.info("🔍 QR PARSE DEBUG: mimeType={}, байт={}, первые 100 символов: {}", 
                mimeType, imageBytes.length, 
                imageBytes.length > 100 ? new String(imageBytes, 0, 100, "UTF-8") : new String(imageBytes, "UTF-8"));
            
            BufferedImage bufferedImage;
            
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            bufferedImage = ImageIO.read(inputStream);
            
            if (bufferedImage == null) {
                log.warn("Не удалось прочитать изображение через ImageIO, пробуем альтернативный способ");
                log.warn("Детали: длина байт={}, mimeType={}, первые 500 байт: {}", 
                    imageBytes.length, mimeType, 
                    imageBytes.length > 500 ? new String(imageBytes, 0, 500, "UTF-8") : new String(imageBytes, "UTF-8"));
                
                if (mimeType.equals("image/svg+xml")) {
                    log.info("Обнаружен SVG формат, пытаемся извлечь embedded PNG");
                    log.info("SVG содержимое (первые 1000 символов): {}", 
                        new String(imageBytes, "UTF-8").length() > 1000 ? 
                            new String(imageBytes, 0, 1000, "UTF-8") : 
                            new String(imageBytes, "UTF-8"));
                    
                    bufferedImage = extractPngFromSvg(imageBytes);
                    
                    if (bufferedImage == null) {
                        log.warn("Не удалось извлечь PNG из SVG, пробуем Batik конвертацию");
                        bufferedImage = convertSvgToBufferedImage(imageBytes);
                    }
                }
                
                if (bufferedImage == null) {
                    log.error("Не удалось конвертировать изображение в BufferedImage");
                    return null;
                }
            }
            
            log.debug("BufferedImage создано успешно: width={}, height={}, type={}", 
                bufferedImage.getWidth(), bufferedImage.getHeight(), bufferedImage.getType());
            
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(bufferedImage)
            ));
            
            Result result = new MultiFormatReader().decode(bitmap);
            return result.getText();
            
        } catch (Exception e) {
            log.error("Ошибка при парсинге QR кода: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Извлекает embedded PNG изображение из SVG.
     * SVG VTB содержит встроенный PNG в формате: xlink:href="data:image/png;base64,..."
     */
    @SneakyThrows
    private BufferedImage extractPngFromSvg(byte[] svgBytes) {
        try {
            String svgContent = new String(svgBytes, "UTF-8");
            log.info("🔍 Пытаемся извлечь embedded PNG из SVG (длина={}, {} символов)", svgContent.length(), svgContent.contains("data:image/png;base64,"));
            
            Matcher matcher = PNG_FROM_SVG_PATTERN.matcher(svgContent);
            if (matcher.find()) {
                String pngBase64 = matcher.group(1);
                log.info("🔍 Найдено embedded PNG в SVG, длина base64: {} символов", pngBase64.length());
                
                byte[] pngBytes = Base64.getDecoder().decode(pngBase64);
                log.info("🔍 Декодировано PNG: {} байт", pngBytes.length);
                
                BufferedImage pngImage = ImageIO.read(new ByteArrayInputStream(pngBytes));
                log.info("🔍 PNG успешно прочитан: width={}, height={}", 
                    pngImage != null ? pngImage.getWidth() : 0,
                    pngImage != null ? pngImage.getHeight() : 0);
                return pngImage;
            }
            
            log.warn("🔍 Embedded PNG не найден в SVG");
            return null;
        } catch (Exception e) {
            log.error("❌ Ошибка извлечения PNG из SVG: {}", e.getMessage(), e);
            return null;
        }
    }
    
    @SneakyThrows
    private BufferedImage convertSvgToBufferedImage(byte[] svgBytes) {
        log.error("❌ Batik не поддерживается, xerces недоступен");
        return null;
    }
    
    @SneakyThrows
    private void waitForHomePage() {
        try {
            WebDriverWait homeWait = new WebDriverWait(driver, Duration.ofSeconds(60));
            homeWait.until(ExpectedConditions.urlContains("/home"));
            log.info("Браузер перешел на главную страницу");
        } catch (Exception e) {
            log.warn("Не удалось дождаться перехода на главную страницу в течение 60 секунд");
        }
    }
}
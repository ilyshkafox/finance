package ru.ilyshka.libs.messages;

import io.appium.java_client.android.Activity;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceEventServiceTest {
    private AndroidDriver driver;

    @BeforeEach
    public void setUp() throws Exception {
        // 1. Подготовка параметров подключения
        UiAutomator2Options options = new UiAutomator2Options()
                .setPlatformName("Android")
                .setAutomationName("UiAutomator2");

        // 2. (Опционально) Указываем UDID устройства, если нужно подключиться к конкретному
        // options.setUdid("emulator-5554");

        // 3. Создание сессии и подключение к первому доступному устройству
        // Appium сервер сам определит первое устройство из списка adb devices
        driver = new AndroidDriver(new URL("http://localhost:4723"), options);
    }

    @Test
    public void launchSpecificApp() {
        String targetPackage = "com.example.targetapp";
        String targetActivity = ".MainActivity"; // или полное имя

        // 1. Проверяем, установлено ли приложение
        boolean isInstalled = driver.isAppInstalled(targetPackage);
        assertTrue(isInstalled, "Приложение " + targetPackage + " не установлено на устройстве");

        // 2. Запускаем приложение по имени пакета и Activity
        Activity appActivity = new Activity(targetPackage, targetActivity);
        driver.startActivity(appActivity);

        // 3. Дополнительная проверка: убеждаемся, что запустилось нужное приложение
        String currentPackage = driver.getCurrentPackage();
        assertEquals(targetPackage, currentPackage,
                "Запустилось другое приложение: " + currentPackage);
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}
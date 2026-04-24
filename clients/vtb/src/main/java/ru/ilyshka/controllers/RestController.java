package ru.ilyshka.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.ilyshka.dto.AccessCodeRequest;
import ru.ilyshka.dto.QrLoginData;
import ru.ilyshka.dto.SmsCodeRequest;
import ru.ilyshka.servies.VtbAuthService;
import ru.ilyshka.servies.VtbService;

@Slf4j
@RequiredArgsConstructor
@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api")
public class RestController {
    private final VtbAuthService authService;
    private final VtbService service;

    @PostMapping("/action")
    public void action() {
        service.startActions();
    }
    
    @PostMapping("/qr/start")
    public QrLoginData startQrLogin() {
        log.info("Получен запрос на запуск QR авторизации");
        try {
            QrLoginData qrData = authService.startQrLogin();
            log.info("QR авторизация запущена успешно");
            return qrData;
        } catch (Exception e) {
            log.error("Ошибка запуска QR авторизации: {}", e.getMessage(), e);
            throw new RuntimeException("Ошибка запуска QR авторизации: " + e.getMessage(), e);
        }
    }
    
    @PostMapping("/qr/cancel")
    public void cancelQrLogin() {
        log.info("Получен запрос на отмену QR авторизации");
        try {
            authService.cancelQrLogin();
            log.info("QR авторизация отменена");
        } catch (Exception e) {
            log.error("Ошибка отмены QR авторизации: {}", e.getMessage(), e);
        }
    }
}

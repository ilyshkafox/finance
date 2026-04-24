package ru.ilyshka.servies;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotifyService {
    public void notifyRequiredCmcCode() {
        log.debug("Требуется смс код");
    }

    public void notifyRequiredSecretCode() {
        log.debug("Требуется код авторизации");
    }

    public void notifyError(String message, String url, String img) {
        log.debug(url + " - " + message);
    }
    
    // ==================== QR Code Notifications ====================
    
    /**
     * Notifies that QR code login is required.
     * @param qrCodeData The QR code data to display to the user.
     */
    public void notifyQrLoginRequired(String qrCodeData) {
        log.info("Требуется авторизация по QR коду");
        if (qrCodeData != null) {
            log.info("QR данные доступны для отображения");
        }
    }
    
    /**
     * Notifies that QR code login session has expired.
     */
    public void notifyQrLoginExpired() {
        log.warn("Сессия QR авторизации истекла");
    }
    
    /**
     * Notifies that QR code has been scanned successfully.
     */
    public void notifyQrScanned() {
        log.info("QR код отсканирован, ожидание подтверждения");
    }
    
    /**
     * Notifies that QR login was successful.
     */
    public void notifyQrLoginSuccess() {
        log.info("QR авторизация успешна");
    }
    
    /**
     * Sends the parsed QR code content to events for processing.
     * @param qrContent The decoded content from the QR code.
     */
    public void notifyQrCodeParsed(String qrContent) {
        log.info("QR код успешно распарсен, отправка данных в события: {}", qrContent);
        if (qrContent != null && !qrContent.isEmpty()) {
            // Здесь будет логика отправки qrContent в систему событий/обработки
            // Например: отправка в Kafka, HTTP-запрос, или сохранение в очередь
            log.info("Данные QR кода переданы в систему обработки");
        }
    }
}

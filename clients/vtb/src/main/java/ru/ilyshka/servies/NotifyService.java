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
}

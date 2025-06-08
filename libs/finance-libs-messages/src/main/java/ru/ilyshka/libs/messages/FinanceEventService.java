package ru.ilyshka.libs.messages;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;
import ru.ilyshka.libs.messages.configuration.MessagesProperties;
import ru.ilyshka.libs.messages.dto.HealthCheckMessage;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinanceEventService {
    private final MessagesProperties properties;
    private final MessageChannel healthCheckChannel;
    private final MessageChannel walletInfoChannel;
    private final MessageChannel operationChannel;

    public void pushHatchCheck(HealthCheckMessage.Status status, String message) {
        long currentTimestamp = System.currentTimeMillis();
        log.info("Send event: {}, {}", status, message);
        healthCheckChannel.send(new GenericMessage<>(
                new HealthCheckMessage(currentTimestamp, status, message),
                Map.of(
                        "messageId", currentTimestamp,
                        "clientId", properties.clientId()
                )
        ));
    }
}

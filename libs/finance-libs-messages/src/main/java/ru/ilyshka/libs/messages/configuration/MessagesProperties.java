package ru.ilyshka.libs.messages.configuration;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.message")
public record MessagesProperties(
        @NotEmpty String clientId
) {
}

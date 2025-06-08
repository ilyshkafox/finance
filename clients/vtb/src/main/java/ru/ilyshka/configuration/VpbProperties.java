package ru.ilyshka.configuration;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record VpbProperties(
        @NotNull @NotEmpty @Length(min = 10, max = 10) String phone
) {
}

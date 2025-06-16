package ru.ilyshka.core;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@SpringBootApplication(scanBasePackages = "ru.ilyshka")
@ConfigurationPropertiesScan
@RequiredArgsConstructor
@EnableScheduling
@EnableWebMvc
public class FinanceCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceCoreApplication.class, args);

    }
}
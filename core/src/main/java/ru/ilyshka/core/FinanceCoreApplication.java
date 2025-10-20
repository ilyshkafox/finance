package ru.ilyshka.core;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@SpringBootApplication(scanBasePackages = "ru.ilyshka")
@ConfigurationPropertiesScan
@RequiredArgsConstructor
@EnableScheduling
@EnableWebMvc
@EnableTransactionManagement
@EnableJpaRepositories
public class FinanceCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(FinanceCoreApplication.class, args);

    }
}
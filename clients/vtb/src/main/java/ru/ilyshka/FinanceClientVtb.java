package ru.ilyshka;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "ru.ilyshka")
@ConfigurationPropertiesScan
@RequiredArgsConstructor
@EnableScheduling
public class FinanceClientVtb {
    public static void main(String[] args) {
        SpringApplication.run(FinanceClientVtb.class, args);
    }
}
// button  Продолжить работу

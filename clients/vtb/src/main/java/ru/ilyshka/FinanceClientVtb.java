package ru.ilyshka;


import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@RequiredArgsConstructor
public class FinanceClientVtb {
    public static void main(String[] args) {
        SpringApplication.run(FinanceClientVtb.class, args);
    }
}
// button  Продолжить работу

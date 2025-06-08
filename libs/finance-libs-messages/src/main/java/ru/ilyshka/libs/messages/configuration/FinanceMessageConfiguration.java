package ru.ilyshka.libs.messages.configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

@EnableConfigurationProperties(MessagesProperties.class)
@Configuration(proxyBeanMethods = false)
@EnableIntegration
public class FinanceMessageConfiguration {
    @Bean
    public MessageChannel healthCheckChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel walletInfoChannel() {
        return new PublishSubscribeChannel();
    }

    @Bean
    public MessageChannel operationChannel() {
        return new PublishSubscribeChannel();
    }
}

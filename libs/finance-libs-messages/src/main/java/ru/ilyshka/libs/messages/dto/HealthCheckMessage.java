package ru.ilyshka.libs.messages.dto;

public record HealthCheckMessage(
        long timestamp,
        Status status,
        String message
) {

    public enum Status {
        OK, WARNING, ERROR
    }
}

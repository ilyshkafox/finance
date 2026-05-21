package ru.ilyshka.controllers.dto;

import java.time.LocalDate;

public record GetReceipt(
        LocalDate from,
        LocalDate to
) {

}

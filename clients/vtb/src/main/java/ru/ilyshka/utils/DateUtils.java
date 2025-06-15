package ru.ilyshka.utils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public class DateUtils {

    private static final Locale RUSSIAN = new Locale("ru");
    private static final DateTimeFormatter FORMATTER_FULL =
            DateTimeFormatter.ofPattern("d MMMM yyyy 'года'", RUSSIAN);
    private static final DateTimeFormatter FORMATTER_WITHOUT_YEAR =
            DateTimeFormatter.ofPattern("d MMMM", RUSSIAN);

    public static LocalDate parseDate(String input) {
        if (input.startsWith("Вчера, ") || input.startsWith("Сегодня, ")) {
            input = input.substring(input.indexOf(",") + 1).trim();
        }

        // Удаление возможных запятых в начале
        String cleaned = input.startsWith(",")
                ? input.substring(1).trim()
                : input;

        // Парсинг абсолютных дат
        try {
            // Попытка парсинга с годом
            return LocalDate.parse(cleaned, FORMATTER_FULL);
        } catch (DateTimeParseException e) {
            // Парсинг без года + установка текущего года
            LocalDate date = LocalDate.parse(cleaned, FORMATTER_WITHOUT_YEAR);
            return date.withYear(LocalDate.now().getYear());
        }
    }
}

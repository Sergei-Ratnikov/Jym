package com.example.jym.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Утилитный класс для работы с датами.
 *
 * Все методы — статические, поэтому создавать объект не нужно:
 *   DateUtils.formatDate(...)  ← сразу так
 *   не так: new DateUtils().formatDate(...)
 *
 * Locale.getDefault() — берёт язык и регион с телефона пользователя.
 * Для России это будет русский формат даты.
 */
public class DateUtils {

    /**
     * Формат для даты: "11.09.2026"
     */
    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());

    /**
     * Формат для даты и времени: "11.09.2026 18:30"
     */
    private static final SimpleDateFormat DATE_TIME_FORMAT =
            new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

    /**
     * Формат только для времени: "18:30"
     */
    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.getDefault());

    /**
     * Формат для сохранения даты в базу данных: "2026-09-11".
     * Такой формат удобен для сортировки и поиска (лексикографический порядок
     * совпадает с хронологическим).
     */
    private static final SimpleDateFormat DB_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    /**
     * Превращает timestamp (миллисекунды) в строку "11.09.2026".
     */
    public static String formatDate(long timestamp) {
        return DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Превращает timestamp в строку "11.09.2026 18:30".
     */
    public static String formatDateTime(long timestamp) {
        return DATE_TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Превращает timestamp в строку "18:30".
     */
    public static String formatTime(long timestamp) {
        return TIME_FORMAT.format(new Date(timestamp));
    }

    /**
     * Возвращает дату в формате "2026-09-11" для сохранения в БД.
     */
    public static String toDbDate(long timestamp) {
        return DB_DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Возвращает сегодняшнюю дату в формате "2026-09-11".
     */
    public static String todayDbDate() {
        return DB_DATE_FORMAT.format(new Date());
    }

    /**
     * Считает, сколько дней прошло между двумя timestamp.
     *
     * Как работает:
     *  - Берём разницу в миллисекундах.
     *  - Делим на 1000 (секунды), 60 (минуты), 60 (часы), 24 (дни).
     *  - Округляем вниз (полные дни).
     *
     * Пример: если разница 25 часов — вернёт 1 (один полный день).
     */
    public static int daysBetween(long fromTimestamp, long toTimestamp) {
        long diffMillis = toTimestamp - fromTimestamp;
        return (int) (diffMillis / (1000L * 60 * 60 * 24));
    }

    /**
     * Считает, сколько дней назад был указанный момент.
     * Если сегодня — вернёт 0.
     */
    public static int daysAgo(long timestamp) {
        return daysBetween(timestamp, System.currentTimeMillis());
    }

    /**
     * Формирует читаемую строку "X дней назад" по timestamp.
     *
     * Правила русского языка:
     *  - 1 день → "1 день назад"
     *  - 2, 3, 4 → "2 дня назад"
     *  - 5...20 → "5 дней назад"
     *  - 21 → "21 день назад", 22 → "22 дня назад" и т.д.
     */
    public static String getDaysAgoText(long timestamp) {
        int days = daysAgo(timestamp);

        if (days == 0) {
            return "сегодня";
        }
        if (days == 1) {
            return "1 день назад";
        }

        // Определяем правильное окончание.
        // Формула: смотрим последнюю цифру и последние две цифры.
        int lastDigit = days % 10;
        int lastTwoDigits = days % 100;

        // Исключение: 11-14 — всегда "дней" (даже если последняя цифра 1-4).
        if (lastTwoDigits >= 11 && lastTwoDigits <= 14) {
            return days + " дней назад";
        }
        if (lastDigit == 1) {
            return days + " день назад";
        }
        if (lastDigit >= 2 && lastDigit <= 4) {
            return days + " дня назад";
        }
        return days + " дней назад";
    }

    /**
     * Форматирует длительность тренировки в читаемый вид.
     *
     * Примеры:
     *  - 45 минут → "45 мин"
     *  - 1 час 20 минут → "1 ч 20 мин"
     *  - 2 часа → "2 ч"
     */
    public static String formatDuration(long startTime, long endTime) {
        long diffMillis = endTime - startTime;
        long totalMinutes = diffMillis / (1000L * 60);

        if (totalMinutes < 60) {
            return totalMinutes + " мин";
        }

        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;

        if (minutes == 0) {
            return hours + " ч";
        }
        return hours + " ч " + minutes + " мин";
    }
}
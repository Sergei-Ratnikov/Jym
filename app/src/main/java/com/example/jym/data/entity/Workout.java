package com.example.jym.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity-класс для таблицы "workouts" (завершённые тренировки).
 * Каждая строка = одна завершённая тренировка.
 * ВАЖНО: тренировка НЕ хранит привязку к сету.
 * Сет — это только шаблон. Когда тренировка завершена, мы записываем
 * только список выполненных упражнений (в другой таблице).
 */
@Entity(tableName = "workouts")
public class Workout {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Время начала тренировки в миллисекундах (timestamp).
     * Например, 1731234567890 — это конкретный момент времени.
     * System.currentTimeMillis() возвращает такое число.
     */
    @ColumnInfo(name = "start_time")
    public long startTime;

    /**
     * Время окончания тренировки в миллисекундах.
     */
    @ColumnInfo(name = "end_time")
    public long endTime;

    /**
     * Дата тренировки в удобном формате, например "2026-09-11".
     * Нужна для быстрого поиска (быстрее, чем парсить timestamp).
     */
    @ColumnInfo(name = "date")
    public String date;

    public Workout() {
    }

    public Workout(long startTime, long endTime, String date) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.date = date;
    }
}
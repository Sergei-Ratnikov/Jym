package com.example.jym.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity-класс для таблицы "exercises" (упражнения).
 *
 * Каждое поле этого класса = колонка в таблице базы данных.
 * Каждый объект этого класса = одна строка в таблице.
 *
 * Например, строка "Жим лежа", тип 1, не в архиве — это один объект Exercise.
 */
@Entity(tableName = "exercises")
public class Exercise {

    /**
     * Уникальный идентификатор упражнения.
     * @PrimaryKey(autoGenerate = true) — Room автоматически выдаст уникальный номер
     * при добавлении (1, 2, 3, ...). Нам не нужно задавать его вручную.
     */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Название упражнения, например "Жим лежа".
     * Уникальное — в приложении не может быть двух упражнений с одинаковым названием.
     */
    @ColumnInfo(name = "name")
    public String name;

    /**
     * Тип упражнения:
     *  1 — Вес + повторения (Жим лежа, Приседания)
     *  2 — Без веса + повторения (Подтягивания, Отжимания)
     *  3 — Только время (Планка)
     *  4 — Время + расстояние + нагрузка (Гребля, Ходьба в горку)
     */
    @ColumnInfo(name = "type")
    public int type;

    /**
     * Флаг архива.
     * false — упражнение активно, показывается в списках.
     * true  — упражнение в архиве (пользователь его удалил, но история осталась).
     * Архивные не показываются в блоках "Все упражнения" и "Бывало раньше",
     * но участвуют в истории и статистике.
     */
    @ColumnInfo(name = "archived")
    public boolean archived;

    /**
     * Пустой конструктор — обязателен для Room.
     */
    public Exercise() {
    }

    /**
     * Удобный конструктор для создания нового упражнения.
     */
    public Exercise(String name, int type) {
        this.name = name;
        this.type = type;
        this.archived = false; // По умолчанию новое упражнение активно
    }
}
package com.example.jym.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity-класс для таблицы "workout_exercises" (выполненные упражнения).
 *
 * Это самая важная таблица для истории и статистики.
 * Каждая строка = одно упражнение внутри одной тренировки.
 *
 * Используем Integer (обёртку), а не int, чтобы значения могли быть null.
 * Например, для упражнения на время поле weight будет null (нам не нужно вес).
 */
@Entity(tableName = "workout_exercises")
public class WorkoutExercise {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * ID тренировки, к которой относится это упражнение.
     */
    @ColumnInfo(name = "workout_id")
    public int workoutId;

    /**
     * ID упражнения (из справочника).
     */
    @ColumnInfo(name = "exercise_id")
    public int exerciseId;

    /**
     * Вес в кг. null, если упражнение без веса или на время.
     */
    @ColumnInfo(name = "weight")
    public Integer weight;

    /**
     * Количество повторений. null, если упражнение на время.
     */
    @ColumnInfo(name = "reps")
    public Integer reps;

    /**
     * Количество подходов. null, если упражнение на время.
     */
    @ColumnInfo(name = "sets_count")
    public Integer setsCount;

    /**
     * Время в секундах. Используется для типов 3 и 4.
     */
    @ColumnInfo(name = "time_seconds")
    public Integer timeSeconds;

    /**
     * Расстояние в метрах. Используется только для типа 4.
     */
    @ColumnInfo(name = "distance_meters")
    public Integer distanceMeters;

    /**
     * Уровень нагрузки. Используется только для типа 4.
     */
    @ColumnInfo(name = "level")
    public Integer level;


    /**
     * Список повторений через запятую для типа 2:
     * например "10,10,9,8". Для других типов — null.
     * Дублирует информацию reps (макс.) и setsCount (кол-во),
     * чтобы была совместимость со старой логикой и статистикой.
     */
    @ColumnInfo(name = "reps_list")
    public String repsList;


    /**
     * Флаг "пропущено".
     * false — упражнение было выполнено.
     * true  — упражнение было в сете, но пользователь его не делал.
     */
    @ColumnInfo(name = "skipped")
    public boolean skipped;

    /**
     * Флаг "добавлено вручную".
     * false — упражнение было в сете.
     * true  — упражнение добавлено пользователем прямо на тренировке.
     * Такие упражнения участвуют в формировании блока "Бывало раньше".
     */
    @ColumnInfo(name = "is_custom")
    public boolean isCustom;

    public WorkoutExercise() {
    }
}
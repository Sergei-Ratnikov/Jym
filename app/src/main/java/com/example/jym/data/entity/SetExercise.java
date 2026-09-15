package com.example.jym.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity-класс для таблицы "set_exercises" (упражнения внутри сета).
 *
 * Это связь "многие-ко-многим": один сет содержит много упражнений,
 * и одно упражнение может быть в нескольких сетах.
 *
 * Пример строки: setId = 1 (сет "Руки"), exerciseId = 5 (Жим лежа), position = 0.
 * Это значит: в сете "Руки" на первой позиции стоит "Жим лежа".
 */
@Entity(tableName = "set_exercises")
public class SetExercise {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * ID сета, к которому относится эта строка.
     * Ссылается на поле id в таблице sets.
     */
    @ColumnInfo(name = "set_id")
    public int setId;

    /**
     * ID упражнения, которое стоит в этом сете.
     * Ссылается на поле id в таблице exercises.
     */
    @ColumnInfo(name = "exercise_id")
    public int exerciseId;

    /**
     * Позиция упражнения внутри сета (0, 1, 2, ...).
     * Используется для сортировки: сначала идёт упражнение с позицией 0,
     * потом 1, потом 2 и т.д.
     */
    @ColumnInfo(name = "position")
    public int position;

    public SetExercise() {
    }

    public SetExercise(int setId, int exerciseId, int position) {
        this.setId = setId;
        this.exerciseId = exerciseId;
        this.position = position;
    }
}
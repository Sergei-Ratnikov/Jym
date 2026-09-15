package com.example.jym.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entity-класс для таблицы "sets" (сеты/шаблоны тренировок).
 * Сет — это просто набор упражнений с названием.
 * Например, сет "Руки" — это список упражнений, которые надо сделать.
 * ВАЖНО: сет НЕ хранит значения веса/повторов. Они задаются на каждой тренировке.
 */
@Entity(tableName = "sets")
public class WorkoutSet {

    /**
     * Уникальный ID сета. Room сам присвоит номер.
     */
    @PrimaryKey(autoGenerate = true)
    public int id;

    /**
     * Название сета, например "Руки", "Ноги", "Спина".
     */
    @ColumnInfo(name = "name")
    public String name;

    /**
     * Порядковый номер в очереди.
     * 0 — первый сет, 1 — второй, 2 — третий и т.д.
     * Когда пользователь завершает тренировку, приложение переходит
     * к следующему сету по этому порядку.
     */
    @ColumnInfo(name = "order_index")
    public int orderIndex;
    /**
     * Активность тренировки-шаблона.
     * true  — участвует в очереди, показывается в списке «Сменить сет».
     * false — серый в списке, не участвует в очереди.
     */
    @ColumnInfo(name = "active", defaultValue = "1")
    public boolean active;


    public WorkoutSet() {
    }

    public WorkoutSet(String name, int orderIndex) {
        this.name = name;
        this.orderIndex = orderIndex;
        this.active = true;
    }
}
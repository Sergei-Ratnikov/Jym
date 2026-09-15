package com.example.jym.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.jym.data.entity.WorkoutSet;

import java.util.List;

/**
 * DAO для таблицы "sets".
 */
@Dao
public interface WorkoutSetDao {

    @Insert
    long insert(WorkoutSet set);

    @Update
    void update(WorkoutSet set);

    @Delete
    void delete(WorkoutSet set);

    /**
     * Получить все сеты, отсортированные по порядку в очереди.
     */
    @Query("SELECT * FROM sets ORDER BY order_index ASC")
    LiveData<List<WorkoutSet>> getAll();

    /**
     * То же самое, но синхронно (для логики в коде).
     */
    @Query("SELECT * FROM sets ORDER BY order_index ASC")
    List<WorkoutSet> getAllSync();

    @Query("SELECT * FROM sets WHERE id = :id")
    WorkoutSet getById(int id);

    /**
     * Получить максимальный order_index.
     * Нужно, когда добавляем новый сет — он должен встать в конец очереди.
     * Если сетов ещё нет — вернёт null.
     */
    @Query("SELECT MAX(order_index) FROM sets")
    Integer getMaxOrderIndex();

    /**
     * Найти первый сет в очереди (с минимальным order_index).
     * Используется при первом запуске, когда нет ещё завершённых тренировок.
     */
    @Query("SELECT * FROM sets ORDER BY order_index ASC LIMIT 1")
    WorkoutSet getFirstSet();

    /**
     * Удалить все сеты (используется в редких случаях — например, при отладке).
     */
    @Query("DELETE FROM sets")
    void deleteAll();

    @Query("DELETE FROM sets WHERE id = :id")
    void deleteById(int id);


    /** Активные тренировки-шаблоны (для диалога смены на экране тренировки). */
    @Query("SELECT * FROM sets WHERE active = 1 ORDER BY order_index ASC")
    List<WorkoutSet> getActiveSync();

    /** Первая активная тренировка в очереди. */
    @Query("SELECT * FROM sets WHERE active = 1 ORDER BY order_index ASC LIMIT 1")
    WorkoutSet getFirstActive();

    /** Переключить активность. */
    @Query("UPDATE sets SET active = :active WHERE id = :id")
    void setActive(int id, boolean active);

}
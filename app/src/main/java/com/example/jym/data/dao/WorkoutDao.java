package com.example.jym.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.jym.data.entity.Workout;

import java.util.List;

/**
 * DAO для таблицы "workouts".
 */
@Dao
public interface WorkoutDao {

    @Insert
    long insert(Workout workout);

    /**
     * Все тренировки, свежие сверху.
     * Используется на экране "История".
     */
    @Query("SELECT * FROM workouts ORDER BY start_time DESC")
    LiveData<List<Workout>> getAll();

    @Query("SELECT * FROM workouts WHERE id = :id")
    Workout getById(int id);

    /**
     * Получить самую последнюю тренировку.
     * Нужно для подписи "Сделано в прошлый раз: X дней назад".
     */
    @Query("SELECT * FROM workouts ORDER BY start_time DESC LIMIT 1")
    Workout getLastWorkout();

    /**
     * Синхронно получить все тренировки (свежие сверху).
     * Используется при загрузке экрана истории.
     */
    @Query("SELECT * FROM workouts ORDER BY start_time DESC")
    List<Workout> getAllSync();

    /** Удалить тренировку по id (используется при удалении записи из истории). */
    @Query("DELETE FROM workouts WHERE id = :id")
    void deleteById(int id);

}
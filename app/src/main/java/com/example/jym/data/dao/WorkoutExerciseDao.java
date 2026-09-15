package com.example.jym.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.example.jym.data.entity.WorkoutExercise;

import java.util.List;

/**
 * DAO для таблицы "workout_exercises".
 *
 * Здесь самые сложные запросы:
 *  - получить последние значения для упражнения (для автоподстановки),
 *  - получить список "ручных" упражнений (для блока "Бывало раньше"),
 *  - получить упражнения, выполненные сегодня.
 */
@Dao
public interface WorkoutExerciseDao {

    @Insert
    long insert(WorkoutExercise workoutExercise);

    /**
     * Все упражнения одной тренировки.
     */
    @Query("SELECT * FROM workout_exercises WHERE workout_id = :workoutId")
    LiveData<List<WorkoutExercise>> getByWorkoutId(int workoutId);

    @Query("SELECT * FROM workout_exercises WHERE workout_id = :workoutId")
    List<WorkoutExercise> getByWorkoutIdSync(int workoutId);

    /**
     * ПОЛУЧИТЬ ПОСЛЕДНИЕ ЗНАЧЕНИЯ ДЛЯ УПРАЖНЕНИЯ.
     *
     * Что делает: находит самую свежую (по времени тренировки) запись
     * для указанного упражнения, которая НЕ была пропущена.
     *
     * Используется на экране тренировки: чтобы подставить пользователю
     * те значения (вес, повторы и т.д.), которые он делал в прошлый раз.
     *
     * Логика: соединяем таблицу workout_exercises с workouts по workout_id,
     * фильтруем по exercise_id и skipped = 0, сортируем по start_time
     * (сначала свежие), берём первую.
     */
    @Query("SELECT we.* FROM workout_exercises we " +
            "INNER JOIN workouts w ON we.workout_id = w.id " +
            "WHERE we.exercise_id = :exerciseId AND we.skipped = 0 " +
            "ORDER BY w.start_time DESC LIMIT 1")
    WorkoutExercise getLastValuesForExercise(int exerciseId);

    /**
     * Уникальные ID упражнений, которые когда-либо были добавлены ВРУЧНУЮ
     * (is_custom = 1). Используется для блока "Бывало раньше".
     */
    @Query("SELECT DISTINCT exercise_id FROM workout_exercises WHERE is_custom = 1")
    List<Integer> getAllCustomExerciseIds();

    /**
     * ID упражнений, которые были задействованы (выполнены или пропущены)
     * в конкретную дату. Используется, чтобы исключить их из блока
     * "Все упражнения" на текущей тренировке.
     *
     * date — строка вида "2026-09-11".
     */
    @Query("SELECT DISTINCT we.exercise_id FROM workout_exercises we " +
            "INNER JOIN workouts w ON we.workout_id = w.id " +
            "WHERE w.date = :date")
    List<Integer> getExerciseIdsByDate(String date);

    /**
     * Удалить все упражнения тренировки (при удалении самой тренировки).
     */
    @Query("DELETE FROM workout_exercises WHERE workout_id = :workoutId")
    void deleteByWorkoutId(int workoutId);

    /**
     * Получить все записи об упражнении (для построения графика прогресса).
     * Сортировка: сначала старые, потом новые — чтобы график шёл слева направо.
     * Пропущенные записи исключаем.
     */
    @Query("SELECT we.* FROM workout_exercises we " +
            "INNER JOIN workouts w ON we.workout_id = w.id " +
            "WHERE we.exercise_id = :exerciseId AND we.skipped = 0 " +
            "ORDER BY w.start_time ASC")
    List<WorkoutExercise> getAllForExercise(int exerciseId);

    /**
     * Получить все НЕ пропущенные записи упражнений (для подсчёта рекордов).
     * Сортировка: сначала старые, потом новые.
     */
    @Query("SELECT we.* FROM workout_exercises we " +
            "WHERE we.skipped = 0 " +
            "ORDER BY we.id ASC")
    List<WorkoutExercise> getAllNonSkipped();

    /**
     * Получить дату (timestamp) тренировки для конкретной записи.
     * Нужно, чтобы сопоставить значения упражнения с датой.
     */
    @Query("SELECT w.start_time FROM workouts w " +
            "WHERE w.id = (SELECT workout_id FROM workout_exercises WHERE id = :weId)")
    long getWorkoutDateFor(int weId);


}
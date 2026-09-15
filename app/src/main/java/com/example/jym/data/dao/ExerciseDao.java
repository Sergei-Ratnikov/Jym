package com.example.jym.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.jym.data.entity.Exercise;

import java.util.List;

/**
 * DAO для таблицы "exercises".
 *
 * @Dao — говорит Room: "Сгенерируй реализацию этого интерфейса".
 *
 * LiveData<List<Exercise>> — "живой" список. Если данные в БД изменятся,
 * LiveData автоматически уведомит UI, и список на экране обновится.
 * Это удобно: не нужно вручную перезагружать.
 */
@Dao
public interface ExerciseDao {

    /**
     * Добавить новое упражнение.
     * @Insert автоматически сгенерирует INSERT INTO exercises ...
     * @return возвращает id вставленной строки.
     */
    @Insert
    long insert(Exercise exercise);

    /**
     * Обновить существующее упражнение.
     */
    @Update
    void update(Exercise exercise);

    /**
     * Удалить упражнение полностью.
     */
    @Delete
    void delete(Exercise exercise);

    /**
     * Получить все АКТИВНЫЕ упражнения, отсортированные по названию (A→Я).
     * Используется в блоке "Все упражнения" на экране тренировки.
     */
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    LiveData<List<Exercise>> getAllActive();

    /**
     * То же самое, но не LiveData, а обычный список.
     * Нужно для проверок в коде (например, проверка дубликатов).
     */
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE ASC")
    List<Exercise> getAllActiveSync();

    /**
     * Получить ВСЕ упражнения (активные + архивные), отсортированные по названию.
     * Используется на экране "Упражнения".
     */
    @Query("SELECT * FROM exercises ORDER BY archived ASC, name COLLATE NOCASE ASC")
    LiveData<List<Exercise>> getAllIncludingArchived();

    /**
     * Получить одно упражнение по его ID.
     */
    @Query("SELECT * FROM exercises WHERE id = :id")
    Exercise getById(int id);

    /**
     * Найти упражнение по названию (для проверки дубликата при создании).
     * Возвращает null, если такого нет.
     */
    @Query("SELECT * FROM exercises WHERE name = :name COLLATE NOCASE LIMIT 1")
    Exercise findByName(String name);

    /**
     * Проверить, есть ли у упражнения история выполнения.
     * Если есть — при удалении его нужно архивировать, а не удалять.
     * Возвращает количество записей в workout_exercises.
     */
    @Query("SELECT COUNT(*) FROM workout_exercises WHERE exercise_id = :exerciseId")
    int countHistory(int exerciseId);

    /**
     * Полное удаление по ID (используется при удалении навсегда из архива).
     */
    @Query("DELETE FROM exercises WHERE id = :id")
    void deleteById(int id);

    /**
     * Синхронная версия — все упражнения (активные + архивные).
     * Сортировка: активные сверху, потом по имени.
     */
    @Query("SELECT * FROM exercises ORDER BY archived ASC, name COLLATE NOCASE ASC")
    List<Exercise> getAllIncludingArchivedSync();
}
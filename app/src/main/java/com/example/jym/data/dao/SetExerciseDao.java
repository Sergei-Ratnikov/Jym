package com.example.jym.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.example.jym.data.entity.SetExercise;

import java.util.List;

/**
 * DAO для таблицы "set_exercises".
 */
@Dao
public interface SetExerciseDao {

    /**
     * Обновить связь (например, изменить позицию).
     */
    @Update
    void update(SetExercise setExercise);

    @Insert
    long insert(SetExercise setExercise);

    @Delete
    void delete(SetExercise setExercise);

    /**
     * Получить все упражнения внутри конкретного сета,
     * отсортированные по позиции.
     */
    @Query("SELECT * FROM set_exercises WHERE set_id = :setId ORDER BY position ASC")
    LiveData<List<SetExercise>> getBySetId(int setId);

    /**
     * Синхронная версия — для внутренних проверок.
     */
    @Query("SELECT * FROM set_exercises WHERE set_id = :setId ORDER BY position ASC")
    List<SetExercise> getBySetIdSync(int setId);

    /**
     * Получить максимальную позицию в сете.
     * Нужно при добавлении нового упражнения — оно встаёт в конец.
     */
    @Query("SELECT MAX(position) FROM set_exercises WHERE set_id = :setId")
    Integer getMaxPosition(int setId);

    /**
     * Удалить все упражнения из сета (используется при удалении самого сета).
     */
    @Query("DELETE FROM set_exercises WHERE set_id = :setId")
    void deleteBySetId(int setId);

    /**
     * Удалить конкретную связь по set_id и exercise_id.
     * Полезно, чтобы не давать добавить одно и то же упражнение дважды в сет.
     */
    @Query("DELETE FROM set_exercises WHERE set_id = :setId AND exercise_id = :exerciseId")
    void deleteBySetAndExercise(int setId, int exerciseId);

    /**
     * Проверить, есть ли упражнение в сете.
     */
    @Query("SELECT COUNT(*) FROM set_exercises WHERE set_id = :setId AND exercise_id = :exerciseId")
    int countInSet(int setId, int exerciseId);

    /**
     * Подсчитать количество упражнений в каждом сете.
     * Возвращает список объектов SetCount (setId + count).
     * Используется на экране «Сеты», чтобы показать «N упражнений».
     */
    @Query("SELECT set_id AS setId, COUNT(*) AS count " +
            "FROM set_exercises GROUP BY set_id")
    List<SetCount> getAllSetCounts();

}
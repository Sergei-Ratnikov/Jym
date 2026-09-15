package com.example.jym.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.jym.data.dao.ExerciseDao;
import com.example.jym.data.dao.SetExerciseDao;
import com.example.jym.data.dao.WorkoutDao;
import com.example.jym.data.dao.WorkoutExerciseDao;
import com.example.jym.data.dao.WorkoutSetDao;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.SetExercise;
import com.example.jym.data.entity.Workout;
import com.example.jym.data.entity.WorkoutExercise;
import com.example.jym.data.entity.WorkoutSet;

/**
 * Главный класс базы данных.
 *
 * @Database — говорит Room:
 *  - entities: список всех таблиц (все наши Entity-классы)
 *  - version: версия схемы. Если вы что-то поменяете в Entity,
 *             увеличьте число и добавьте миграцию.
 *  - exportSchema = false — не сохранять JSON-описание схемы (для простого проекта).
 *
 * RoomDatabase — родитель. Он даёт метод getDatabaseBuilder() для создания.
 */
@Database(
        entities = {
                Exercise.class,
                WorkoutSet.class,
                SetExercise.class,
                Workout.class,
                WorkoutExercise.class
        },
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    /**
     * Абстрактные методы. Room сам сгенерирует их реализацию.
     * Через них мы получаем доступ к DAO.
     */
    public abstract ExerciseDao exerciseDao();
    public abstract WorkoutSetDao workoutSetDao();
    public abstract SetExerciseDao setExerciseDao();
    public abstract WorkoutDao workoutDao();
    public abstract WorkoutExerciseDao workoutExerciseDao();

    /**
     * Singleton — единственный экземпляр базы данных на всё приложение.
     * Зачем: создавать несколько подключений к БД нельзя — это ошибка.
     * Поэтому храним один экземпляр в статическом поле.
     */
    private static volatile AppDatabase INSTANCE;

    /**
     * Получить экземпляр базы данных.
     * При первом вызове — создаёт БД, при последующих — возвращает готовую.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    // Создаём базу данных.
                    // "jym_database" — имя файла БД на телефоне.
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "jym_database"
                            )
                            // fallbackToDestructiveMigration — если схема изменилась,
                            // старая БД будет удалена и создана заново.
                            // ВАЖНО: это удалит данные пользователя. На этапе разработки — ок.
                            // В финальной версии надо писать миграции.
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
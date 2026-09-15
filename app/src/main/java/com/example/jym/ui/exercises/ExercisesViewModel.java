package com.example.jym.ui.exercises;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.jym.data.Repository;
import com.example.jym.data.entity.Exercise;

import java.util.List;

/**
 * ViewModel для экрана «Упражнения».
 * Что такое ViewModel:
 *  - хранит данные экрана отдельно от его UI;
 *  - переживает поворот экрана (данные не теряются);
 *  - связывает UI с Repository.
 * AndroidViewModel — разновидность ViewModel, которая даёт доступ к Application
 * (нужно для получения контекста и Repository).
 */
public class ExercisesViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<List<Exercise>> exercises;

    public ExercisesViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
        // LiveData — «живой» список. При изменении БД он автоматически обновится.
        exercises = repository.getAllExercises();
    }

    public LiveData<List<Exercise>> getExercises() {
        return exercises;
    }

    /**
     * Добавить упражнение. callback сообщает: true — добавлено, false — дубликат.
     */
    public void addExercise(String name, int type, Repository.Callback<Boolean> callback) {
        repository.addExercise(name, type, callback);
    }

    public void updateExercise(Exercise exercise) {
        repository.updateExercise(exercise);
    }

    /**
     * Удалить упражнение: callback сообщает true, если оно было архивировано
     * (была история), и false, если удалено навсегда.
     */
    public void deleteExercise(Exercise exercise, Repository.Callback<Boolean> callback) {
        repository.deleteExercise(exercise, callback);
    }

    public void restoreExercise(Exercise exercise) {
        repository.restoreExercise(exercise);
    }

    public void deleteExercisePermanently(int id) {
        repository.deleteExercisePermanently(id);
    }

    /**
     * Проверить, есть ли у упражнения история выполнения.
     * Нужно перед редактированием (менять ли тип) и перед удалением.
     */
    public void hasHistory(int exerciseId, Repository.Callback<Boolean> callback) {
        repository.hasHistory(exerciseId, callback);
    }
}
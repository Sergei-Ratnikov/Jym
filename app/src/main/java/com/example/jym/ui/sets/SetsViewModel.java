package com.example.jym.ui.sets;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.jym.data.Repository;
import com.example.jym.data.entity.WorkoutSet;

import java.util.List;
import java.util.Map;

/**
 * ViewModel для экрана «Сеты».
 */
public class SetsViewModel extends AndroidViewModel {

    private final Repository repository;
    private final LiveData<List<WorkoutSet>> sets;

    public SetsViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
        sets = repository.getAllSets();
    }

    public LiveData<List<WorkoutSet>> getSets() {
        return sets;
    }

    /** Создать сет. Возвращает id созданного сета. */
    public void addSet(String name, Repository.Callback<Integer> callback) {
        repository.addSet(name, callback);
    }

    /** Обновить название сета. */
    public void updateSet(WorkoutSet set) {
        repository.updateSet(set);
    }

    /** Удалить сет (безвозвратно, вместе со связями с упражнениями). */
    public void deleteSet(int setId) {
        repository.deleteSet(setId);
    }

    /** Сохранить новый порядок после drag & drop. */
    public void updateSetsOrder(List<WorkoutSet> orderedSets) {
        repository.updateSetsOrder(orderedSets);
    }

    /** Получить Map: setId → количество упражнений. */
    public void loadCounts(Repository.Callback<Map<Integer, Integer>> callback) {
        repository.getSetCounts(callback);
    }

    /** Переключить активность тренировки. */
    public void toggleSetActive(int setId, boolean active) {
        repository.toggleSetActive(setId, active);
    }
}
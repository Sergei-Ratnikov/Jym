package com.example.jym.ui.sets;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.jym.data.Repository;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.SetExercise;
import com.example.jym.data.entity.WorkoutSet;

import java.util.List;

/**
 * ViewModel для редактора сета.
 */
public class SetEditorViewModel extends AndroidViewModel {

    private final Repository repository;
    private int setId;

    public SetEditorViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
    }

    public void setSetId(int setId) {
        this.setId = setId;
    }

    /** LiveData — упражнения, которые лежат в этом сете. */
    public LiveData<List<SetExercise>> getExercisesInSet() {
        return repository.getExercisesInSet(setId);
    }

    /** Название сета (берём из БД). Работает синхронно, но вызывается из фона. */
    public void loadSet(Repository.Callback<WorkoutSet> callback) {
        new Thread(() -> {
            WorkoutSet s = repository.getSetById(setId);
            if (callback != null) callback.onResult(s);
        }).start();
    }

    public void updateSet(WorkoutSet set) {
        repository.updateSet(set);
    }

    public void addExerciseToSet(int exerciseId, Repository.Callback<Boolean> callback) {
        repository.addExerciseToSet(setId, exerciseId, callback);
    }

    public void removeExerciseFromSet(int exerciseId) {
        repository.removeExerciseFromSet(setId, exerciseId);
    }

    public void updateExercisesOrder(List<SetExercise> list) {
        repository.updateExercisesOrder(setId, list);
    }

    /** Получить все активные упражнения (для диалога выбора). */
    public LiveData<List<Exercise>> getAllActiveExercises() {
        return repository.getAllActiveExercises();
    }

    public void updateSetNameOnly(int setId, String newName) {
        repository.updateSetNameOnly(setId, newName);
    }

    public Exercise getExerciseById(int id) {
        return repository.getExerciseById(id);
    }

}
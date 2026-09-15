package com.example.jym.ui.statistics;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jym.data.Repository;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.Workout;
import com.example.jym.data.entity.WorkoutExercise;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel для экрана статистики.
 *
 * Содержит три независимых потока данных:
 *  - прогресс по выбранному упражнению (для графика);
 *  - список личных рекордов;
 *  - общая статистика по месяцам.
 */
public class StatisticsViewModel extends AndroidViewModel {

    private final Repository repository;

    // === Список всех упражнений (для выпадающего списка) ===
    private final MutableLiveData<List<Exercise>> exercises = new MutableLiveData<>();

    // === Данные для графика прогресса ===
    private final MutableLiveData<List<ProgressPoint>> progressPoints = new MutableLiveData<>();
    private final MutableLiveData<Integer> progressType = new MutableLiveData<>();

    // === Личные рекорды ===
    private final MutableLiveData<List<PersonalRecord>> records = new MutableLiveData<>();

    // === Общая статистика ===
    private final MutableLiveData<Integer> totalWorkouts = new MutableLiveData<>();
    private final MutableLiveData<Long> totalVolume = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Integer>> workoutsByMonth = new MutableLiveData<>();

    public StatisticsViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
    }

    public LiveData<List<Exercise>> getExercises() { return exercises; }
    public LiveData<List<ProgressPoint>> getProgressPoints() { return progressPoints; }
    public LiveData<Integer> getProgressType() { return progressType; }
    public LiveData<List<PersonalRecord>> getRecords() { return records; }
    public LiveData<Integer> getTotalWorkouts() { return totalWorkouts; }
    public LiveData<Long> getTotalVolume() { return totalVolume; }
    public LiveData<Map<String, Integer>> getWorkoutsByMonth() { return workoutsByMonth; }

    // ================================================================
    // ============       ЗАГРУЗКА СПИСКА УПРАЖНЕНИЙ    ==============
    // ================================================================

    public void loadExercises() {
        new Thread(() -> {
            List<Exercise> list = repository.getAllExercisesSync();
            exercises.postValue(list);
        }).start();
    }

    // ================================================================
    // ============         ПРОГРЕСС ПО УПРАЖНЕНИЮ      ==============
    // ================================================================

    /**
     * Загрузить точки для графика прогресса по выбранному упражнению.
     * Логика значений:
     *  - тип 1: максимальный вес за тренировку (вес × повторы × подходы);
     *  - тип 2: максимальное количество повторений;
     *  - тип 3: максимальное время (сек);
     *  - тип 4: время (сек) — можно поменять на расстояние/уровень.
     */
    public void loadProgress(int exerciseId) {
        new Thread(() -> {
            List<WorkoutExercise> all = repository.getAllForExerciseSync(exerciseId);
            if (all.isEmpty()) {
                progressPoints.postValue(new ArrayList<>());
                return;
            }

            // Берём тип упражнения из первой записи.
            Exercise ex = repository.getExerciseById(exerciseId);
            int type = (ex != null) ? ex.type : 1;
            progressType.postValue(type);

            // Группируем по тренировке: для каждого workout_id берём максимум.
            Map<Integer, Float> maxByWorkout = new HashMap<>();
            Map<Integer, Long> dateByWorkout = new HashMap<>();

            for (WorkoutExercise we : all) {
                float v = extractValue(we, type);
                Float cur = maxByWorkout.get(we.workoutId);
                if (cur == null || v > cur) {
                    maxByWorkout.put(we.workoutId, v);
                }
                if (!dateByWorkout.containsKey(we.workoutId)) {
                    long d = repository.getWorkoutDateForSync(we.id);
                    dateByWorkout.put(we.workoutId, d);
                }
            }

            // Формируем список точек и сортируем по дате.
            List<ProgressPoint> points = new ArrayList<>();
            for (Map.Entry<Integer, Float> e : maxByWorkout.entrySet()) {
                Long d = dateByWorkout.get(e.getKey());
                if (d != null) points.add(new ProgressPoint(d, e.getValue()));
            }
            points.sort((a, b) -> Long.compare(a.date, b.date));

            progressPoints.postValue(points);
        }).start();
    }

    /** Достаёт основное значение из записи в зависимости от типа. */
    private float extractValue(WorkoutExercise we, int type) {
        return switch (type) {
            case 1 -> we.weight == null ? 0f : we.weight;
            case 2 -> we.reps == null ? 0f : we.reps;
            case 3, 4 -> we.timeSeconds == null ? 0f : we.timeSeconds;
            default -> 0f;
        };
    }

    // ================================================================
    // ============           ЛИЧНЫЕ РЕКОРДЫ             ==============
    // ================================================================

    /**
     * Собирает личные рекорды по всем упражнениям.
     */
    public void loadRecords() {
        new Thread(() -> {
            List<WorkoutExercise> all = repository.getAllNonSkippedSync();
            List<Exercise> allExercises = repository.getAllExercisesSync();

            // Карта: exerciseId → лучший результат.
            Map<Integer, Float> best = new HashMap<>();
            Map<Integer, Integer> bestWeId = new HashMap<>();

            for (WorkoutExercise we : all) {
                Exercise ex = findById(allExercises, we.exerciseId);
                if (ex == null) continue;
                float v = extractValue(we, ex.type);
                Float cur = best.get(we.exerciseId);
                if (cur == null || v > cur) {
                    best.put(we.exerciseId, v);
                    bestWeId.put(we.exerciseId, we.id);
                }
            }

            List<PersonalRecord> result = new ArrayList<>();
            for (Map.Entry<Integer, Float> e : best.entrySet()) {
                Exercise ex = findById(allExercises, e.getKey());
                if (ex == null) continue;

                PersonalRecord pr = new PersonalRecord();
                pr.exerciseName = ex.name;
                pr.exerciseType = ex.type;
                pr.recordText = formatRecord(e.getValue(), ex.type);

                Integer weId = bestWeId.get(e.getKey());
                pr.date = (weId != null) ? repository.getWorkoutDateForSync(weId) : 0L;
                result.add(pr);
            }

            // Сортировка: сначала свежие рекорды.
            result.sort((a, b) -> Long.compare(b.date, a.date));
            records.postValue(result);
        }).start();
    }

    private Exercise findById(List<Exercise> list, int id) {
        for (Exercise e : list) if (e.id == id) return e;
        return null;
    }

    private String formatRecord(float v, int type) {
        if (type == 1) return (int) v + " кг";
        if (type == 2) return (int) v + " повт.";
        if (type == 3) return (int) v + " с";
        if (type == 4) return (int) v + " с";
        return String.valueOf((int) v);
    }

    // ================================================================
    // ============         ОБЩАЯ СТАТИСТИКА            ==============
    // ================================================================

    public void loadGeneral() {
        new Thread(() -> {
            List<Workout> workouts = repository.getAllWorkoutsSync();
            totalWorkouts.postValue(workouts.size());

            // Считаем суммарный объём (только для типов 1 и 2).
            long volume = getVolume();
            totalVolume.postValue(volume);

            // Группировка по месяцам (последние 6 месяцев).
            Map<String, Integer> byMonth = new HashMap<>();
            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("yyyy-MM",
                            java.util.Locale.getDefault());
            for (Workout w : workouts) {
                String key = fmt.format(new java.util.Date(w.startTime));
                Integer cnt = byMonth.get(key);
                byMonth.put(key, cnt == null ? 1 : cnt + 1);
            }
            workoutsByMonth.postValue(byMonth);
        }).start();
    }

    private long getVolume() {
        List<WorkoutExercise> all = repository.getAllNonSkippedSync();
        List<Exercise> allExercises = repository.getAllExercisesSync();

        long volume = 0;
        for (WorkoutExercise we : all) {
            Exercise ex = findById(allExercises, we.exerciseId);
            if (ex == null) continue;
            if (ex.type == 1 && we.weight != null && we.reps != null && we.setsCount != null) {
                volume += (long) we.weight * we.reps * we.setsCount;
            } else if (ex.type == 2 && we.reps != null && we.setsCount != null) {
                volume += (long) we.reps * we.setsCount;
            }
        }
        return volume;
    }
}
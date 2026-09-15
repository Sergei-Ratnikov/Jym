package com.example.jym.ui.training;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jym.data.Repository;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.SetExercise;
import com.example.jym.data.entity.Workout;
import com.example.jym.data.entity.WorkoutExercise;
import com.example.jym.data.entity.WorkoutSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * ViewModel экрана тренировки.
 * Содержит всю логику:
 *  - загрузка текущего сета и формирование трёх блоков;
 *  - активация/деактивация упражнений;
 *  - таймер тренировки (старт по первому активному, стоп и обнуление по последнему);
 *  - сохранение тренировки.
 */
public class TrainingViewModel extends AndroidViewModel {

    private final Repository repository;

    // === LiveData для адаптера ===
    private final MutableLiveData<List<TrainingListItem>> listItems = new MutableLiveData<>();

    // === Инфо-строки ===
    private final MutableLiveData<String> lastWorkoutText = new MutableLiveData<>();
    private final MutableLiveData<String> currentSetName = new MutableLiveData<>();

    // === Таймер ===
    private final MutableLiveData<String> timerText = new MutableLiveData<>("00:00");
    private final MutableLiveData<Boolean> hasActiveExercises = new MutableLiveData<>(false);

    /** Накопленное количество секунд (без текущего запуска). */
    private int timerSeconds = 0;
    /** Момент последнего запуска таймера (System.currentTimeMillis). */
    private long timerStartTime = 0;
    /** Идёт ли таймер сейчас. */
    private boolean timerRunning = false;


    /** Имя SharedPreferences-файла для черновика. */
    private static final String DRAFT_PREFS = "jym_draft";
    private static final String DRAFT_KEY = "draft";

    /** Флаг: проверяли ли черновик при этом запуске (не сбрасывается, пока жива ViewModel). */
    private boolean draftChecked = false;

    /** Handler для тика каждую секунду. */
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    /** Runnable, который обновляет таймер каждую секунду. */
    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (!timerRunning) return;
            long elapsed = (System.currentTimeMillis() - timerStartTime) / 1000;
            int total = timerSeconds + (int) elapsed;
            timerText.setValue(formatTime(total));
            timerHandler.postDelayed(this, 1000);
        }
    };

    // === Данные тренировки ===
    private WorkoutSet currentSet;
    private final List<ExerciseItem> allItems = new ArrayList<>();
    private final List<Exercise> availableForAll = new ArrayList<>();
    private int activationCounter = 0;

    /** Флаг: загрузка уже была. Нужен, чтобы возврат из других экранов не сбрасывал состояние. */
    private boolean initialized = false;

    public TrainingViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
    }

    // === Геттеры ===
    public LiveData<List<TrainingListItem>> getListItems() { return listItems; }
    public LiveData<String> getLastWorkoutText() { return lastWorkoutText; }
    public LiveData<String> getCurrentSetName() { return currentSetName; }
    public LiveData<String> getTimerText() { return timerText; }
    public LiveData<Boolean> getHasActiveExercises() { return hasActiveExercises; }
    public WorkoutSet getCurrentSet() { return currentSet; }

    /**
     * Есть ли сейчас активные упражнения (для диалога смены сета).
     */
    public boolean isAnyActivated() {
        return Boolean.TRUE.equals(hasActiveExercises.getValue());
    }

    // ================================================================
    // ============          ЗАГРУЗКА ДАННЫХ          =================
    // ================================================================

    public void load() {
        if (initialized) {
            refreshAvailableExercises();
            return;
        }
        initialized = true;

        resetTimerState();

        new Thread(() -> {
            loadEverythingSync();

            // Формируем список для адаптера.
            new Handler(Looper.getMainLooper()).post(() -> {
                currentSetName.setValue(currentSet != null ? currentSet.name : "Нет тренировок");
                rebuildList();
            });
        }).start();
    }

    /**
     * Обновляет только список доступных упражнений (блок «Все упражнения»).
     * Не трогает состояние тренировки.
     */
    public void refreshAvailableExercises() {
        new Thread(() -> {
            List<Exercise> allActive = repository.getAllActiveExercisesSync();
            allActive.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
            new Handler(Looper.getMainLooper()).post(() -> {
                availableForAll.clear();
                availableForAll.addAll(allActive);
                rebuildList();
            });
        }).start();
    }

    // ================================================================
    // ============       ФОРМИРОВАНИЕ СПИСКА          =================
    // ================================================================

    private void rebuildList() {
        List<TrainingListItem> result = new ArrayList<>();

        if (currentSet == null) {
            listItems.postValue(result);
            return;
        }

        // === Блок «Сегодня» ===

        // 1. Активные (по убыванию activationOrder).
        List<ExerciseItem> activeItems = new ArrayList<>();
        for (ExerciseItem it : allItems) if (it.isActive) activeItems.add(it);
        activeItems.sort((a, b) -> Integer.compare(b.activationOrder, a.activationOrder));
        List<ExerciseItem> todayList = new ArrayList<>(activeItems);

        // 2. Неактивные из сета.
        List<ExerciseItem> setItems = new ArrayList<>();
        for (ExerciseItem it : allItems) {
            if (it.sourceBlock == ExerciseItem.SOURCE_SET
                    && !it.isActive && !it.wasActivated) {
                setItems.add(it);
            }
        }
        setItems.sort(Comparator.comparingInt(a -> a.setPosition));
        todayList.addAll(setItems);

        // 3. Ранее активированные, потом погашенные.
        List<ExerciseItem> previouslyActive = new ArrayList<>();
        for (ExerciseItem it : allItems) {
            if (!it.isActive && it.wasActivated) previouslyActive.add(it);
        }
        previouslyActive.sort(Comparator.comparingInt(a -> a.activationOrder));
        todayList.addAll(previouslyActive);

        result.add(TrainingListItem.header("СЕГОДНЯ"));
        for (ExerciseItem it : todayList) result.add(TrainingListItem.exercise(it));

        // === Блок «Бывало раньше» ===
        List<ExerciseItem> pastList = new ArrayList<>();
        for (ExerciseItem it : allItems) {
            if (it.sourceBlock == ExerciseItem.SOURCE_PAST
                    && !it.isActive && !it.wasActivated) {
                pastList.add(it);
            }
        }
        pastList.sort((a, b) -> a.exercise.name.compareToIgnoreCase(b.exercise.name));
        if (!pastList.isEmpty()) {
            result.add(TrainingListItem.header("ДОБАВКА"));
            for (ExerciseItem it : pastList) result.add(TrainingListItem.exercise(it));
        }

        // === Блок «Все упражнения» ===
        if (!availableForAll.isEmpty()) {
            result.add(TrainingListItem.header("ВСЕ УПРАЖНЕНИЯ"));
            for (Exercise e : availableForAll) {
                result.add(TrainingListItem.exercise(ExerciseItem.template(e)));
            }
        }

        listItems.postValue(result);
    }

    // ================================================================
    // ============     АКТИВАЦИЯ / ДЕАКТИВАЦИЯ        =================
    // ================================================================

    public void toggleExercise(ExerciseItem item) {
        if (item.isTemplate) return;

        if (item.isActive) {
            // Деактивация.
            item.isActive = false;
        } else {
            // Активация.
            item.isActive = true;
            item.wasActivated = true;
            item.activationOrder = ++activationCounter;
        }
        rebuildList();
        updateActiveStateAndTimer();
    }

    /**
     * Добавить новое упражнение из шаблона.
     * Сразу делаем его активным → таймер запустится.
     */
    public void addFromTemplate(Exercise exercise) {
        new Thread(() -> {
            WorkoutExercise last = repository.getLastValuesForExerciseSync(exercise.id);

            ExerciseItem item = new ExerciseItem();
            item.exercise = exercise;
            item.sourceBlock = ExerciseItem.SOURCE_ALL;
            item.isTemplate = false;
            item.applyLastValues(last);
            item.isActive = true;
            item.wasActivated = true;

            new Handler(Looper.getMainLooper()).post(() -> {
                item.activationOrder = ++activationCounter;
                allItems.add(item);
                rebuildList();
                updateActiveStateAndTimer();
            });
        }).start();
    }

    // ================================================================
    // ============          ТАЙМЕР                    =================
    // ================================================================

    /** Сколько упражнений сейчас активно. */
    private int countActive() {
        int c = 0;
        for (ExerciseItem it : allItems) if (it.isActive) c++;
        return c;
    }

    /**
     * Обновляет флаг hasActiveExercises и управляет таймером:
     *  - 0 → n: запускает таймер;
     *  - n → 0: обнуляет и останавливает.
     */
    private void updateActiveStateAndTimer() {
        int active = countActive();
        hasActiveExercises.setValue(active > 0);

        if (active > 0 && !timerRunning) {
            startTimer();
        } else if (active == 0 && timerRunning) {
            stopAndResetTimer();
        }
    }

    private void startTimer() {
        timerStartTime = System.currentTimeMillis();
        timerRunning = true;
        timerText.setValue(formatTime(timerSeconds));
        timerHandler.post(timerRunnable);
    }

    /** Останавливает таймер, СОХРАНЯЯ накопленные секунды. */
    private void stopTimer() {
        if (!timerRunning) return;
        long elapsed = (System.currentTimeMillis() - timerStartTime) / 1000;
        timerSeconds += (int) elapsed;
        timerRunning = false;
        timerHandler.removeCallbacks(timerRunnable);
    }

    /** Останавливает и обнуляет таймер. */
    private void stopAndResetTimer() {
        stopTimer();
        timerSeconds = 0;
        timerText.setValue("00:00");
    }

    /** Полный сброс состояния таймера (без сохранения). */
    private void resetTimerState() {
        timerHandler.removeCallbacks(timerRunnable);
        timerRunning = false;
        timerSeconds = 0;
        timerText.postValue("00:00");
        hasActiveExercises.postValue(false);
    }

    /** Формат: "MM:SS" или "H:MM:SS" для долгих тренировок. */
    private String formatTime(int totalSeconds) {
        int h = totalSeconds / 3600;
        int m = (totalSeconds % 3600) / 60;
        int s = totalSeconds % 60;
        if (h > 0) {
            return String.format(Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        }
        return String.format(Locale.getDefault(), "%02d:%02d", m, s);
    }

    // ================================================================
    // ============           ЗАКОНЧИТЬ               =================
    // ================================================================

    /**
     * Завершить тренировку.
     * Останавливает таймер, сохраняет в БД, сдвигает очередь.
     */
    public void finishWorkout(Runnable callback) {
        // Останавливаем таймер и берём накопленное время как длительность.
        clearDraft();
        stopTimer();
        final int durationSeconds = timerSeconds;

        // Сбрасываем состояние таймера.
        timerSeconds = 0;
        timerRunning = false;
        timerText.setValue("00:00");
        hasActiveExercises.setValue(false);

        new Thread(() -> {
            // Активные упражнения.
            List<WorkoutExercise> active = new ArrayList<>();
            for (ExerciseItem it : allItems) {
                if (it.isActive) active.add(it.toWorkoutExercise());
            }

            // Время: старт = конец - длительность (в секундах).
            long endTime = System.currentTimeMillis();
            long startTime = endTime - durationSeconds * 1000L;

            // Передаём пустой список вместо пропущенных —
            // в историю пишем только то, что пользователь реально сделал.
            repository.saveWorkoutSync(startTime, active, new ArrayList<>());

            // Сдвиг очереди на следующий сет.
            if (currentSet != null) {
                WorkoutSet next = repository.getNextSet(currentSet.id);
                if (next != null) repository.setCurrentSetId(next.id);
            }

            if (callback != null) {
                new Handler(Looper.getMainLooper()).post(callback);
            }
        }).start();
    }

    /** Полный сброс для новой тренировки. */
    public void resetForNewWorkout() {
        clearDraft(); // ← добавляем
        resetTimerState();
        activationCounter = 0;
        initialized = false;
        load();
    }

    // ================================================================
    // ============         СМЕНА Тренировки          =================
    // ================================================================

    public void loadAllSetsAsync(Repository.Callback<List<WorkoutSet>> callback) {
        new Thread(() -> {
            final List<WorkoutSet> sets = repository.getActiveSetsSync();
            new Handler(Looper.getMainLooper()).post(() -> {
                if (callback != null) callback.onResult(sets);
            });
        }).start();
    }

    public void changeSet(int newSetId) {
        repository.setCurrentSetId(newSetId);
        resetForNewWorkout();
    }


    // ================================================================
    // ============         АВТОСОХРАНЕНИЕ             =================
    // ================================================================

    /** Был ли уже проверен черновик (для UI — не спрашивать повторно). */
    public boolean isDraftChecked() { return draftChecked; }
    public void setDraftChecked(boolean b) { this.draftChecked = b; }

    /** Есть ли сохранённый черновик. */
    public boolean hasDraft() {
        SharedPreferences p = getApplication()
                .getSharedPreferences(DRAFT_PREFS, Context.MODE_PRIVATE);
        return p.contains(DRAFT_KEY);
    }

    /** Удалить черновик (при завершении тренировки). */
    public void clearDraft() {
        SharedPreferences p = getApplication()
                .getSharedPreferences(DRAFT_PREFS, Context.MODE_PRIVATE);
        p.edit().remove(DRAFT_KEY).apply();
    }

    /**
     * Сохранить черновик текущей тренировки.
     * Работает только если есть хотя бы одно активное упражнение.
     * Иначе — черновик удаляется.
     */
    public void saveDraft() {
        if (countActive() == 0) {
            clearDraft();
            return;
        }

        try {
            JSONObject root = new JSONObject();
            root.put("setId", currentSet != null ? currentSet.id : -1);

            // Текущее время таймера (с учётом идущего запуска).
            int totalSeconds = timerSeconds;
            if (timerRunning) {
                totalSeconds += (int) ((System.currentTimeMillis() - timerStartTime) / 1000);
            }
            root.put("timerSeconds", totalSeconds);
            root.put("activationCounter", activationCounter);

            JSONArray arr = new JSONArray();
            for (ExerciseItem it : allItems) {
                if (!it.isActive) continue;

                JSONObject obj = new JSONObject();
                obj.put("exerciseId", it.exercise.id);
                obj.put("sourceBlock", it.sourceBlock);
                obj.put("setPosition", it.setPosition);
                obj.put("activationOrder", it.activationOrder);
                putNullable(obj, "weight", it.weight);
                putNullable(obj, "reps", it.reps);
                putNullable(obj, "setsCount", it.setsCount);
                putNullable(obj, "timeSeconds", it.timeSeconds);
                putNullable(obj, "distanceMeters", it.distanceMeters);
                putNullable(obj, "level", it.level);
                arr.put(obj);
            }
            root.put("activeItems", arr);

            SharedPreferences p = getApplication()
                    .getSharedPreferences(DRAFT_PREFS, Context.MODE_PRIVATE);
            p.edit().putString(DRAFT_KEY, root.toString()).apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Восстановить тренировку из черновика.
     * Загружает всё как обычно, затем применяет активные упражнения.
     */
    public void restoreDraft() {
        new Thread(() -> {
            // 1. Загружаем базу (сет, справочники, шаблоны).
            loadEverythingSync();

            // 2. Читаем черновик.
            SharedPreferences p = getApplication()
                    .getSharedPreferences(DRAFT_PREFS, Context.MODE_PRIVATE);
            String json = p.getString(DRAFT_KEY, null);
            if (json == null) {
                new Handler(Looper.getMainLooper()).post(this::rebuildList);
                return;
            }

            try {
                JSONObject root = new JSONObject(json);
                final int timerFromDraft = root.getInt("timerSeconds");
                final int counterFromDraft = root.getInt("activationCounter");
                JSONArray arr = root.getJSONArray("activeItems");

                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    int exId = obj.getInt("exerciseId");
                    int sourceBlock = obj.getInt("sourceBlock");
                    int setPos = obj.optInt("setPosition", -1);

                    // Ищем подходящий ExerciseItem среди загруженных.
                    ExerciseItem found = null;
                    for (ExerciseItem it : allItems) {
                        if (it.exercise.id == exId
                                && it.sourceBlock == sourceBlock
                                && !it.isActive) {
                            found = it;
                            break;
                        }
                    }

                    if (found == null) {
                        // Не нашли — создаём вручную (например, из «Все упражнения»).
                        Exercise e = repository.getExerciseById(exId);
                        if (e == null) continue;
                        found = new ExerciseItem();
                        found.exercise = e;
                        found.sourceBlock = sourceBlock;
                        found.setPosition = setPos;
                        found.isTemplate = false;
                        allItems.add(found);
                    }

                    found.isActive = true;
                    found.wasActivated = true;
                    found.activationOrder = obj.optInt("activationOrder", i + 1);
                    found.weight = getNullable(obj, "weight");
                    found.reps = getNullable(obj, "reps");
                    found.setsCount = getNullable(obj, "setsCount");
                    found.timeSeconds = getNullable(obj, "timeSeconds");
                    found.distanceMeters = getNullable(obj, "distanceMeters");
                    found.level = getNullable(obj, "level");
                }

                // 3. Восстанавливаем накопленное время и счётчик.
                timerSeconds = timerFromDraft;
                activationCounter = Math.max(counterFromDraft, countActive());

            } catch (JSONException e) {
                e.printStackTrace();
            }

            // 4. Возвращаемся в UI-поток.
            new Handler(Looper.getMainLooper()).post(() -> {
                currentSetName.setValue(currentSet != null ? currentSet.name : "Нет сетов");
                rebuildList();
                updateActiveStateAndTimer(); // таймер стартует автоматически, если есть активные
                timerText.setValue(formatTime(timerSeconds));
            });
        }).start();
    }

    /**
     * Синхронная загрузка «всего, что нужно» — сет, упражнения из сета,
     * custom-упражнения, шаблоны. Используется и в load(), и в restoreDraft().
     */
    private void loadEverythingSync() {
        // 1. Текущий сет.
        int setId = repository.getCurrentSetId();
        WorkoutSet set = (setId > 0) ? repository.getSetById(setId) : null;
        if (set == null) {
            set = repository.getFirstSet();
            if (set != null) repository.setCurrentSetId(set.id);
        }
        currentSet = set;

        // 2. Инфо-строка.
        Workout last = repository.getLastWorkoutSync();
        String text;
        if (last == null) {
            text = "Выполняется впервые";
        } else {
            text = "Сделано в прошлый раз: " +
                    com.example.jym.util.DateUtils.getDaysAgoText(last.startTime);
        }
        lastWorkoutText.postValue(text);

        // 3. Очищаем и формируем списки.
        allItems.clear();
        availableForAll.clear();

        // 3.1. Из сета.
        Set<Integer> inSetIds = new HashSet<>();
        if (set != null) {
            List<SetExercise> setExercises = repository.getExercisesInSetSync(set.id);
            for (SetExercise se : setExercises) {
                Exercise e = repository.getExerciseById(se.exerciseId);
                if (e == null || e.archived) continue;
                ExerciseItem item = new ExerciseItem();
                item.exercise = e;
                item.sourceBlock = ExerciseItem.SOURCE_SET;
                item.setPosition = se.position;
                item.applyLastValues(repository.getLastValuesForExerciseSync(e.id));
                allItems.add(item);
                inSetIds.add(e.id);
            }
        }

        // 3.2. Custom из истории.
        List<Integer> customIds = repository.getCustomExerciseIdsSync();
        for (int id : customIds) {
            if (inSetIds.contains(id)) continue;
            Exercise e = repository.getExerciseById(id);
            if (e == null || e.archived) continue;
            ExerciseItem item = new ExerciseItem();
            item.exercise = e;
            item.sourceBlock = ExerciseItem.SOURCE_PAST;
            item.applyLastValues(repository.getLastValuesForExerciseSync(id));
            allItems.add(item);
        }

        // 3.3. Шаблоны для блока «Все упражнения».
        List<Exercise> allActive = repository.getAllActiveExercisesSync();
        allActive.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        availableForAll.addAll(allActive);
    }

    /** Записать значение в JSON, если оно не null. */
    private void putNullable(JSONObject o, String key, Integer v) throws JSONException {
        if (v == null) o.put(key, JSONObject.NULL);
        else o.put(key, v);
    }

    /** Прочитать значение из JSON, вернуть null если было null. */
    private Integer getNullable(JSONObject o, String key) {
        if (o.isNull(key)) return null;
        int v = o.optInt(key, Integer.MIN_VALUE);
        return v == Integer.MIN_VALUE ? null : v;
    }


    // ================================================================
    // ============        ОЧИСТКА РЕСУРСОВ           =================
    // ================================================================

    @Override
    protected void onCleared() {
        super.onCleared();
        timerHandler.removeCallbacks(timerRunnable);
    }
}
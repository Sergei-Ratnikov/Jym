package com.example.jym.ui.history;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.jym.data.Repository;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.Workout;
import com.example.jym.data.entity.WorkoutExercise;
import com.example.jym.util.DateUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * ViewModel экрана «История».
 * Загружает тренировки, группирует их по дням, суммирует подходы
 * у одинаковых упражнений, сортирует по алфавиту и вставляет
 * заголовки месяцев между днями.
 */
public class HistoryViewModel extends AndroidViewModel {

    private final Repository repository;
    private final MutableLiveData<List<HistoryListItem>> items = new MutableLiveData<>();

    public HistoryViewModel(@NonNull Application application) {
        super(application);
        repository = Repository.getInstance(application);
    }

    public LiveData<List<HistoryListItem>> getItems() {
        return items;
    }

    // ================================================================
    // ============          ЗАГРУЗКА И ГРУППИРОВКА       =============
    // ================================================================

    public void load() {
        new Thread(() -> {
            List<Workout> workouts = repository.getAllWorkoutsSync();
            // getAllSync уже возвращает сортировку по start_time DESC,
            // значит свежие дни в начале.

            // Группируем по дате "yyyy-MM-dd" — сохраняем порядок (LinkedHashMap).
            Map<String, List<Workout>> byDate = new LinkedHashMap<>();
            for (Workout w : workouts) {
                List<Workout> list = byDate.get(w.date);
                if (list == null) {
                    list = new ArrayList<>();
                    byDate.put(w.date, list);
                }
                list.add(w);
            }

            // Формируем итоговый список с заголовками месяцев.
            List<HistoryListItem> result = new ArrayList<>();
            String prevMonth = null;

            for (Map.Entry<String, List<Workout>> entry : byDate.entrySet()) {
                String date = entry.getKey();
                List<Workout> dayList = entry.getValue();

                // Заголовок месяца.
                String month = date.substring(0, 7); // "yyyy-MM"
                if (!month.equals(prevMonth)) {
                    result.add(HistoryListItem.month(formatMonth(date)));
                    prevMonth = month;
                }

                // Карточка дня.
                result.add(HistoryListItem.day(buildDay(dayList)));
            }

            items.postValue(result);
        }).start();
    }

    /**
     * Удалить тренировку (или все тренировки, если их несколько в одном дне).
     * После удаления перезагружаем список.
     */
    public void deleteDay(HistoryItem day) {
        new Thread(() -> {
            for (Integer id : day.workoutIds) {
                repository.deleteWorkout(id);
            }
            // Небольшая пауза, чтобы БД успела применить изменения.
            try { Thread.sleep(100); } catch (InterruptedException e) { /* ignore */ }
            load();
        }).start();
    }

    // ================================================================
    // ============     ПОСТРОЕНИЕ КАРТОЧКИ ДНЯ           =============
    // ================================================================

    private HistoryItem buildDay(List<Workout> dayWorkouts) {
        HistoryItem item = new HistoryItem();

        // Сортируем внутри дня по времени начала (утренняя — раньше).
        dayWorkouts.sort((a, b) -> Long.compare(a.startTime, b.startTime));

        item.date = dayWorkouts.get(0).date;
        item.startTime = dayWorkouts.get(0).startTime;
        item.endTime = dayWorkouts.get(dayWorkouts.size() - 1).endTime;

        // Карта объединённых упражнений:
        // ключ = имя + тип + параметры → дельта
        Map<String, HistoryItem.ExerciseDetail> grouped = new LinkedHashMap<>();
        long totalDuration = 0;

        for (Workout w : dayWorkouts) {
            item.workoutIds.add(w.id);
            totalDuration += (w.endTime - w.startTime);

            List<WorkoutExercise> wes = repository.getWorkoutExercisesSync(w.id);
            for (WorkoutExercise we : wes) {
                Exercise e = repository.getExerciseById(we.exerciseId);
                String name = (e == null) ? "(удалено)" : e.name;
                int type = (e == null) ? 1 : e.type;

                String key = buildGroupKey(name, type, we);

                HistoryItem.ExerciseDetail existing = grouped.get(key);

                if (existing == null) {
                    HistoryItem.ExerciseDetail d = new HistoryItem.ExerciseDetail();
                    d.name = name;
                    d.type = type;
                    d.weight = we.weight;
                    d.reps = we.reps;
                    d.setsCount = we.setsCount;
                    d.repsList = we.repsList; // ← новое
                    d.timeSeconds = we.timeSeconds;
                    d.distanceMeters = we.distanceMeters;
                    d.level = we.level;
                    d.skipped = we.skipped;
                    grouped.put(key, d);
                } else {
                    // Одинаковые — суммируем подходы и склеиваем списки.
                    if (existing.setsCount != null && we.setsCount != null) {
                        existing.setsCount += we.setsCount;
                    } else if (we.setsCount != null) {
                        existing.setsCount = we.setsCount;
                    }
                    // Склеиваем repsList через запятую.
                    if (we.repsList != null && !we.repsList.isEmpty()) {
                        if (existing.repsList == null || existing.repsList.isEmpty()) {
                            existing.repsList = we.repsList;
                        } else {
                            existing.repsList = existing.repsList + "," + we.repsList;
                        }
                    }
                }



            }
        }

        item.totalDuration = totalDuration;
        item.details.addAll(grouped.values());

        // Сортируем по алфавиту (без учёта регистра).
        item.details.sort((a, b) -> a.name.compareToIgnoreCase(b.name));

        return item;
    }

    /**
     * Ключ для группировки: одинаковые упражнения с одинаковыми параметрами
     * объединяются. Упражнения с разными весами/повторами/временем — отдельно.
     */
    private String buildGroupKey(String name, int type, WorkoutExercise we) {
        if (we.skipped) return "SKIPPED|" + name;
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("|").append(type).append("|");
        switch (type) {
            case 1:
                sb.append(we.weight).append("|").append(we.reps);
                break;
            case 2:
                // Группируем по списку подходов целиком, чтобы "10,10" и "9,9"
                // не слились в одну строку.
                sb.append(we.repsList != null ? we.repsList : String.valueOf(we.reps));
                break;
            case 3:
                sb.append(we.timeSeconds);
                break;
            case 4:
                sb.append(we.timeSeconds).append("|")
                        .append(we.distanceMeters).append("|")
                        .append(we.level);
                break;
        }
        return sb.toString();
    }

    /** Превращает "2026-09-11" в "СЕНТЯБРЬ 2026". */
    private String formatMonth(String date) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date d = in.parse(date);
            SimpleDateFormat out = new SimpleDateFormat("LLLL yyyy", new Locale("ru"));
            return out.format(d).toUpperCase(new Locale("ru"));
        } catch (Exception e) {
            return date;
        }
    }

    // ================================================================
    // ============       ТЕКСТ ДЛЯ КОПИРОВАНИЯ           =============
    // ================================================================

    /**
     * Сформировать текст для буфера обмена.
     * Пропущенные упражнения идут отдельным блоком.
     */
    public static String buildCopyText(HistoryItem item) {
        StringBuilder sb = new StringBuilder();
        sb.append("Тренировка\n");
        sb.append("Дата: ")
                .append(DateUtils.formatDate(item.startTime)).append(" ")
                .append(DateUtils.formatTime(item.startTime)).append("\n");
        sb.append("Длительность: ")
                .append(DateUtils.formatDuration(item.startTime, item.startTime + item.totalDuration))
                .append("\n\n");

        sb.append("Упражнения:\n");
        int num = 1;
        for (HistoryItem.ExerciseDetail d : item.details) {
            if (d.skipped) continue;
            sb.append(num++).append(". ").append(d.formatForCopy()).append("\n");
        }

        List<HistoryItem.ExerciseDetail> skipped = new ArrayList<>();
        for (HistoryItem.ExerciseDetail d : item.details) {
            if (d.skipped) skipped.add(d);
        }
        if (!skipped.isEmpty()) {
            sb.append("\nПропущено:\n");
            for (HistoryItem.ExerciseDetail d : skipped) {
                sb.append("- ").append(d.name).append("\n");
            }
        }
        return sb.toString();
    }
}
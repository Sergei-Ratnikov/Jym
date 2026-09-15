package com.example.jym.ui.training;

import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.WorkoutExercise;

/**
 * Модель строки упражнения на экране тренировки.
 * Здесь хранится всё, что нужно для отображения и логики:
 *  - сам объект упражнения (название, тип);
 *  - последние значения (что делал в прошлый раз) — для подстановки;
 *  - текущие значения (что пользователь ввёл сегодня);
 *  - флаги активности и источника.
 */
public class ExerciseItem {

    /** Тип источника: упражнение из текущего сета. */
    public static final int SOURCE_SET = 0;
    /** Тип источника: упражнение из истории (is_custom = true) → «Бывало раньше». */
    public static final int SOURCE_PAST = 1;
    /** Тип источника: обычное упражнение из справочника → «Все упражнения». */
    public static final int SOURCE_ALL = 2;

    /** Базовые данные упражнения. */
    public Exercise exercise;

    /** Позиция в сете (0, 1, 2…). Для упражнений не из сета = -1. */
    public int setPosition = -1;

    /** Откуда пришло упражнение. */
    public int sourceBlock = SOURCE_ALL;

    // ============ СОСТОЯНИЕ НА ЭКРАНЕ ============

    /** Пользователь активировал это упражнение (жирная строка). */
    public boolean isActive = false;

    /** Когда-либо активировалось в этой сессии (для варианта Б). */
    public boolean wasActivated = false;

    /** Порядковый номер активации — для сортировки (больше = позже). */
    public int activationOrder = 0;

    /**
     * Флаг «шаблон».
     * true  — это шаблон из блока «Все упражнения». Клик по нему
     *         НЕ переключает, а создаёт новую запись в «Сегодня».
     * false — обычная строка (из сета, из истории или уже добавленная).
     */
    public boolean isTemplate = false;


    // ============ ТЕКУЩИЕ ЗНАЧЕНИЯ ============

    public Integer weight;         // вес (кг)
    public Integer reps;           // повторы
    public Integer setsCount;      // подходы
    public Integer timeSeconds;    // время (с)
    public Integer distanceMeters; // расстояние (м)
    public Integer level;          // уровень нагрузки


    /** Список повторений для типа 2 (10, 10, 9, 8). */
    public java.util.List<Integer> repsList = new java.util.ArrayList<>();


    /**
     * Заполнить значения из прошлой записи (WorkoutExercise).
     * Если прошлой записи нет — оставляем всё null.
     */
    public void applyLastValues(WorkoutExercise last) {
        if (last == null) return;
        this.weight = last.weight;
        this.reps = last.reps;
        this.setsCount = last.setsCount;
        this.timeSeconds = last.timeSeconds;
        this.distanceMeters = last.distanceMeters;
        this.level = last.level;

        // Восстанавливаем список подходов для типа 2.
        if (exercise != null && exercise.type == 2 && last.repsList != null
                && !last.repsList.isEmpty()) {
            this.repsList.clear();
            for (String s : last.repsList.split(",")) {
                try {
                    this.repsList.add(Integer.parseInt(s.trim()));
                } catch (NumberFormatException ignored) { }
            }
        }
    }

    /**
     * Собрать объект WorkoutExercise из текущих значений,
     * чтобы сохранить в историю.
     */
    public WorkoutExercise toWorkoutExercise() {
        android.util.Log.d("JYM_SAVE",
                "name=" + exercise.name
                        + " type=" + exercise.type
                        + " w=" + weight
                        + " r=" + reps
                        + " s=" + setsCount
                        + " t=" + timeSeconds
                        + " d=" + distanceMeters
                        + " l=" + level);

        WorkoutExercise we = new WorkoutExercise();
        we.exerciseId = exercise.id;
        we.weight = weight;
        we.setsCount = setsCount;
        we.timeSeconds = timeSeconds;
        we.distanceMeters = distanceMeters;
        we.level = level;
        we.skipped = false;
        we.isCustom = (sourceBlock != SOURCE_SET);

        if (exercise.type == 2 && !repsList.isEmpty()) {
            // Тип 2: список подходов.
            StringBuilder sb = new StringBuilder();
            int max = 0;
            for (int i = 0; i < repsList.size(); i++) {
                Integer v = repsList.get(i);
                if (v == null) continue;
                if (i > 0) sb.append(",");
                sb.append(v);
                if (v > max) max = v;
            }
            we.repsList = sb.toString();
            we.reps = max; // совместимость со статистикой
            we.setsCount = repsList.size();
        } else {
            // Остальные типы — как раньше.
            we.reps = reps;
        }
        return we;
    }



    /**
     * Создать упражнение-шаблон для блока «Все упражнения».
     * Такие элементы НЕ хранятся в общем списке allItems,
     * а создаются каждый раз при пересборке списка.
     */
    public static ExerciseItem template(Exercise exercise) {
        ExerciseItem it = new ExerciseItem();
        it.exercise = exercise;
        it.sourceBlock = SOURCE_ALL;
        it.isTemplate = true;
        return it;
    }
}
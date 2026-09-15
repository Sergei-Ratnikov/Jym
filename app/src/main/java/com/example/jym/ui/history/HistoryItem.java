package com.example.jym.ui.history;

import java.util.ArrayList;
import java.util.List;

/**
 * Модель ОДНОГО ДНЯ истории.
 * Может содержать одну или несколько тренировок, выполненных в этот день.
 * Всё, что нужно для отображения, уже просчитано в ViewModel:
 *  - объединённое время начала / конца / суммарная длительность;
 *  - список упражнений со суммированными подходами;
 *  - список workout_id для удаления.
 */
public class HistoryItem {

    public String date;                  // "yyyy-MM-dd"
    public long startTime;               // самое раннее время начала
    public long endTime;                 // самое позднее время окончания
    public long totalDuration;           // суммарная длительность в миллисекундах

    /** ID всех тренировок этого дня — для удаления. */
    public List<Integer> workoutIds = new ArrayList<>();

    /** Упражнения дня (объединённые + отсортированные по алфавиту). */
    public List<ExerciseDetail> details = new ArrayList<>();

    /**
     * Деталь одного упражнения внутри объединённого дня.
     */
    public static class ExerciseDetail {
        public String name;
        public int type;
        public Integer weight;
        public Integer reps;
        public String repsList;  // "10,10,9,8" для типа 2
        public Integer setsCount;
        public Integer timeSeconds;
        public Integer distanceMeters;
        public Integer level;
        public boolean skipped;

        /** Значения в читаемом виде в зависимости от типа. */
        public String formatValues() {
            StringBuilder sb = new StringBuilder();
            if (type == 1) {
                sb.append(weight == null ? "—" : weight).append(" кг × ")
                        .append(reps == null ? "—" : reps).append(" × ")
                        .append(setsCount == null ? "—" : setsCount);
            } else if (type == 2) {
                if (repsList != null && !repsList.isEmpty()) {
                    // Показываем как "10, 10, 9, 8".
                    sb.append(repsList.replace(",", ", "));
                } else if (reps != null && setsCount != null) {
                    // Старый формат для совместимости.
                    sb.append(reps).append(" повт × ").append(setsCount);
                } else {
                    sb.append("—");
                }
            } else if (type == 3) {
                sb.append(formatTime(timeSeconds)).append(" × ")
                        .append(setsCount == null ? "—" : setsCount).append(" подх");
            } else if (type == 4) {
                sb.append(distanceMeters == null ? "—" : distanceMeters).append(" м × ")
                        .append(formatTime(timeSeconds)).append(" × ")
                        .append(level == null ? "—" : level).append(" ур");
            }


            return sb.toString();
        }

        public String formatForCopy() {
            return name + " — " + formatValues();
        }

        /**
         * Форматирует время: "45 сек", "3 мин", "3 мин 30 сек".
         */
        private String formatTime(Integer totalSeconds) {
            if (totalSeconds == null) return "—";
            int m = totalSeconds / 60;
            int s = totalSeconds % 60;
            if (m == 0) return s + " с";
            if (s == 0) return m + " мин";
            return m + " мин " + s + " с";
        }
    }
}
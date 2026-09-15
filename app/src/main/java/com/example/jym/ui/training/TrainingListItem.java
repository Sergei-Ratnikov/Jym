package com.example.jym.ui.training;

/**
 * Элемент списка на экране тренировки.
 *
 * Может быть либо заголовком блока («СЕГОДНЯ», «БЫВАЛО РАНЬШЕ», «ВСЕ УПРАЖНЕНИЯ»),
 * либо строкой упражнения.
 */
public class TrainingListItem {

    public static final int TYPE_HEADER = 0;
    public static final int TYPE_EXERCISE = 1;

    public int type;         // TYPE_HEADER или TYPE_EXERCISE
    public String title;     // для заголовка
    public ExerciseItem item; // для строки упражнения

    /** Создать заголовок блока. */
    public static TrainingListItem header(String title) {
        TrainingListItem li = new TrainingListItem();
        li.type = TYPE_HEADER;
        li.title = title;
        return li;
    }

    /** Создать строку упражнения. */
    public static TrainingListItem exercise(ExerciseItem item) {
        TrainingListItem li = new TrainingListItem();
        li.type = TYPE_EXERCISE;
        li.item = item;
        return li;
    }
}
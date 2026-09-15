package com.example.jym.ui.statistics;

/**
 * Одна точка на графике прогресса.
 * Например: дата=1731234567890, значение=80.
 */
public class ProgressPoint {
    public long date;
    public float value;

    public ProgressPoint(long date, float value) {
        this.date = date;
        this.value = value;
    }
}
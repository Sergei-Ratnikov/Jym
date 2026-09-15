package com.example.jym.data.dao;

/**
 * Вспомогательный класс для подсчёта упражнений в сетах.
 * Используется в SQL-запросе с GROUP BY.
 * Поля должны называться точно так же, как в SELECT ... AS ...
 */
public class SetCount {
    public int setId;
    public int count;
}
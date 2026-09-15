package com.example.jym.ui.history;

/**
 * Элемент списка на экране истории.
 * Может быть либо заголовком месяца, либо карточкой одного дня.
 */
public class HistoryListItem {

    public static final int TYPE_MONTH = 0;
    public static final int TYPE_DAY = 1;

    public int type;
    public String monthTitle;   // для TYPE_MONTH: "СЕНТЯБРЬ 2026"
    public HistoryItem day;     // для TYPE_DAY

    public static HistoryListItem month(String title) {
        HistoryListItem li = new HistoryListItem();
        li.type = TYPE_MONTH;
        li.monthTitle = title;
        return li;
    }

    public static HistoryListItem day(HistoryItem day) {
        HistoryListItem li = new HistoryListItem();
        li.type = TYPE_DAY;
        li.day = day;
        return li;
    }
}
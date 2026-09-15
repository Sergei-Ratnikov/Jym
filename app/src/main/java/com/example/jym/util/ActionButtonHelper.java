package com.example.jym.util;

import android.content.res.ColorStateList;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.example.jym.R;
import com.google.android.material.button.MaterialButton;

/**
 * Помощник для настройки универсальной кнопки действия в нижней панели.
 * Используется фрагментами: TrainingFragment, SetsFragment, SetEditorFragment,
 * ExercisesFragment.
 */
public class ActionButtonHelper {

    /**
     * Настроить кнопку под конкретный экран.
     * @param root      корневой View (обычно — decorView activity)
     * @param text      текст кнопки
     * @param colorRes  ID ресурса цвета (например, R.color.primary_green)     Если передать 0 — используется цвет темы по умолчанию.
     * @param onClick   обработчик клика
     */
    public static void setup(View root, String text, int colorRes, View.OnClickListener onClick) {
        MaterialButton btn = root.findViewById(R.id.button_action);
        if (btn == null) return;

        btn.setText(text);
        if (colorRes != 0) {
            btn.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(root.getContext(), colorRes)));
        } else {
            // Сбрасываем на стандартный цвет темы.
            btn.setBackgroundTintList(null);
        }
        btn.setOnClickListener(onClick);
        btn.setVisibility(View.VISIBLE);
    }

    /** Скрыть кнопку. */
    public static void hide(View root) {
        MaterialButton btn = root.findViewById(R.id.button_action);
        if (btn != null) {
            btn.setVisibility(View.INVISIBLE);
            btn.setOnClickListener(null);
        }
    }
}
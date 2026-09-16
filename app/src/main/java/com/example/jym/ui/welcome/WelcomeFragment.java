package com.example.jym.ui.welcome;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.jym.MainActivity;
import com.example.jym.R;
import com.google.android.material.button.MaterialButton;

/**
 * Экран приветствия, показывается при первом запуске приложения.
 * Содержит:
 *  - описание приложения;
 *  - галочку «Больше не показывать»;
 *  - кнопку «Начать».
 */
public class WelcomeFragment extends Fragment {

    /** Имя файла настроек и ключ флага «Больше не показывать». */
    private static final String PREFS = "jym_prefs";
    private static final String KEY_WELCOME_SKIP = "welcome_skip";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CheckBox checkDontShow = view.findViewById(R.id.check_dont_show);
        MaterialButton buttonStart = view.findViewById(R.id.button_start);

        buttonStart.setOnClickListener(v -> {
            // Если пользователь поставил галочку — сохраняем флаг.
            if (checkDontShow.isChecked()) {
                SharedPreferences p = requireContext()
                        .getSharedPreferences(PREFS, Context.MODE_PRIVATE);
                p.edit().putBoolean(KEY_WELCOME_SKIP, true).apply();
            }
            // Переходим на экран тренировки.
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showScreen(MainActivity.Screen.TRAINING);
            }
        });
    }

    /**
     * Проверяет, надо ли показывать приветствие.
     * Возвращает true, если экран ещё не был показан (или пользователь
     * не ставил галочку «Больше не показывать»).
     */
    public static boolean shouldShow(Context context) {
        SharedPreferences p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return !p.getBoolean(KEY_WELCOME_SKIP, false);
    }
}
package com.example.jym;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

/**
 * Нижнее меню (аналог кнопки «Пуск»).
 * Открывается как BottomSheet — выезжает снизу.
 * Показывает 5 пунктов — по одному на каждый экран.
 * Текущий экран показывается серым и неактивным.
 */
public class MenuBottomSheet extends BottomSheetDialogFragment {

    /**
     * Слушатель выбора пункта меню.
     * MainActivity передаёт сюда свой код, который переключит экран.
     */
    public interface OnScreenSelected {
        void onSelected(MainActivity.Screen screen);
    }

    private final MainActivity.Screen currentScreen;
    private final OnScreenSelected listener;

    /**
     * @param currentScreen текущий открытый экран (будет серым)
     * @param listener      что делать при выборе пункта
     */
    public MenuBottomSheet(MainActivity.Screen currentScreen, OnScreenSelected listener) {
        this.currentScreen = currentScreen;
        this.listener = listener;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.bottom_sheet_menu, container, false);

        // Настраиваем каждый пункт.
        bindItem(view, R.id.menu_training, MainActivity.Screen.TRAINING);
        bindItem(view, R.id.menu_exercises, MainActivity.Screen.EXERCISES);
        bindItem(view, R.id.menu_sets, MainActivity.Screen.SETS);
        bindItem(view, R.id.menu_history, MainActivity.Screen.HISTORY);
        bindItem(view, R.id.menu_statistics, MainActivity.Screen.STATISTICS);

        return view;
    }

    /**
     * Настраивает один пункт меню.
     * Если пункт совпадает с текущим экраном — делает его серым и неактивным.
     * Иначе — вешает обработчик клика.
     */
    private void bindItem(View root, int viewId, MainActivity.Screen screen) {
        TextView tv = root.findViewById(viewId);

        if (screen == currentScreen) {
            // Текущий экран — серый, кликнуть нельзя.
            tv.setEnabled(false);
            tv.setAlpha(0.4f);
        } else {
            tv.setOnClickListener(v -> {
                listener.onSelected(screen);
                dismiss(); // закрываем меню
            });
        }
    }
}
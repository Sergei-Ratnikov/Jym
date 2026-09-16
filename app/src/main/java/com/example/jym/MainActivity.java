package com.example.jym;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.jym.ui.exercises.ExercisesFragment;
import com.example.jym.ui.history.HistoryFragment;
import com.example.jym.ui.sets.SetsFragment;
import com.example.jym.ui.statistics.StatisticsFragment;
import com.example.jym.ui.training.TrainingFragment;
import com.example.jym.ui.sets.SetEditorFragment;

import com.example.jym.data.Repository;
import com.example.jym.ui.welcome.WelcomeFragment;
import android.view.View;
/**
 * Главная (и единственная) Activity приложения.
 * Здесь:
 *  - FrameLayout, в который подставляются фрагменты (экраны).
 *  - Кнопка «Меню» внизу, которая открывает BottomSheet со списком экранов.
 * Такой подход называется Single Activity Architecture.
 * Все экраны — это фрагменты, которые заменяют друг друга в этом контейнере.
 */
public class MainActivity extends AppCompatActivity {

    /**
     * Перечисление всех экранов приложения.
     * Используется в меню для навигации.
     */
    public enum Screen {
        WELCOME,      // экран приветствия
        TRAINING,     // экран тренировки
        EXERCISES,    // экран упражнений
        SETS,         // экран сетов
        HISTORY,      // экран истории
        STATISTICS    // экран статистики
    }

    /** Текущий открытый экран. Нужен, чтобы подсветить его в меню как «активный». */
    private Screen currentScreen = Screen.TRAINING;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // При первом запуске добавляем базовые упражнения.
        // При последующих — ничего не делается.
        Repository.getInstance(this).addDefaultExercisesIfNeeded();

        // Кнопка «Меню». По нажатию открываем BottomSheet.
        Button buttonMenu = findViewById(R.id.button_menu);
        buttonMenu.setOnClickListener(v -> openMenu());

        // При первом запуске: если надо — показываем приветствие,
        // иначе — сразу экран тренировки.
        if (savedInstanceState == null) {
            if (WelcomeFragment.shouldShow(this)) {
                showScreen(Screen.WELCOME);
            } else {
                showScreen(Screen.TRAINING);
            }
        }
    }

    /**
     * Открывает нижнее меню (BottomSheet).
     * Передаём туда текущий экран, чтобы его можно было отобразить серым.
     */
    private void openMenu() {
        MenuBottomSheet menu = new MenuBottomSheet(currentScreen, this::showScreen);
        menu.show(getSupportFragmentManager(), "menu");
    }

    /**
     * Переключает фрагмент в контейнере.
     * Заменяет текущий фрагмент на новый.
     */
    public void showScreen(Screen screen) {
        currentScreen = screen;

        Fragment fragment;
        switch (screen) {
            case WELCOME:
                fragment = new WelcomeFragment();
                break;
            case EXERCISES:
                fragment = new ExercisesFragment();
                break;
            case SETS:
                fragment = new SetsFragment();
                break;
            case HISTORY:
                fragment = new HistoryFragment();
                break;
            case STATISTICS:
                fragment = new StatisticsFragment();
                break;
            case TRAINING:
            default:
                fragment = new TrainingFragment();
                break;
        }

        // Скрываем нижнюю панель с меню на экране приветствия.
        View bottomPanel = findViewById(R.id.bottom_panel);
        if (bottomPanel != null) {
            bottomPanel.setVisibility(
                    screen == Screen.WELCOME ? View.GONE : View.VISIBLE);
        }

        // Заменяем содержимое контейнера на новый фрагмент.
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }


    /**
     * Открыть редактор сета.
     * Используется, когда пользователь тапает на сет в списке.
     */
    public void openSetEditor(int setId) {
        SetEditorFragment fragment = SetEditorFragment.newInstance(setId);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null) // ← позволяет вернуться системной кнопкой «Назад»
                .commit();
    }

}
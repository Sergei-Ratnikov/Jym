package com.example.jym.ui.training;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.jym.R;


/**
 * Экран тренировки — главный экран приложения.
 */
public class TrainingFragment extends Fragment implements TrainingAdapter.Listener {

    private TrainingViewModel viewModel;
    private TrainingAdapter adapter;
    private TextView textSetName, textLastWorkout;
    /** Виден ли сейчас фрагмент (для управления кнопкой «Закончить»). */
    private boolean isResumed = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_training, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textSetName = view.findViewById(R.id.text_set_name);
        textLastWorkout = view.findViewById(R.id.text_last_workout);
        RecyclerView recycler = view.findViewById(R.id.recycler);

        adapter = new TrainingAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);
        // Скрытие клавиатуры при тапе по пустому месту внутри RecyclerView.
        setupRecyclerKeyboardHiding(recycler);

        // Привязываем ViewModel к Activity, чтобы она пережила
        // замену фрагмента (когда пользователь уходит в другие экраны
        // через меню). Иначе состояние тренировки терялось бы.
        viewModel = new ViewModelProvider(requireActivity()).get(TrainingViewModel.class);

        viewModel.getCurrentSetName().observe(getViewLifecycleOwner(), s ->
                textSetName.setText(s == null ? "" : s));

        viewModel.getLastWorkoutText().observe(getViewLifecycleOwner(), s ->
                textLastWorkout.setText(s == null ? "" : s));

        viewModel.getListItems().observe(getViewLifecycleOwner(), items ->
                adapter.submit(items));

        // Таймер.
        TextView textTimer = view.findViewById(R.id.text_timer);
        viewModel.getTimerText().observe(getViewLifecycleOwner(), s ->
                textTimer.setText(s == null ? "00:00" : s));

        // Видимость кнопки «Закончить».
        viewModel.getHasActiveExercises().observe(getViewLifecycleOwner(),
                hasActive -> updateFinishButton());

        // Кнопка «Сменить сет».
        view.findViewById(R.id.button_change_set).setOnClickListener(v -> showChangeSetDialog());

        // Первая загрузка.
        viewModel.load();
        // Скрытие клавиатуры при тапе по пустому месту.
        setupKeyboardHiding(view);
        // Проверка черновика (только один раз за жизнь ViewModel).
        checkDraftOnce();
    }

    // ================================================================
    // ==============         СМЕНА СЕТА              =================
    // ================================================================
    private void showChangeSetDialog() {
        // Лямбда, которая откроет диалог выбора сета.
        // Сама загрузка сетов теперь асинхронная.
        Runnable pickSet = () -> viewModel.loadAllSetsAsync(sets -> {
            if (!isAdded()) return;

            if (sets == null || sets.isEmpty()) {
                Toast.makeText(getContext(),
                        "Сначала соберите хотя бы одну тренировку (Меню -> Тренировки)", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[sets.size()];
            for (int i = 0; i < sets.size(); i++) {
                names[i] = sets.get(i).name;
            }

            new AlertDialog.Builder(requireContext())
                    .setTitle("Выберите тренировку")
                    .setItems(names, (d, which) -> viewModel.changeSet(sets.get(which).id))
                    .setNegativeButton("Отмена", null)
                    .show();
        });

        // Если пользователь уже что-то активировал — предупреждаем.
        if (viewModel.isAnyActivated()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle("Тренировка не завершена")
                    .setMessage("Изменения будут потеряны. Переключиться?")
                    .setPositiveButton("Продолжить", (d, w) -> pickSet.run())
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            pickSet.run();
        }
    }

    // ================================================================
    // ==============         ЗАКОНЧИТЬ               =================
    // ================================================================
    private void confirmFinish() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Завершить тренировку?")
                .setMessage("Активные упражнения будут сохранены в историю")
                .setPositiveButton("Завершить", (d, w) -> {
                    viewModel.finishWorkout(() -> {
                        if (!isAdded()) return;
                        Toast.makeText(getContext(),
                                "Тренировка сохранена", Toast.LENGTH_SHORT).show();
                        // Обновляем экран — новый сет по очереди.
                        viewModel.resetForNewWorkout();
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ================================================================
    // ==============   КЛИК ПО СТРОКЕ УПРАЖНЕНИЯ     =================
    // ================================================================
    @Override
    public void onItemClick(ExerciseItem item) {
        if (item.isTemplate) {
            // Тап по шаблону из «Все упражнения» — добавляем НОВУЮ запись.
            viewModel.addFromTemplate(item.exercise);
        } else {
            // Обычная строка — активация / деактивация.
            viewModel.toggleExercise(item);
        }
    }


    /**
     * Вызывается из MainActivity при нажатии на кнопку «🏁 Закончить»
     * в нижней панели. Открывает диалог подтверждения.
     */
    public void onFinishButtonClicked() {
        confirmFinish();
    }

    @Override
    public void onResume() {
        super.onResume();
        isResumed = true;
        updateFinishButton();
    }

    @Override
    public void onStop() {
        super.onStop();
        isResumed = false;
        viewModel.saveDraft();
        com.example.jym.util.ActionButtonHelper.hide(requireActivity().getWindow().getDecorView());
    }

    /**
     * Кнопка действия видна ТОЛЬКО когда:
     *  - фрагмент тренировки на экране (isResumed);
     *  - есть хотя бы одно активное упражнение.
     * Когда активных нет — кнопка просто скрывается.
     */
    private void updateFinishButton() {
        if (!isAdded() || !isResumed) return;
        Boolean hasActive = viewModel.getHasActiveExercises().getValue();

        if (Boolean.TRUE.equals(hasActive)) {
            com.example.jym.util.ActionButtonHelper.setup(
                    requireActivity().getWindow().getDecorView(),
                    "Закончить",
                    R.color.primary_green,
                    v -> confirmFinish()
            );
        } else {
            com.example.jym.util.ActionButtonHelper.hide(
                    requireActivity().getWindow().getDecorView());
        }
    }


    /**
     * Скрывает клавиатуру при тапе по пустому месту ВНЕ RecyclerView
     * (например, по верхней панели с названием тренировки).
     */

    @SuppressLint("ClickableViewAccessibility")
    private void setupKeyboardHiding(View root) {
        root.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboardIfOpen();
            }
            return false;
        });
    }

    /**
     * Скрывает клавиатуру при тапе по пустому месту ВНУТРИ RecyclerView
     * (по строке упражнения, но не по полю ввода)
     * Использует OnItemTouchListener — это единственный надёжный способ
     * перехватить тапы внутри элементов RecyclerView, потому что элементы
     * поглощают событие и оно не доходит до корневого View.
     */
    private void setupRecyclerKeyboardHiding(RecyclerView recycler) {
        recycler.addOnItemTouchListener(new RecyclerView.SimpleOnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv,
                                                 @NonNull MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    View focused = requireActivity().getCurrentFocus();
                    if (focused instanceof EditText) {
                        // Проверяем: попал ли тап в САМ фокусный EditText.
                        // Если да — не скрываем (пользователь продолжает ввод).
                        // Если нет — скрываем.
                        int[] loc = new int[2];
                        focused.getLocationOnScreen(loc);
                        float sx = e.getRawX();
                        float sy = e.getRawY();
                        boolean onEditText =
                                sx >= loc[0] && sx < loc[0] + focused.getWidth() &&
                                        sy >= loc[1] && sy < loc[1] + focused.getHeight();
                        if (!onEditText) {
                            hideKeyboardIfOpen();
                        }
                    }
                }
                return false; // не перехватываем — событие идёт дальше
            }
        });
    }

    /**
     * Прячет клавиатуру, если она открыта.
     */
    private void hideKeyboardIfOpen() {
        View focused = requireActivity().getCurrentFocus();
        if (focused instanceof EditText) {
            InputMethodManager imm = (InputMethodManager)
                    requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
            }
            focused.clearFocus();
        }
    }
    /**
     * Проверить наличие черновика и спросить пользователя.
     * Показывается один раз за жизнь ViewModel (а не при каждом возврате).
     */
    private void checkDraftOnce() {
        if (viewModel.isDraftChecked()) return;
        viewModel.setDraftChecked(true);

        if (!viewModel.hasDraft()) return;

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Продолжить тренировку?")
                .setMessage("Найдена незавершённая тренировка. Продолжить её?")
                .setPositiveButton("Продолжить", (d, w) -> viewModel.restoreDraft())
                .setNegativeButton("Начать заново", (d, w) -> viewModel.clearDraft())
                .setCancelable(false)
                .show();
    }
}
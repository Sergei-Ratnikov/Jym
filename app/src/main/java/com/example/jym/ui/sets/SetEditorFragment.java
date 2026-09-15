package com.example.jym.ui.sets;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.SetExercise;
import com.example.jym.data.entity.WorkoutSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Редактор одной тренировки.
 * Создаётся через newInstance(setId). Внутри:
 *  - редактируемое название тренировки в верхней панели;
 *  - список упражнений (drag & drop, удаление);
 *  - кнопка «+ Добавить» → диалог выбора упражнения из справочника;
 *  - кнопка «← Назад» → возврат к списку тренировок (системная кнопка тоже работает).
 */
public class SetEditorFragment extends Fragment implements SetExercisesAdapter.Listener {

    private static final String ARG_SET_ID = "set_id";

    private int setId;
    private SetEditorViewModel viewModel;
    private SetExercisesAdapter adapter;
    private EditText editSetName;
    private TextView textEmpty;
    private RecyclerView recycler;

    /** Кэш имён упражнений — чтобы не запрашивать БД на каждую строку. */
    private final Map<Integer, String> nameCache = new HashMap<>();
    private final Map<Integer, Integer> typeCache = new HashMap<>();

    /** Флаг, чтобы не запускать обработчик текста до окончания загрузки названия. */
    private boolean suppressNameUpdate = false;

    public static SetEditorFragment newInstance(int setId) {
        SetEditorFragment f = new SetEditorFragment();
        Bundle b = new Bundle();
        b.putInt(ARG_SET_ID, setId);
        f.setArguments(b);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_set_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            setId = getArguments().getInt(ARG_SET_ID);
        }

        editSetName = view.findViewById(R.id.edit_set_name);
        textEmpty = view.findViewById(R.id.text_empty);
        recycler = view.findViewById(R.id.recycler);

        // Стрелка «назад» (крестик) — сохраняет название и выходит.
        view.findViewById(R.id.button_back).setOnClickListener(v -> {
            saveAndClose();
        });


        // === ViewModel ===
        viewModel = new ViewModelProvider(this).get(SetEditorViewModel.class);
        viewModel.setSetId(setId);

        // Загружаем сведения о тренировке (название).
        viewModel.loadSet(set -> {
            if (!isAdded() || set == null) return;
            requireActivity().runOnUiThread(() -> {
                suppressNameUpdate = true;
                editSetName.setText(set.name);
                suppressNameUpdate = false;
            });
        });

        // Автосохранение названия при каждом изменении.
        editSetName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) {}
            @Override
            public void afterTextChanged(Editable s) {
                if (suppressNameUpdate) return;
                String newName = s.toString().trim();
                if (newName.isEmpty()) return;
                WorkoutSet ws = new WorkoutSet();
                ws.id = setId;
                ws.name = newName;
                // orderIndex не меняем — берём из БД и сохраним его при следующей загрузке.
                // Проще: отдельный UPDATE только name.
                viewModel.updateSetNameOnly(setId, newName);
            }
        });

        // === Адаптер ===
        adapter = new SetExercisesAdapter(nameCache, typeCache, this);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        // === Drag & drop ===
        ItemTouchHelper.SimpleCallback dragCallback =
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder t) {
                        adapter.moveItem(vh.getAdapterPosition(), t.getAdapterPosition());
                        return true;
                    }
                    @Override public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int d) {}
                    @Override
                    public void clearView(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh) {
                        super.clearView(rv, vh);
                        viewModel.updateExercisesOrder(adapter.getItems());
                    }
                };
        new ItemTouchHelper(dragCallback).attachToRecyclerView(recycler);

        // === Подписка на список упражнений в тренировке ===
        viewModel.getExercisesInSet().observe(getViewLifecycleOwner(), list -> {
            // Сначала подгружаем имена упражнений (если их нет в кэше).
            loadNamesFor(list, () -> {
                adapter.submit(list);
                boolean empty = list == null || list.isEmpty();
                textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
                // RecyclerView НЕ скрываем — он просто пустой.
                // Так нижний блок с кнопкой «Готово» всегда остаётся внизу.
            });
        });
    }

    /**
     * Подгружает имена упражнений в кэш для всех ID, которых там ещё нет.
     * Затем вызывает onDone().
     */
    private void loadNamesFor(List<SetExercise> list, Runnable onDone) {
        if (list == null) {
            onDone.run();
            return;
        }
        List<Integer> missing = new ArrayList<>();
        for (SetExercise se : list) {
            if (!nameCache.containsKey(se.exerciseId)) missing.add(se.exerciseId);
        }
        if (missing.isEmpty()) {
            onDone.run();
            return;
        }
        // Загружаем синхронно в фоне.
        new Thread(() -> {
            for (int id : missing) {
                Exercise e = viewModel.getExerciseById(id);
                if (e != null) {
                    nameCache.put(id, e.name);
                    typeCache.put(id, e.type);
                } else {
                    nameCache.put(id, "(удалено)");
                    typeCache.put(id, 1);
                }
            }
            if (isAdded()) {
                requireActivity().runOnUiThread(onDone);
            }
        }).start();
    }

    /** Диалог выбора упражнения из справочника. */
    private void showPickerDialog() {
        // Берём все активные упражнения. Используем observe один раз.
        viewModel.getAllActiveExercises().observe(getViewLifecycleOwner(), exercises -> {
            // Отпишемся сразу после первого получения — это одноразовый запрос.
            // (простой способ для новичка)

            if (exercises == null || exercises.isEmpty()) {
                Toast.makeText(getContext(),
                        "В справочнике нет упражнений", Toast.LENGTH_SHORT).show();
                return;
            }

            String[] names = new String[exercises.size()];
            for (int i = 0; i < exercises.size(); i++) {
                Exercise e = exercises.get(i);
                names[i] = ExercisesAdapterHelper.iconFor(e.type) + "  " + e.name;
            }

            new AlertDialog.Builder(getContext())
                    .setTitle("Выберите упражнение")
                    .setItems(names, (d, which) -> {
                        Exercise e = exercises.get(which);
                        viewModel.addExerciseToSet(e.id, success -> {
                            if (!success) {
                                requireActivity().runOnUiThread(() ->
                                        Toast.makeText(getContext(),
                                                "Это упражнение уже в тренировке",
                                                Toast.LENGTH_SHORT).show());
                            }
                        });
                    })
                    .setNegativeButton("Отмена", null)
                    .show();
        });
    }

    @Override
    public void onDelete(SetExercise item) {
        String name = nameCache.get(item.exerciseId);
        if (name == null) name = "упражнение";
        new AlertDialog.Builder(getContext())
                .setTitle("Убрать из тренировки?")
                .setMessage("\"" + name + "\" будет убрано из тренировки.\n" +
                        "В справочнике и в истории упражнение останется.")
                .setPositiveButton("Убрать", (d, w) ->
                        viewModel.removeExerciseFromSet(item.exerciseId))
                .setNegativeButton("Отмена", null)
                .show();
    }

    /** Маленький помощник для иконок. */
    static class ExercisesAdapterHelper {
        static String iconFor(int type) {
            return switch (type) {
                case 1 -> "🏋";
                case 2 -> "🤸";
                case 3 -> "⏱";
                case 4 -> "🚣";
                default -> "🏋";
            };
        }
    }

    /**
     * Сохраняет текущее название тренировки и закрывает редактор.
     * Вызывается как по крестику, так и по системной кнопке «Назад».
     */
    private void saveAndClose() {
        // Принудительно сохраняем название — на случай, если TextWatcher
        // не успел сработать (пользователь ввёл и сразу тапнул назад).
        if (editSetName != null) {
            String currentName = editSetName.getText().toString().trim();
            if (!currentName.isEmpty()) {
                viewModel.updateSetNameOnly(setId, currentName);
            }
        }
        // Закрываем фрагмент через системный back.
        requireActivity().getOnBackPressedDispatcher().onBackPressed();
    }

    /**
     * При уходе с экрана (в том числе по системной кнопке «Назад»)
     * на всякий случай сохраняем название.
     */
    @Override
    public void onStop() {
        super.onStop();
        com.example.jym.util.ActionButtonHelper.hide(
                requireActivity().getWindow().getDecorView());
        // Сохраняем название (уже было раньше).
        if (editSetName != null) {
            String currentName = editSetName.getText().toString().trim();
            if (!currentName.isEmpty()) {
                viewModel.updateSetNameOnly(setId, currentName);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        com.example.jym.util.ActionButtonHelper.setup(
                requireActivity().getWindow().getDecorView(),
                "+ Упражнение",
                R.color.primary_green,
                v -> showPickerDialog()
        );
    }

}
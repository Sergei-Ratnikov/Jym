package com.example.jym.ui.exercises;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.data.entity.Exercise;

/**
 * Экран «Упражнения» — справочник.
 * Что умеет:
 *  - показывать список всех упражнений (активных и архивных);
 *  - добавлять новые упражнения;
 *  - редактировать существующие;
 *  - удалять (архивировать, если есть история);
 *  - восстанавливать из архива.
 */
public class ExercisesFragment extends Fragment implements ExercisesAdapter.Listener {

    private ExercisesViewModel viewModel;
    private ExercisesAdapter adapter;
    private TextView textEmpty;
    private RecyclerView recycler;

    /** Названия типов для выпадающего списка в диалоге. */
    private static final String[] TYPE_NAMES = {
            "1. Вес + повторения",
            "2. Без веса + повторы",
            "3. Время + подходы",
            "4. Расстояние + время + нагрузка"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_exercises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Находим View-элементы.
        textEmpty = view.findViewById(R.id.text_empty);
        recycler = view.findViewById(R.id.recycler);

        // Создаём адаптер и вешаем на RecyclerView.
        adapter = new ExercisesAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        // Получаем ViewModel. Она привязывается к фрагменту и живёт, пока он жив.
        viewModel = new ViewModelProvider(this).get(ExercisesViewModel.class);

        // Подписываемся на список. Когда данные меняются — обновляем список.
        viewModel.getExercises().observe(getViewLifecycleOwner(), exercises -> {
            adapter.submit(exercises);
            boolean empty = exercises == null || exercises.isEmpty();
            textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

    }

    // ================================================================
    // ===============       ДИАЛОГ ДОБАВЛЕНИЯ        =================
    // ================================================================
    private void showAddDialog() {
        // Инфлейтим разметку dialog_exercise.xml.
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_exercise, null);
        EditText editName = v.findViewById(R.id.edit_name);
        Spinner spinner = v.findViewById(R.id.spinner_type);

        // Настраиваем Spinner — выпадающий список типов.
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, TYPE_NAMES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);

        new AlertDialog.Builder(getContext())
                .setTitle("Новое упражнение")
                .setView(v)
                .setPositiveButton("Сохранить", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Введите название", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int type = spinner.getSelectedItemPosition() + 1; // 1..4

                    viewModel.addExercise(name, type, success -> {
                        if (!success) {
                            requireActivity().runOnUiThread(() ->
                                    Toast.makeText(getContext(),
                                            "Такое упражнение уже есть",
                                            Toast.LENGTH_SHORT).show());
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // ================================================================
    // ===============     ДИАЛОГ РЕДАКТИРОВАНИЯ      =================
    // ================================================================
    private void showEditDialog(Exercise exercise) {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_exercise, null);
        EditText editName = v.findViewById(R.id.edit_name);
        Spinner spinner = v.findViewById(R.id.spinner_type);

        editName.setText(exercise.name);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                getContext(), android.R.layout.simple_spinner_item, TYPE_NAMES);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(spinnerAdapter);
        spinner.setSelection(exercise.type - 1);

        // Проверяем, есть ли история — если есть, тип менять нельзя.
        viewModel.hasHistory(exercise.id, has -> {
            requireActivity().runOnUiThread(() -> {
                if (has) {
                    spinner.setEnabled(false);
                }

                new AlertDialog.Builder(getContext())
                        .setTitle("Редактировать упражнение")
                        .setView(v)
                        .setPositiveButton("Сохранить", (d, w) -> {
                            String name = editName.getText().toString().trim();
                            if (name.isEmpty()) {
                                Toast.makeText(getContext(), "Введите название",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                            exercise.name = name;
                            if (!has) {
                                exercise.type = spinner.getSelectedItemPosition() + 1;
                            }
                            viewModel.updateExercise(exercise);
                        })
                        .setNegativeButton("Отмена", null)
                        .show();
            });
        });
    }

    // ================================================================
    // ===============          УДАЛЕНИЕ              =================
    // ================================================================

    /**
     * Для активного упражнения — проверяем историю:
     *  - есть → предлагаем архивировать;
     *  - нет  → предлагаем удалить безвозвратно.
     */
    private void showDeleteDialog(Exercise exercise) {
        viewModel.hasHistory(exercise.id, has -> {
            requireActivity().runOnUiThread(() -> {
                if (has) {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Архивировать?")
                            .setMessage("Упражнение \"" + exercise.name +
                                    "\" имеет историю. Оно будет перемещено в архив.")
                            .setPositiveButton("Архивировать", (d, w) ->
                                    viewModel.deleteExercise(exercise, null))
                            .setNegativeButton("Отмена", null)
                            .show();
                } else {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Удалить?")
                            .setMessage("Упражнение \"" + exercise.name +
                                    "\" будет удалено безвозвратно.")
                            .setPositiveButton("Удалить", (d, w) ->
                                    viewModel.deleteExercise(exercise, null))
                            .setNegativeButton("Отмена", null)
                            .show();
                }
            });
        });
    }

    // ================================================================
    // ===============  ОБРАБОТЧИКИ КНОПОК СТРОКИ    ==================
    // ================================================================

    /**
     * ✏️ для активного — открыть редактирование.
     * ♻️ для архивного — восстановить.
     */
    @Override
    public void onEdit(Exercise exercise) {
        if (exercise.archived) {
            viewModel.restoreExercise(exercise);
        } else {
            showEditDialog(exercise);
        }
    }

    /**
     * 🗑️ для активного — проверить историю и удалить/архивировать.
     * 🗑️ для архивного — удалить безвозвратно (с подтверждением).
     */
    @Override
    public void onDelete(Exercise exercise) {
        if (exercise.archived) {
            new AlertDialog.Builder(getContext())
                    .setTitle("Удалить навсегда?")
                    .setMessage("Упражнение \"" + exercise.name + "\" исчезнет из справочника. " +
                            "История останется.")
                    .setPositiveButton("Удалить", (d, w) ->
                            viewModel.deleteExercisePermanently(exercise.id))
                    .setNegativeButton("Отмена", null)
                    .show();
        } else {
            showDeleteDialog(exercise);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        com.example.jym.util.ActionButtonHelper.setup(
                requireActivity().getWindow().getDecorView(),
                "+ Упражнение",
                R.color.primary_green,
                v -> showAddDialog()
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        com.example.jym.util.ActionButtonHelper.hide(
                requireActivity().getWindow().getDecorView());
    }

}
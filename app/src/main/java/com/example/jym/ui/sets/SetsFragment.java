package com.example.jym.ui.sets;

import android.os.Bundle;
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

import com.example.jym.MainActivity;
import com.example.jym.R;
import com.example.jym.data.entity.WorkoutSet;

import java.util.List;

/**
 * Экран «Сеты».
 */
public class SetsFragment extends Fragment implements SetsAdapter.Listener {

    private SetsViewModel viewModel;
    private SetsAdapter adapter;
    private RecyclerView recycler;
    private TextView textEmpty;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sets, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recycler = view.findViewById(R.id.recycler);
        textEmpty = view.findViewById(R.id.text_empty);

        adapter = new SetsAdapter(this);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        // === DRAG & DROP ===
        // ItemTouchHelper позволяет перетаскивать элементы за долгое нажатие.
        ItemTouchHelper.SimpleCallback dragCallback =
                new ItemTouchHelper.SimpleCallback(
                        ItemTouchHelper.UP | ItemTouchHelper.DOWN,
                        0 // без свайпов
                ) {
                    @Override
                    public boolean onMove(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh,
                                          @NonNull RecyclerView.ViewHolder target) {
                        int from = vh.getAdapterPosition();
                        int to = target.getAdapterPosition();
                        adapter.moveItem(from, to);
                        return true;
                    }

                    @Override
                    public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                        // Не используется.
                    }

                    @Override
                    public void clearView(@NonNull RecyclerView rv,
                                          @NonNull RecyclerView.ViewHolder vh) {
                        super.clearView(rv, vh);
                        // Когда пользователь отпустил — сохраняем новый порядок в БД.
                        viewModel.updateSetsOrder(adapter.getItems());
                    }
                };
        new ItemTouchHelper(dragCallback).attachToRecyclerView(recycler);

        // === ViewModel ===
        viewModel = new ViewModelProvider(this).get(SetsViewModel.class);

        viewModel.getSets().observe(getViewLifecycleOwner(), sets -> {
            adapter.submit(sets);
            boolean empty = sets == null || sets.isEmpty();
            textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);

            // Загружаем количества упражнений.
            viewModel.loadCounts(counts -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> adapter.submitCounts(counts));
                }
            });
        });
    }

    /** Диалог создания нового сета. */
    private void showAddDialog() {
        View v = LayoutInflater.from(getContext()).inflate(R.layout.dialog_set_name, null);
        EditText editName = v.findViewById(R.id.edit_name);

        new AlertDialog.Builder(getContext())
                .setTitle("Новая тренировка")
                .setView(v)
                .setPositiveButton("Создать", (d, w) -> {
                    String name = editName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(getContext(), "Введите название", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    viewModel.addSet(name, newId -> {
                        // Сразу открываем редактор нового сета.
                        if (isAdded()) {
                            requireActivity().runOnUiThread(() -> {
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).openSetEditor(newId);
                                }
                            });
                        }
                    });
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    // === Действия в строке ===

    @Override
    public void onEdit(WorkoutSet set) {
        // Открываем редактор сета.
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).openSetEditor(set.id);
        }
    }

    @Override
    public void onToggleActive(WorkoutSet set) {
        // Тап по названию — переключаем активность.
        viewModel.toggleSetActive(set.id, !set.active);
    }

    @Override
    public void onDelete(WorkoutSet set) {
        new AlertDialog.Builder(getContext())
                .setTitle("Удалить тренировку?")
                .setMessage("Тренировка \"" + set.name + "\" будет удалена.\n" +
                        "Упражнения в справочнике и история выполненных тренировок не пострадают.")
                .setPositiveButton("Удалить", (d, w) -> viewModel.deleteSet(set.id))
                .setNegativeButton("Отмена", null)
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        com.example.jym.util.ActionButtonHelper.setup(
                requireActivity().getWindow().getDecorView(),
                "+ Тренировка",
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
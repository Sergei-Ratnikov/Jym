package com.example.jym.ui.history;
import com.example.jym.ui.history.HistoryItem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;

/**
 * Экран «История».
 * Показывает список завершённых тренировок в виде карточек.
 * Каждая карточка — с кнопкой «Копировать».
 */
public class HistoryFragment extends Fragment {

    private HistoryViewModel viewModel;
    private HistoryAdapter adapter;
    private TextView textEmpty;
    private RecyclerView recycler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        textEmpty = view.findViewById(R.id.text_empty);
        recycler = view.findViewById(R.id.recycler);

        adapter = new HistoryAdapter(this::showDeleteDialog);
        recycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(HistoryViewModel.class);

        viewModel.getItems().observe(getViewLifecycleOwner(), items -> {
            adapter.submit(items);
            boolean empty = items == null || items.isEmpty();
            textEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.load();
    }

    /**
     * Диалог подтверждения удаления записи дня.
     */
    private void showDeleteDialog(HistoryItem item) {
        int count = item.workoutIds.size();
        String msg = (count == 1)
                ? "Тренировка за этот день будет удалена. Это действие нельзя отменить."
                : "За этот день найдено " + count + " тренировки. Все они будут удалены. Это действие нельзя отменить.";

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Удалить запись?")
                .setMessage(msg)
                .setPositiveButton("Удалить", (d, w) -> viewModel.deleteDay(item))
                .setNegativeButton("Отмена", null)
                .show();
    }
}
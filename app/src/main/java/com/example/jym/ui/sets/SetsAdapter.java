package com.example.jym.ui.sets;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.data.entity.WorkoutSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Адаптер списка сетов.
 * Поддерживает drag & drop (через ItemTouchHelper в самом фрагменте).
 */
public class SetsAdapter extends RecyclerView.Adapter<SetsAdapter.VH> {

    public interface Listener {
        void onEdit(WorkoutSet set);        // карандаш — открыть редактор
        void onDelete(WorkoutSet set);      // корзина — удалить
        void onToggleActive(WorkoutSet set); // тап по названию — вкл/выкл
    }

    private List<WorkoutSet> items = new ArrayList<>();
    private Map<Integer, Integer> counts = new HashMap<>();
    private final Listener listener;

    public SetsAdapter(Listener listener) {
        this.listener = listener;
    }

    /** Передать новый список. */
    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<WorkoutSet> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    /** Обновить карту количеств упражнений. */
    @SuppressLint("NotifyDataSetChanged")
    public void submitCounts(Map<Integer, Integer> newCounts) {
        this.counts = newCounts;
        notifyDataSetChanged();
    }

    /** Понадобится при drag & drop. */
    public List<WorkoutSet> getItems() {
        return items;
    }

    /** Двигаем элементы местами в списке (для drag & drop). */
    public void moveItem(int from, int to) {
        WorkoutSet moved = items.remove(from);
        items.add(to, moved);
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_set, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position), position);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    class VH extends RecyclerView.ViewHolder {
        TextView number, name, count;
        com.google.android.material.button.MaterialButton edit, delete;

        VH(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.text_number);
            name = itemView.findViewById(R.id.text_name);
            count = itemView.findViewById(R.id.text_count);
            edit = itemView.findViewById(R.id.button_edit);
            delete = itemView.findViewById(R.id.button_delete);
        }

        @SuppressLint("SetTextI18n")
        void bind(WorkoutSet s, int position) {
            number.setText((position + 1) + ".");
            name.setText(s.name);

            // Количество упражнений из карты.
            Integer c = counts.get(s.id);
            int cnt = (c == null) ? 0 : c;
            count.setText(pluralizeExercises(cnt));

            // Неактивные — серые и полупрозрачные.
            if (s.active) {
                itemView.setAlpha(1f);
                name.setAlpha(1f);
                count.setAlpha(0.6f);
            } else {
                itemView.setAlpha(0.5f);
                name.setAlpha(0.7f);
                count.setAlpha(0.4f);
            }

            // Сброс старых слушателей (RecyclerView переиспользует ViewHolder).
            itemView.setOnClickListener(null);
            itemView.setClickable(false);
            name.setOnClickListener(null);

            // Карандаш и корзина — как было.
            edit.setOnClickListener(v -> listener.onEdit(s));
            delete.setOnClickListener(v -> listener.onDelete(s));

            // Тап по НАЗВАНИЮ — переключает активность.
            name.setOnClickListener(v -> listener.onToggleActive(s));
        }
    }

    /** Правильные окончания: 1 упражнение, 2 упражнения, 5 упражнений. */
    public static String pluralizeExercises(int n) {
        int lastDigit = n % 10;
        int lastTwo = n % 100;
        if (lastTwo >= 11 && lastTwo <= 14) return n + " упражнений";
        if (lastDigit == 1) return n + " упражнение";
        if (lastDigit >= 2 && lastDigit <= 4) return n + " упражнения";
        return n + " упражнений";
    }
}
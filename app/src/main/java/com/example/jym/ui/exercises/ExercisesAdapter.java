package com.example.jym.ui.exercises;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.data.entity.Exercise;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер списка упражнений.
 * - Тап по названию → редактирование (или восстановление, если упражнение в архиве).
 * - Кнопка 🗑 (красная) → удаление/архивация.
 * - Иконок типов нет, тип показывается текстом под названием.
 */
public class ExercisesAdapter extends RecyclerView.Adapter<ExercisesAdapter.VH> {

    public interface Listener {
        void onEdit(Exercise exercise);
        void onDelete(Exercise exercise);
    }

    private List<Exercise> items = new ArrayList<>();
    private final Listener listener;

    public ExercisesAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Exercise> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_exercise, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    /** Название типа для отображения. */
    public static String getTypeName(int type) {
        return switch (type) {
            case 1 -> "Масса + повторения + подходы";
            case 2 -> "Повторения + подходы";
            case 3 -> "Время + подходы";
            case 4 -> "Расстояние + время + нагрузка";
            default -> "";
        };
    }

    class VH extends RecyclerView.ViewHolder {
        TextView name, type;
        MaterialButton delete;

        VH(View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.text_name);
            type = itemView.findViewById(R.id.text_type);
            delete = itemView.findViewById(R.id.button_delete);
        }

        void bind(Exercise e) {
            // Название с пометкой для архивных.
            name.setText(e.archived ? e.name + " (архив)" : e.name);
            type.setText(getTypeName(e.type));

            // Архивные — полупрозрачные.
            itemView.setAlpha(e.archived ? 0.55f : 1f);

            // Клик по НАЗВАНИЮ → редактирование/восстановление.
            name.setOnClickListener(v -> listener.onEdit(e));

            // Кнопка удаления.
            delete.setOnClickListener(v -> listener.onDelete(e));
        }
    }
}
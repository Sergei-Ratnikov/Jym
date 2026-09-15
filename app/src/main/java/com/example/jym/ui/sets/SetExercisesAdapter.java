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
import com.example.jym.data.entity.SetExercise;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер списка упражнений внутри одного сета.
 */
public class SetExercisesAdapter extends RecyclerView.Adapter<SetExercisesAdapter.VH> {

    public interface Listener {
        void onDelete(SetExercise item);
    }

    /** Кэш: exerciseId → name. Нужен, чтобы не дёргать БД на каждую строку. */
    private final java.util.Map<Integer, String> nameCache;

    private List<SetExercise> items = new ArrayList<>();
    private final Listener listener;

    public SetExercisesAdapter(java.util.Map<Integer, String> nameCache,
                               java.util.Map<Integer, Integer> typeCache,
                               Listener listener) {
        this.nameCache = nameCache;
        this.listener = listener;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<SetExercise> list) {
        this.items = list;
        notifyDataSetChanged();
    }

    public List<SetExercise> getItems() {
        return items;
    }

    public void moveItem(int from, int to) {
        SetExercise moved = items.remove(from);
        items.add(to, moved);
        notifyItemMoved(from, to);
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_set_exercise, parent, false);
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
        TextView number, name;
        com.google.android.material.button.MaterialButton delete;

        VH(View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.text_number);
            name = itemView.findViewById(R.id.text_name);
            delete = itemView.findViewById(R.id.button_delete);
        }

        @SuppressLint("SetTextI18n")
        void bind(SetExercise se, int position) {
            number.setText((position + 1) + ".");
            String n = nameCache.get(se.exerciseId);
            if (n == null) n = "(упражнение " + se.exerciseId + ")";
            name.setText(n);
            delete.setOnClickListener(v -> listener.onDelete(se));
        }
    }
}
package com.example.jym.ui.history;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.util.DateUtils;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер истории. Различает два типа элементов:
 *  - заголовок месяца (крупный текст);
 *  - карточку дня с кнопками «Копировать» (жёлтая) и «Удалить» (красная).
 */
public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** Колбэк для удаления дня. */
    public interface Listener {
        void onDelete(HistoryItem item);
    }

    private List<HistoryListItem> items = new ArrayList<>();
    private final Listener listener;

    public HistoryAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<HistoryListItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == HistoryListItem.TYPE_MONTH) {
            View v = inf.inflate(R.layout.item_history_month, parent, false);
            return new MonthVH(v);
        } else {
            View v = inf.inflate(R.layout.item_history_workout, parent, false);
            return new DayVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        HistoryListItem li = items.get(position);
        if (li.type == HistoryListItem.TYPE_MONTH) {
            ((MonthVH) holder).bind(li.monthTitle);
        } else {
            ((DayVH) holder).bind(li.day);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ================================================================
    // =====================  VIEW HOLDER: МЕСЯЦ  =====================
    // ================================================================
    static class MonthVH extends RecyclerView.ViewHolder {
        TextView text;
        MonthVH(View v) {
            super(v);
            text = v.findViewById(R.id.text_month);
        }
        void bind(String title) {
            text.setText(title);
        }
    }

    // ================================================================
    // =====================  VIEW HOLDER: ДЕНЬ  ======================
    // ================================================================
    class DayVH extends RecyclerView.ViewHolder {
        TextView textDate, textDuration;
        LinearLayout layoutExercises;
        MaterialButton buttonCopy, buttonDelete;

        DayVH(View v) {
            super(v);
            textDate = v.findViewById(R.id.text_date);
            textDuration = v.findViewById(R.id.text_duration);
            layoutExercises = v.findViewById(R.id.layout_exercises);
            buttonCopy = v.findViewById(R.id.button_copy);
            buttonDelete = v.findViewById(R.id.button_delete);
        }

        void bind(HistoryItem item) {
            // Дата и время.
            String date = DateUtils.formatDate(item.startTime)
                    + "  " + DateUtils.formatTime(item.startTime)
                    + " — " + DateUtils.formatTime(item.endTime);
            textDate.setText(date);

            // Длительность.
            String dur = "(" + DateUtils.formatDuration(
                    item.startTime, item.startTime + item.totalDuration) + ")";
            textDuration.setText(dur);

            // Упражнения.
            layoutExercises.removeAllViews();
            LayoutInflater inf = LayoutInflater.from(itemView.getContext());
            for (HistoryItem.ExerciseDetail d : item.details) {
                View row = inf.inflate(R.layout.item_history_exercise,
                        layoutExercises, false);
                TextView marker = row.findViewById(R.id.text_marker);
                TextView name   = row.findViewById(R.id.text_name);
                TextView values = row.findViewById(R.id.text_values);

                if (d.skipped) {
                    marker.setText("✕");
                    marker.setAlpha(0.5f);
                    name.setAlpha(0.5f);
                    values.setText("(пропущено)");
                    values.setAlpha(0.5f);
                } else {
                    marker.setText("✓");
                    marker.setAlpha(1f);
                    name.setAlpha(1f);
                    values.setAlpha(0.7f);
                    values.setText(d.formatValues());
                }
                name.setText(d.name);

                layoutExercises.addView(row);
            }

            // Кнопка «Копировать».
            buttonCopy.setOnClickListener(v -> {
                String text = HistoryViewModel.buildCopyText(item);
                ClipboardManager cm = (ClipboardManager) itemView.getContext()
                        .getSystemService(Context.CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Jym", text));
                Toast.makeText(itemView.getContext(),
                        "Скопировано в буфер обмена", Toast.LENGTH_SHORT).show();
            });

            // Кнопка «Удалить».
            buttonDelete.setOnClickListener(v -> listener.onDelete(item));
        }
    }
}
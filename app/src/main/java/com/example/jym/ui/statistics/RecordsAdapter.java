package com.example.jym.ui.statistics;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.util.DateUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер списка личных рекордов.
 */
public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.VH> {

    private List<PersonalRecord> items = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    public void submit(List<PersonalRecord> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
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

    static class VH extends RecyclerView.ViewHolder {
        TextView textExercise, textDate, textRecord;

        VH(View v) {
            super(v);
            textExercise = v.findViewById(R.id.text_exercise);
            textDate = v.findViewById(R.id.text_date);
            textRecord = v.findViewById(R.id.text_record);
        }

        void bind(PersonalRecord pr) {
            textExercise.setText(pr.exerciseName);
            textDate.setText(DateUtils.formatDate(pr.date));
            textRecord.setText(pr.recordText);
        }
    }
}
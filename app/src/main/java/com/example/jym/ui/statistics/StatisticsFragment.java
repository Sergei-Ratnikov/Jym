package com.example.jym.ui.statistics;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;
import com.example.jym.data.entity.Exercise;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Экран «Статистика» с тремя вкладками:
 *  1. Прогресс — линейный график по выбранному упражнению.
 *  2. Рекорды — список лучших результатов.
 *  3. Общая — цифры и столбчатая диаграмма.
 */
public class StatisticsFragment extends Fragment {

    private StatisticsViewModel viewModel;

    // Прогресс
    private Spinner spinnerExercise;
    private LineChart lineChart;
    private TextView textProgressEmpty;
    private List<Exercise> exerciseList = new ArrayList<>();

    // Рекорды
    private RecordsAdapter recordsAdapter;
    private RecyclerView recordsRecycler;
    private TextView textRecordsEmpty;

    // Общая
    private TextView textTotalWorkouts, textTotalVolume;
    private BarChart chartMonths;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_statistics, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(StatisticsViewModel.class);

        TabLayout tabs = view.findViewById(R.id.tabs);
        ViewGroup container = view.findViewById(R.id.tab_container);

        // Создаём три вкладки.
        View tabProgress = LayoutInflater.from(getContext())
                .inflate(R.layout.tab_progress, container, false);
        View tabRecords = LayoutInflater.from(getContext())
                .inflate(R.layout.tab_records, container, false);
        View tabGeneral = LayoutInflater.from(getContext())
                .inflate(R.layout.tab_general, container, false);

        // Инициализация вкладки «Прогресс»
        spinnerExercise = tabProgress.findViewById(R.id.spinner_exercise);
        lineChart = tabProgress.findViewById(R.id.chart);
        textProgressEmpty = tabProgress.findViewById(R.id.text_progress_empty);
        setupLineChart();

        // Вкладка «Рекорды»
        recordsRecycler = tabRecords.findViewById(R.id.recycler_records);
        textRecordsEmpty = tabRecords.findViewById(R.id.text_records_empty);
        recordsAdapter = new RecordsAdapter();
        recordsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        recordsRecycler.setAdapter(recordsAdapter);

        // Вкладка «Общая»
        textTotalWorkouts = tabGeneral.findViewById(R.id.text_total_workouts);
        textTotalVolume = tabGeneral.findViewById(R.id.text_total_volume);
        chartMonths = tabGeneral.findViewById(R.id.chart_months);
        setupBarChart();

        // Показываем вкладку «Прогресс» по умолчанию.
        container.addView(tabProgress);

        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                container.removeAllViews();
                switch (tab.getPosition()) {
                    case 0: container.addView(tabProgress); break;
                    case 1: container.addView(tabRecords); break;
                    case 2: container.addView(tabGeneral); break;
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        // === Подписка на LiveData ===

        viewModel.getExercises().observe(getViewLifecycleOwner(), list -> {
            exerciseList = list == null ? new ArrayList<>() : list;
            setupSpinner();
        });

        viewModel.getProgressPoints().observe(getViewLifecycleOwner(), points -> {
            if (points == null || points.isEmpty()) {
                lineChart.setVisibility(View.GONE);
                textProgressEmpty.setVisibility(View.VISIBLE);
                return;
            }
            lineChart.setVisibility(View.VISIBLE);
            textProgressEmpty.setVisibility(View.GONE);
            showLineChart(points);
        });

        viewModel.getRecords().observe(getViewLifecycleOwner(), list -> {
            recordsAdapter.submit(list);
            boolean empty = list == null || list.isEmpty();
            textRecordsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            recordsRecycler.setVisibility(empty ? View.GONE : View.VISIBLE);
        });

        viewModel.getTotalWorkouts().observe(getViewLifecycleOwner(), n ->
                textTotalWorkouts.setText(String.valueOf(n == null ? 0 : n)));

        viewModel.getTotalVolume().observe(getViewLifecycleOwner(), v ->
                textTotalVolume.setText(String.valueOf(v == null ? 0L : v)));

        viewModel.getWorkoutsByMonth().observe(getViewLifecycleOwner(), map -> {
            if (map != null) showBarChart(map);
        });

        // === Загрузка ===
        viewModel.loadExercises();
        viewModel.loadRecords();
        viewModel.loadGeneral();
    }

    // ================================================================
    // ==============           SPINNER               =================
    // ================================================================
    private void setupSpinner() {
        if (exerciseList.isEmpty()) return;

        String[] names = new String[exerciseList.size()];
        for (int i = 0; i < exerciseList.size(); i++) {
            names[i] = exerciseList.get(i).name;
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(), android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerExercise.setAdapter(adapter);

        spinnerExercise.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                viewModel.loadProgress(exerciseList.get(position).id);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // ================================================================
    // ==============         ЛИНЕЙНЫЙ ГРАФИК         =================
    // ================================================================
    private void setupLineChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.getXAxis().setGranularity(1f);
        lineChart.setNoDataText("Нет данных");
    }

    private void showLineChart(List<ProgressPoint> points) {
        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();

        for (int i = 0; i < points.size(); i++) {
            ProgressPoint p = points.get(i);
            entries.add(new Entry(i, p.value));
            labels.add(com.example.jym.util.DateUtils.formatDate(p.date));
        }

        LineDataSet set = new LineDataSet(entries, "");
        set.setColor(Color.parseColor("#4CAF50"));
        set.setCircleColor(Color.parseColor("#4CAF50"));
        set.setLineWidth(2f);
        set.setCircleRadius(4f);
        set.setValueTextSize(11f);

        lineChart.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        lineChart.getXAxis().setLabelRotationAngle(-45f);
        lineChart.setData(new LineData(set));
        lineChart.invalidate();
    }

    // ================================================================
    // ==============      СТОЛБЧАТАЯ ДИАГРАММА       =================
    // ================================================================
    private void setupBarChart() {
        chartMonths.getDescription().setEnabled(false);
        chartMonths.getAxisRight().setEnabled(false);
        chartMonths.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chartMonths.getXAxis().setGranularity(1f);
        chartMonths.setNoDataText("Нет данных");
    }

    private void showBarChart(Map<String, Integer> map) {
        // Сортируем месяцы по ключу (yyyy-MM).
        List<String> keys = new ArrayList<>(map.keySet());
        java.util.Collections.sort(keys);

        // Оставляем последние 6 месяцев.
        if (keys.size() > 6) keys = keys.subList(keys.size() - 6, keys.size());

        List<BarEntry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Integer cnt = map.get(key);
            entries.add(new BarEntry(i, cnt == null ? 0 : cnt));
            labels.add(key);
        }

        BarDataSet set = new BarDataSet(entries, "Тренировок");
        set.setColor(Color.parseColor("#4CAF50"));
        set.setValueTextSize(11f);
        set.setValueFormatter(new ValueFormatter() {
            @Override public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        chartMonths.getXAxis().setValueFormatter(new IndexAxisValueFormatter(labels));
        chartMonths.setData(new BarData(set));
        chartMonths.invalidate();
    }
}
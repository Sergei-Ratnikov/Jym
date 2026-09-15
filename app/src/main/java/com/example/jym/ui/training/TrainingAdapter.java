package com.example.jym.ui.training;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.jym.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Адаптер экрана тренировки.
 * Показывает:
 *  - заголовки блоков (СЕГОДНЯ / БЫВАЛО РАНЬШЕ / ВСЕ УПРАЖНЕНИЯ);
 *  - строки упражнений (активные — с полями, неактивные — с текстом значений).
 */
public class TrainingAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public interface Listener {
        void onItemClick(ExerciseItem item);
    }

    private List<TrainingListItem> items = new ArrayList<>();
    private final Listener listener;

    public TrainingAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<TrainingListItem> newItems) {
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
        if (viewType == TrainingListItem.TYPE_HEADER) {
            View v = inf.inflate(R.layout.item_training_header, parent, false);
            return new HeaderVH(v);
        } else {
            View v = inf.inflate(R.layout.item_training_exercise, parent, false);
            return new ExerciseVH(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        TrainingListItem li = items.get(position);
        if (li.type == TrainingListItem.TYPE_HEADER) {
            ((HeaderVH) holder).bind(li.title);
        } else {
            ((ExerciseVH) holder).bind(li.item);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ================================================================
    // ========================  HEADER VH  ==========================
    // ================================================================
    static class HeaderVH extends RecyclerView.ViewHolder {
        TextView text;
        HeaderVH(View v) {
            super(v);
            text = v.findViewById(R.id.text_header);
        }
        void bind(String title) {
            text.setText(title);
        }
    }

    // ================================================================
    // =======================  EXERCISE VH  ==========================
    // ================================================================
    class ExerciseVH extends RecyclerView.ViewHolder {

        // Все View строки
        TextView name, valuesText;
        LinearLayout fields, fields1, fields2, fields3, fields4;

        EditText weight, reps, sets, sets3, distance, level;
        EditText timeMin, timeSec;
        EditText time4Min, time4Sec;
        android.widget.ImageButton buttonPlusSets, buttonPlusSets3;
        android.widget.ImageButton buttonAddRep;
        android.widget.LinearLayout repsContainer;

        /** Текущий привязанный item. Хранится, чтобы watcher писал в него. */
        private ExerciseItem boundItem;

        /** Флаг, чтобы игнорировать watcher при программной установке текста. */
        private boolean bindGuard = false;

        ExerciseVH(View v) {
            super(v);
            name       = v.findViewById(R.id.text_name);
            valuesText = v.findViewById(R.id.text_values);
            fields     = v.findViewById(R.id.layout_fields);
            fields1    = v.findViewById(R.id.fields_type1);
            fields2    = v.findViewById(R.id.fields_type2);
            fields3    = v.findViewById(R.id.fields_type3);
            fields4    = v.findViewById(R.id.fields_type4);

            // ⚠️ ВАЖНО: все поля должны быть найдены.
            weight   = v.findViewById(R.id.edit_weight);
            reps     = v.findViewById(R.id.edit_reps);
            sets     = v.findViewById(R.id.edit_sets);
            repsContainer = v.findViewById(R.id.reps_container);
            buttonAddRep  = v.findViewById(R.id.button_add_rep);

            timeMin   = v.findViewById(R.id.edit_time_min);
            timeSec   = v.findViewById(R.id.edit_time_sec);
            sets3     = v.findViewById(R.id.edit_sets3);
            time4Min  = v.findViewById(R.id.edit_time4_min);
            time4Sec  = v.findViewById(R.id.edit_time4_sec);
            distance  = v.findViewById(R.id.edit_distance);
            level     = v.findViewById(R.id.edit_level);

            buttonPlusSets  = v.findViewById(R.id.button_plus_sets);
            buttonPlusSets3 = v.findViewById(R.id.button_plus_sets3);
            // buttonPlusSets2 не нужен — вместо него теперь список подходов.

            attachWatchers();
            attachPlusButtons();
        }

        /**
         * Форматирует время в удобном виде: "45 сек", "3 мин", "3 мин 30 сек".
         */
        private String formatTime(Integer totalSeconds) {
            if (totalSeconds == null) return "—";
            int m = totalSeconds / 60;
            int s = totalSeconds % 60;
            if (m == 0) return s + " с";
            if (s == 0) return m + " мин";
            return m + " мин " + s + " с";
        }


        // ================================================================
        // =====================   WATCHERS   ============================
        // ================================================================

        private void attachWatchers() {
            TextWatcher watcher = new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(Editable s) {
                    if (bindGuard) return;
                    if (boundItem == null) return;
                    captureValues();
                }
            };

            // Навешиваем ТОЛЬКО на не-null поля.
            if (weight   != null) weight.addTextChangedListener(watcher);
            if (reps     != null) reps.addTextChangedListener(watcher);
            if (sets     != null) sets.addTextChangedListener(watcher);
            if (timeMin  != null) timeMin.addTextChangedListener(watcher);
            if (timeSec  != null) timeSec.addTextChangedListener(watcher);
            if (sets3    != null) sets3.addTextChangedListener(watcher);
            if (time4Min != null) time4Min.addTextChangedListener(watcher);
            if (time4Sec != null) time4Sec.addTextChangedListener(watcher);
            if (distance != null) distance.addTextChangedListener(watcher);
            if (level    != null) level.addTextChangedListener(watcher);
        }

        /**
         * Навешивает обработчики на кнопки «+».
         * При нажатии увеличивает соответствующее поле подходов на 1.
         */
        private void attachPlusButtons() {
            if (buttonPlusSets != null) {
                buttonPlusSets.setOnClickListener(v -> incrementSets(sets));
            }

            if (buttonPlusSets3 != null) {
                buttonPlusSets3.setOnClickListener(v -> incrementSets(sets3));
            }
        }

        /**
         * Увеличивает значение поля подходов на 1.
         * Если поле пустое — ставит 1.
         * Максимум — 99 (ограничение по ширине и по maxLength).
         */
        private void incrementSets(EditText field) {
            if (field == null || boundItem == null) return;

            int current = 0;
            String text = field.getText().toString().trim();
            if (!text.isEmpty()) {
                try { current = Integer.parseInt(text); }
                catch (NumberFormatException ignored) { current = 0; }
            }
            current++;
            if (current > 99) current = 99;

            field.setText(String.valueOf(current));
            // TextWatcher сработает и обновит boundItem через captureValues(),
            // но для надёжности вызываем явно.
            captureValues();
        }

        /**
         * Считывает значения ТОЛЬКО из полей, относящихся к текущему типу.
         * Так значения из скрытых полей не могут случайно перезаписать нужное.
         */
        private void captureValues() {
            if (boundItem == null) return;
            int t = boundItem.exercise.type;
            switch (t) {
                case 1: // вес + повторы + подходы
                    boundItem.weight    = parseIntOrNull(safeText(weight));
                    boundItem.reps      = parseIntOrNull(safeText(reps));
                    boundItem.setsCount = parseIntOrNull(safeText(sets));
                    break;

                case 2: // список подходов
                    boundItem.repsList = readRepsFromContainer();
                    int max = 0;
                    for (Integer v : boundItem.repsList) {
                        if (v != null && v > max) max = v;
                    }
                    boundItem.reps = boundItem.repsList.isEmpty() ? null : max;
                    boundItem.setsCount = boundItem.repsList.isEmpty() ? null : boundItem.repsList.size();
                    break;

                case 3: // время (мин + сек) + подходы
                    boundItem.timeSeconds = combineMinutesSeconds(timeMin, timeSec);
                    boundItem.setsCount   = parseIntOrNull(safeText(sets3));
                    break;

                case 4: // расстояние + время (мин + сек) + уровень
                    boundItem.timeSeconds    = combineMinutesSeconds(time4Min, time4Sec);
                    boundItem.distanceMeters = parseIntOrNull(safeText(distance));
                    boundItem.level          = parseIntOrNull(safeText(level));
                    break;
            }
        }

        private String safeText(EditText e) {
            return e == null ? "" : e.getText().toString();
        }

        /**
         * Объединяет два поля «мин» и «сек» в общее количество секунд.
         * Если оба пустые — возвращает null.
         */
        private Integer combineMinutesSeconds(EditText minField, EditText secField) {
            Integer m = parseIntOrNull(safeText(minField));
            Integer s = parseIntOrNull(safeText(secField));
            if (m == null && s == null) return null;
            int mm = (m == null) ? 0 : m;
            int ss = (s == null) ? 0 : s;
            return mm * 60 + ss;
        }

        /**
         * Разбивает общее количество секунд на минуты и секунды.
         * Возвращает массив [минуты, секунды]. Если seconds == null — [null, null].
         */
        private Integer[] splitMinutesSeconds(Integer totalSeconds) {
            if (totalSeconds == null) return new Integer[]{null, null};
            return new Integer[]{totalSeconds / 60, totalSeconds % 60};
        }




        // ================================================================
        // ========================   BIND   =============================
        // ================================================================

        @SuppressLint("SetTextI18n")
        void bind(ExerciseItem item) {
            boundItem = item;

            // ⚠️ ВАЖНО: сначала СБРАСЫВАЕМ слушатели со всех View,
            // потому что ViewHolder переиспользуется RecyclerView.
            itemView.setOnClickListener(null);
            itemView.setClickable(false);
            name.setOnClickListener(null);

            // ============================================================
            // 1. Шаблон из блока «Все упражнения» — тап по названию добавляет
            // ============================================================
            if (item.isTemplate) {
                name.setText("+ " + item.exercise.name);
                name.setTypeface(null, android.graphics.Typeface.ITALIC);
                name.setAlpha(0.75f);
                fields.setVisibility(View.GONE);
                valuesText.setVisibility(View.GONE);

                // Клик только по названию.
                name.setOnClickListener(v -> listener.onItemClick(item));
                return;
            }

            // ============================================================
            // 2. Обычная строка (из сета, из истории, из «Все упражнения»)
            // ============================================================
            name.setText(item.exercise.name);

            // Название: жирное/активное или серое/неактивное.
            if (item.isActive) {
                name.setTypeface(null, android.graphics.Typeface.BOLD);
                name.setAlpha(1f);
            } else {
                name.setTypeface(null, android.graphics.Typeface.NORMAL);
                name.setAlpha(0.55f);
            }

            // Показ полей или текста значений.
            if (item.isActive) {
                fields.setVisibility(View.VISIBLE);
                valuesText.setVisibility(View.GONE);

                fields1.setVisibility(View.GONE);
                fields2.setVisibility(View.GONE);
                fields3.setVisibility(View.GONE);
                fields4.setVisibility(View.GONE);

                switch (item.exercise.type) {
                    case 1: fields1.setVisibility(View.VISIBLE); break;
                    case 2: fields2.setVisibility(View.VISIBLE); break;
                    case 3: fields3.setVisibility(View.VISIBLE); break;
                    case 4: fields4.setVisibility(View.VISIBLE); break;
                }

                bindGuard = true;
                setIfNull(weight, item.weight);
                setIfNull(reps, item.reps);
                setIfNull(sets, item.setsCount);
                // Тип 2: список подходов заполняем в контейнер.
                if (item.exercise.type == 2) {
                    populateRepsContainer(item);
                }

                // Время типа 3 — разбиваем на минуты и секунды.
                Integer[] t3 = splitMinutesSeconds(item.timeSeconds);
                setIfNull(timeMin, t3[0]);
                setIfNull(timeSec, t3[1]);
                setIfNull(sets3, item.setsCount);

                // Время типа 4 — тоже разбиваем.
                Integer[] t4 = splitMinutesSeconds(item.timeSeconds);
                setIfNull(time4Min, t4[0]);
                setIfNull(time4Sec, t4[1]);
                setIfNull(distance, item.distanceMeters);
                setIfNull(level, item.level);

                bindGuard = false;

            } else {
                fields.setVisibility(View.GONE);
                valuesText.setVisibility(View.VISIBLE);
                valuesText.setText(buildValuesText(item));
            }

            // ⚠️ Единственный обработчик клика — только на названии.
            // Клик по полям ввода, по «×», по «повт», по пустому месту
            // НЕ переключает упражнение.
            name.setOnClickListener(v -> listener.onItemClick(item));
        }

        private void setIfNull(EditText e, Integer val) {
            if (e == null) return;
            e.setText(val == null ? "" : String.valueOf(val));
        }

        /**
         * Формирует читаемую строку значений для НЕАКТИВНОГО упражнения.
         */
        private String buildValuesText(ExerciseItem item) {
            int t = item.exercise.type;
            StringBuilder sb = new StringBuilder();

            if (t == 1) {
                sb.append(item.weight == null ? "—" : item.weight).append(" кг × ")
                        .append(item.reps == null ? "—" : item.reps).append(" × ")
                        .append(item.setsCount == null ? "—" : item.setsCount);

            } else if (t == 2) {
                // Показываем список подходов: "10, 10, 9, 8".
                if (!item.repsList.isEmpty()) {
                    StringBuilder rl = new StringBuilder();
                    for (int i = 0; i < item.repsList.size(); i++) {
                        if (i > 0) rl.append(", ");
                        rl.append(item.repsList.get(i));
                    }
                    sb.append(rl);
                } else {
                    sb.append("—");
                }
            } else if (t == 3) {
                // Время × подходы
                sb.append(formatTime(item.timeSeconds)).append(" × ")
                        .append(item.setsCount == null ? "—" : item.setsCount).append(" подх.");

            } else if (t == 4) {
                sb.append(item.distanceMeters == null ? "—" : item.distanceMeters).append(" м × ")
                        .append(formatTime(item.timeSeconds)).append(" × ")
                        .append(item.level == null ? "—" : item.level).append(" ур.");
            }

            return sb.toString();
        }

        private static Integer parseIntOrNull(String s) {
            if (s == null) return null;
            s = s.trim();
            if (s.isEmpty()) return null;
            try { return Integer.parseInt(s); }
            catch (NumberFormatException e) { return null; }
        }

        /**
         * Заполняет контейнер полями для каждого подхода.
         * Если список пуст — добавляет одно пустое поле (чтобы пользователь начал).
         */
        private void populateRepsContainer(ExerciseItem item) {
            if (repsContainer == null) return;

            // Полностью очищаем контейнер.
            repsContainer.removeAllViews();

            // Создаём поле на каждый подход.
            if (item.repsList.isEmpty()) {
                addRepField(null, true);
            } else {
                for (Integer v : item.repsList) {
                    addRepField(v, item.repsList.size() > 1);
                }
            }

            // Навешиваем обработчик на кнопку «+».
            if (buttonAddRep != null) {
                buttonAddRep.setOnClickListener(v -> {
                    addRepField(null, true);
                    captureValues();
                });
            }
        }

        /**
         * Добавляет одно поле подхода.
         * @param value    значение (может быть null для пустого)
         * @param removable показывать ли крестик для удаления
         */
        private void addRepField(Integer value, boolean removable) {
            if (repsContainer == null) return;
            android.view.View field = LayoutInflater.from(repsContainer.getContext())
                    .inflate(R.layout.item_rep_field, repsContainer, false);

            EditText edit = field.findViewById(R.id.edit_rep);
            edit.setText(value == null ? "" : String.valueOf(value));
            edit.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
                @Override public void afterTextChanged(android.text.Editable s) {
                    if (bindGuard || boundItem == null) return;
                    captureValues();
                }
            });

            android.widget.ImageButton removeBtn = field.findViewById(R.id.button_remove_rep);
            if (removable) {
                removeBtn.setOnClickListener(v -> {
                    repsContainer.removeView(field);
                    // Если ничего не осталось — добавим пустое поле.
                    if (repsContainer.getChildCount() == 0) {
                        addRepField(null, false);
                    }
                    captureValues();
                });
            } else {
                removeBtn.setVisibility(android.view.View.GONE);
            }

            repsContainer.addView(field);
        }

        /**
         * Считывает все значения из контейнера.
         */
        private java.util.List<Integer> readRepsFromContainer() {
            java.util.List<Integer> result = new java.util.ArrayList<>();
            if (repsContainer == null) return result;
            for (int i = 0; i < repsContainer.getChildCount(); i++) {
                android.view.View child = repsContainer.getChildAt(i);
                EditText edit = child.findViewById(R.id.edit_rep);
                Integer v = parseIntOrNull(edit.getText().toString());
                if (v != null) result.add(v);
            }
            return result;
        }
    }

}
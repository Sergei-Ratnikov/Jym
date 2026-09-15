package com.example.jym.data;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import com.example.jym.data.dao.ExerciseDao;
import com.example.jym.data.dao.SetExerciseDao;
import com.example.jym.data.dao.WorkoutDao;
import com.example.jym.data.dao.WorkoutExerciseDao;
import com.example.jym.data.dao.WorkoutSetDao;
import com.example.jym.data.dao.SetCount;
import com.example.jym.data.entity.Exercise;
import com.example.jym.data.entity.SetExercise;
import com.example.jym.data.entity.Workout;
import com.example.jym.data.entity.WorkoutExercise;
import com.example.jym.data.entity.WorkoutSet;
import com.example.jym.util.DateUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;


/**
 * Repository — прослойка между базой данных и остальным приложением.
 * Все операции записи/чтения идут через этот класс.
 * ВАЖНО: операции записи выполняются в ФОНОВОМ потоке (не в UI).
 * Для этого используем ExecutorService с одним потоком.
 * Почему один поток? Потому что все операции к БД — последовательные.
 * Если запускать несколько потоков, можно получить race condition (гонку).
 */
public class Repository {

    // === DAO (получим из AppDatabase) ===
    private final ExerciseDao exerciseDao;
    private final WorkoutSetDao workoutSetDao;
    private final SetExerciseDao setExerciseDao;
    private final WorkoutDao workoutDao;
    private final WorkoutExerciseDao workoutExerciseDao;

    /**
     * Executor с одним фоновым потоком.
     * Все операции записи будут выполняться здесь по очереди.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Контекст приложения — нужен для SharedPreferences. */
    private final Context appContext;

    /**
     * Singleton — Repository тоже один на всё приложение.
     */
    private static volatile Repository INSTANCE;

    /**
     * Приватный конструктор. Получаем DAO из базы данных.
     * Не вызывать напрямую — используйте getInstance().
     */
    private Repository(Context context) {
        this.appContext = context.getApplicationContext();
        AppDatabase db = AppDatabase.getInstance(context);
        this.exerciseDao = db.exerciseDao();
        this.workoutSetDao = db.workoutSetDao();
        this.setExerciseDao = db.setExerciseDao();
        this.workoutDao = db.workoutDao();
        this.workoutExerciseDao = db.workoutExerciseDao();
    }

    /**
     * Получить единственный экземпляр Repository.
     */
    public static Repository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (Repository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new Repository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    // ================================================================
    // ==============           УПРАЖНЕНИЯ           ==================
    // ================================================================

    /**
     * Получить все активные упражнения (для блока "Все упражнения").
     */
    public LiveData<List<Exercise>> getAllActiveExercises() {
        return exerciseDao.getAllActive();
    }

    /**
     * Получить все упражнения (активные + архивные) — для экрана "Упражнения".
     */
    public LiveData<List<Exercise>> getAllExercises() {
        return exerciseDao.getAllIncludingArchived();
    }

    /**
     * Найти упражнение по ID.
     * Выполняется в фоне, но так как метод используется внутри
     * других операций — оставляем синхронным.
     */
    public Exercise getExerciseById(int id) {
        return exerciseDao.getById(id);
    }

    /**
     * Добавить новое упражнение.
     * Проверяет на дубликат по названию.
     *
     * @param callback вызывается ПОСЛЕ завершения.
     *                 success = true, если добавлено; false, если дубликат.
     */
    public void addExercise(String name, int type, Callback<Boolean> callback) {
        executor.execute(() -> {
            // Проверяем дубликат.
            Exercise existing = exerciseDao.findByName(name);
            if (existing != null) {
                // Такой уже есть — сообщаем об ошибке.
                if (callback != null) callback.onResult(false);
                return;
            }
            // Создаём и вставляем.
            Exercise exercise = new Exercise(name, type);
            exerciseDao.insert(exercise);
            if (callback != null) callback.onResult(true);
        });
    }

    /**
     * Обновить упражнение.
     */
    public void updateExercise(Exercise exercise) {
        executor.execute(() -> exerciseDao.update(exercise));
    }

    /**
     * Удалить упражнение.
     * Логика:
     *  - если у упражнения НЕТ истории → удаляем полностью;
     *  - если ЕСТЬ история → ставим archived = true.
     *
     * @param callback сообщает результат:
     *                 true  — архивировано (была история),
     *                 false — удалено навсегда.
     */
    public void deleteExercise(Exercise exercise, Callback<Boolean> callback) {
        executor.execute(() -> {
            int historyCount = exerciseDao.countHistory(exercise.id);
            if (historyCount > 0) {
                // Есть история — архивируем.
                exercise.archived = true;
                exerciseDao.update(exercise);
                if (callback != null) callback.onResult(true);
            } else {
                // Нет истории — удаляем.
                exerciseDao.delete(exercise);
                if (callback != null) callback.onResult(false);
            }
        });
    }

    /**
     * Полностью удалить упражнение (для архивных, кнопка "Удалить навсегда").
     */
    public void deleteExercisePermanently(int exerciseId) {
        executor.execute(() -> exerciseDao.deleteById(exerciseId));
    }

    /**
     * Восстановить упражнение из архива.
     */
    public void restoreExercise(Exercise exercise) {
        executor.execute(() -> {
            exercise.archived = false;
            exerciseDao.update(exercise);
        });
    }

    /**
     * Проверить, есть ли у упражнения история (синхронно, в фоне).
     * Используется при открытии диалога редактирования — чтобы решить,
     * можно ли менять тип упражнения.
     */
    public void hasHistory(int exerciseId, Callback<Boolean> callback) {
        executor.execute(() -> {
            boolean has = exerciseDao.countHistory(exerciseId) > 0;
            if (callback != null) callback.onResult(has);
        });
    }

    // ================================================================
    // ==============              СЕТЫ               =================
    // ================================================================

    public LiveData<List<WorkoutSet>> getAllSets() {
        return workoutSetDao.getAll();
    }

    public WorkoutSet getSetById(int id) {
        return workoutSetDao.getById(id);
    }

    /**
     * Создать новый сет.
     * Он автоматически встаёт в конец очереди.
     *
     * @param callback возвращает ID созданного сета.
     */
    public void addSet(String name, Callback<Integer> callback) {
        executor.execute(() -> {
            // Узнаём максимальный order_index среди существующих сетов.
            Integer maxOrder = workoutSetDao.getMaxOrderIndex();
            int newOrder = (maxOrder == null) ? 0 : maxOrder + 1;

            // Создаём сет.
            WorkoutSet set = new WorkoutSet(name, newOrder);
            long newId = workoutSetDao.insert(set);

            if (callback != null) callback.onResult((int) newId);
        });
    }

    /**
     * Обновить сет (например, изменить название).
     */
    public void updateSet(WorkoutSet set) {
        executor.execute(() -> workoutSetDao.update(set));
    }

    /**
     * Обновить ТОЛЬКО название сета — удобно для автосохранения в редакторе.
     */
    public void updateSetNameOnly(int setId, String newName) {
        executor.execute(() -> {
            WorkoutSet s = workoutSetDao.getById(setId);
            if (s != null) {
                s.name = newName;
                workoutSetDao.update(s);
            }
        });
    }

    /**
     * Удалить сет ПОЛНОСТЬЮ (без архивации — так мы решили).
     * Дополнительно удаляем все связи этого сета с упражнениями
     * (записи в set_exercises). Но упражнения из справочника
     * и история тренировок НЕ трогаются.
     */
    public void deleteSet(int setId) {
        executor.execute(() -> {
            setExerciseDao.deleteBySetId(setId); // сначала связи
            workoutSetDao.deleteById(setId);     // потом сам сет
        });
    }

    /**
     * Изменить порядок сетов (после drag & drop).
     * Принимает список сетов в нужном порядке и переписывает их order_index.
     */
    public void updateSetsOrder(List<WorkoutSet> orderedSets) {
        executor.execute(() -> {
            for (int i = 0; i < orderedSets.size(); i++) {
                WorkoutSet s = orderedSets.get(i);
                s.orderIndex = i;
                workoutSetDao.update(s);
            }
        });
    }

    /**
     * Получить первый сет в очереди (когда нет завершённых тренировок).
     */
    public WorkoutSet getFirstSet() {
        return workoutSetDao.getFirstActive();
    }

    /**
     * Получить список всех сетов синхронно (для логики в коде).
     */
    public List<WorkoutSet> getAllSetsSync() {
        return workoutSetDao.getAllSync();
    }

    /**
     * Получить карту: set_id → количество упражнений в этом сете.
     * Нужно для отображения «N упражнений» в списке сетов.
     */
    public void getSetCounts(Callback<Map<Integer, Integer>> callback) {
        executor.execute(() -> {
            // Получаем список пар (setId, count) из БД.
            List<SetCount> list = setExerciseDao.getAllSetCounts();
            // Превращаем в Map — так удобнее искать по set_id.
            Map<Integer, Integer> map = new HashMap<>();
            for (SetCount sc : list) {
                map.put(sc.setId, sc.count);
            }
            if (callback != null) callback.onResult(map);
        });
    }

    // ================================================================
    // ==============      УПРАЖНЕНИЯ ВНУТРИ СЕТА     =================
    // ================================================================

    /**
     * Получить упражнения из сета (LiveData).
     */
    public LiveData<List<SetExercise>> getExercisesInSet(int setId) {
        return setExerciseDao.getBySetId(setId);
    }

    /**
     * Получить упражнения из сета синхронно.
     */
    public List<SetExercise> getExercisesInSetSync(int setId) {
        return setExerciseDao.getBySetIdSync(setId);
    }

    /**
     * Добавить упражнение в сет (в конец списка).
     * Проверяет, нет ли уже такого упражнения в сете.
     */
    public void addExerciseToSet(int setId, int exerciseId, Callback<Boolean> callback) {
        executor.execute(() -> {
            // Проверка на дубликат.
            if (setExerciseDao.countInSet(setId, exerciseId) > 0) {
                if (callback != null) callback.onResult(false);
                return;
            }

            // Узнаём максимальную позицию.
            Integer maxPos = setExerciseDao.getMaxPosition(setId);
            int newPos = (maxPos == null) ? 0 : maxPos + 1;

            // Вставляем связь.
            setExerciseDao.insert(new SetExercise(setId, exerciseId, newPos));

            if (callback != null) callback.onResult(true);
        });
    }

    /**
     * Удалить упражнение из сета.
     */
    public void removeExerciseFromSet(int setId, int exerciseId) {
        executor.execute(() -> setExerciseDao.deleteBySetAndExercise(setId, exerciseId));
    }

    /**
     * Изменить порядок упражнений внутри сета (после drag & drop).
     */
    public void updateExercisesOrder(int setId, List<SetExercise> orderedList) {
        executor.execute(() -> {
            for (int i = 0; i < orderedList.size(); i++) {
                SetExercise se = orderedList.get(i);
                se.position = i;
                setExerciseDao.update(se);
            }
        });
    }

    // ================================================================
    // ==============         ТРЕНИРОВКИ              =================
    // ================================================================

    public LiveData<List<Workout>> getAllWorkouts() {
        return workoutDao.getAll();
    }

    /**
     * Получить последнюю завершённую тренировку.
     * Нужно для подписи "Сделано в прошлый раз: X дней назад".
     */
    public void getLastWorkoutDate(Callback<String> callback) {
        executor.execute(() -> {
            Workout last = workoutDao.getLastWorkout();
            if (last == null) {
                if (callback != null) callback.onResult(null);
            } else {
                // Возвращаем готовую строку "X дней назад".
                String text = DateUtils.getDaysAgoText(last.startTime);
                if (callback != null) callback.onResult(text);
            }
        });
    }

    /**
     * СОХРАНИТЬ ТРЕНИРОВКУ.
     * Это ключевой метод. Он вызывается при нажатии "Закончить".
     *
     * @param startTime         время начала тренировки (timestamp)
     * @param activeExercises   список активных упражнений (что пользователь сделал)
     * @param skippedExerciseIds список ID упражнений, которые были в сете, но пропущены
     */
    public void saveWorkout(long startTime,
                            List<WorkoutExercise> activeExercises,
                            List<Integer> skippedExerciseIds) {

        executor.execute(() -> {
            long endTime = System.currentTimeMillis();

            // Шаг 1. Создаём запись тренировки.
            Workout workout = new Workout(
                    startTime,
                    endTime,
                    DateUtils.toDbDate(startTime)
            );
            long workoutId = workoutDao.insert(workout);

            // Шаг 2. Сохраняем каждое активное упражнение.
            for (WorkoutExercise we : activeExercises) {
                we.workoutId = (int) workoutId;
                we.skipped = false;
                workoutExerciseDao.insert(we);
            }

            // Шаг 3. Сохраняем пропущенные упражнения (без значений, skipped = true).
            for (int exerciseId : skippedExerciseIds) {
                WorkoutExercise we = new WorkoutExercise();
                we.workoutId = (int) workoutId;
                we.exerciseId = exerciseId;
                we.skipped = true;
                we.isCustom = false;
                workoutExerciseDao.insert(we);
            }
        });
    }

    /**
     * Получить все упражнения тренировки.
     */
    public LiveData<List<WorkoutExercise>> getExercisesInWorkout(int workoutId) {
        return workoutExerciseDao.getByWorkoutId(workoutId);
    }

    // ================================================================
    // ==============   ЛОГИКА ДЛЯ ЭКРАНА ТРЕНИРОВКИ   ================
    // ================================================================

    /**
     * Получить последние значения для упражнения.
     * Используется для автоподстановки на экране тренировки.
     */
    public void getLastValuesForExercise(int exerciseId, Callback<WorkoutExercise> callback) {
        executor.execute(() -> {
            WorkoutExercise last = workoutExerciseDao.getLastValuesForExercise(exerciseId);
            if (callback != null) callback.onResult(last);
        });
    }

    /**
     * Получить ID всех упражнений, которые пользователь когда-либо
     * добавлял ВРУЧНУЮ (для блока "Бывало раньше").
     */
    public void getCustomExerciseIds(Callback<List<Integer>> callback) {
        executor.execute(() -> {
            List<Integer> ids = workoutExerciseDao.getAllCustomExerciseIds();
            if (callback != null) callback.onResult(ids);
        });
    }

    /**
     * Получить ID упражнений, выполненных за конкретную дату.
     * Нужно, чтобы исключить их из блока "Все упражнения".
     */
    public void getTodayExerciseIds(Callback<List<Integer>> callback) {
        executor.execute(() -> {
            String today = DateUtils.todayDbDate();
            List<Integer> ids = workoutExerciseDao.getExerciseIdsByDate(today);
            if (callback != null) callback.onResult(ids);
        });
    }



    // ================================================================
    // ==========   ХРАНЕНИЕ ID ТЕКУЩЕГО СЕТА (SharedPreferences)  =====
    // ================================================================

    /** Имя файла настроек. */
    private static final String PREFS = "jym_prefs";
    private static final String KEY_CURRENT_SET = "current_set_id";

    /** Прочитать ID текущего сета. -1, если ещё не задан. */
    public int getCurrentSetId() {
        SharedPreferences p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return p.getInt(KEY_CURRENT_SET, -1);
    }

    /** Сохранить ID текущего сета. */
    public void setCurrentSetId(int id) {
        SharedPreferences p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        p.edit().putInt(KEY_CURRENT_SET, id).apply();
    }

    /**
     * Получить следующий сет по очереди.
     * Если текущего в списке нет — вернуть первый.
     * Если сетов нет — вернуть null.
     */
    public WorkoutSet getNextSet(int currentSetId) {
        // Только активные — неактивные пропускаем.
        List<WorkoutSet> all = workoutSetDao.getActiveSync();
        if (all.isEmpty()) return null;
        int idx = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id == currentSetId) { idx = i; break; }
        }
        if (idx == -1) return all.get(0);
        int next = (idx + 1) % all.size();
        return all.get(next);
    }

    /** Синхронно получить все активные упражнения. */
    public List<Exercise> getAllActiveExercisesSync() {
        return exerciseDao.getAllActiveSync();
    }


    // ================================================================
    // ===============     ИСТОРИЯ ТРЕНИРОВОК          ================
    // ================================================================

    /** Синхронно получить все тренировки (свежие сверху). */
    public List<Workout> getAllWorkoutsSync() {
        // В DAO у нас LiveData, но для фоновой загрузки проще сделать
        // обычный запрос. Если его нет — добавим ниже.
        return workoutDao.getAllSync();
    }

    /** Синхронно получить все упражнения тренировки. */
    public List<WorkoutExercise> getWorkoutExercisesSync(int workoutId) {
        return workoutExerciseDao.getByWorkoutIdSync(workoutId);
    }

    // ================================================================
    // ================      СТАТИСТИКА              ==================
    // ================================================================

    /** Синхронно получить все не пропущенные записи упражнений. */
    public List<WorkoutExercise> getAllNonSkippedSync() {
        return workoutExerciseDao.getAllNonSkipped();
    }

    /** Синхронно получить все записи по конкретному упражнению. */
    public List<WorkoutExercise> getAllForExerciseSync(int exerciseId) {
        return workoutExerciseDao.getAllForExercise(exerciseId);
    }

    /** Получить дату тренировки (timestamp) для записи упражнения. */
    public long getWorkoutDateForSync(int workoutExerciseId) {
        return workoutExerciseDao.getWorkoutDateFor(workoutExerciseId);
    }

    /** Синхронно получить все упражнения (включая архивные). */
    public List<Exercise> getAllExercisesSync() {
        // В ExerciseDao есть только LiveData-версия.
        // Загрузим её значение через простой хак — берём getAllActiveSync + архивные.
        // Но лучше добавить метод в DAO (см. ниже).
        return exerciseDao.getAllIncludingArchivedSync();
    }


    // ================================================================
    // ============       УПРАЖНЕНИЯ ПО УМОЛЧАНИЮ       ==============
    // ================================================================

    /** Ключ флага: были ли уже добавлены дефолтные упражнения. */
    private static final String KEY_DEFAULTS_ADDED = "defaults_added";

    /**
     * При САМОМ ПЕРВОМ запуске приложения добавляет базовые упражнения.
     * При последующих запусках ничего не делает — даже если пользователь
     * удалил какие-то из них.
     * Работает в фоновом потоке, не блокирует UI.
     */
    public void addDefaultExercisesIfNeeded() {
        executor.execute(() -> {
            SharedPreferences p = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            if (p.getBoolean(KEY_DEFAULTS_ADDED, false)) {
                return; // уже добавляли
            }

            // Формат: {название, тип}
            // Тип 1 = Вес + повторения + подходы
            // Тип 2 = Без веса + повторения + подходы
            // Тип 3 = Время + подходы
            // Тип 4 = Расстояние + время + нагрузка
            String[][] defaults = {
                    {"Подтягивания",                  "2"},
                    {"Бег",                           "4"},
                    {"Ходьба",                        "4"},
                    {"Гребля",                        "4"},
                    {"Планка на локтях",              "3"},
                    {"Гало с гирей",                  "1"},
                    {"Жим штанги на наклонной скамье","1"},
                    {"Гребная тяга",                  "1"},
                    {"Тяга верхнего блока",           "1"},
                    {"Пресс-машина",                  "1"},
                    {"Дельта-машина",                 "1"},
                    {"Тяга гири к подбородку",        "1"},
                    {"Жим платформы ногами",          "1"},
                    {"Скручивания на наклонной скамье","2"},
                    {"Подъем ног в висе",             "2"}
            };

            for (String[] pair : defaults) {
                Exercise e = new Exercise(pair[0], Integer.parseInt(pair[1]));
                exerciseDao.insert(e);
            }

            // Ставим флаг — больше не будем добавлять.
            p.edit().putBoolean(KEY_DEFAULTS_ADDED, true).apply();
        });
    }


    // ================================================================
    // ==============            CALLBACK             =================
    // ================================================================

    /**
     * Простой callback-интерфейс для асинхронных операций.
     * Пример использования:
     *   repository.addExercise("Жим лежа", 1, success -> {
     *       if (success) { ... } else { ... }
     *   });
     */
    public interface Callback<T> {
        void onResult(T result);
    }

    /**
     * Синхронно сохранить тренировку (вызывается из фонового потока).
     */
    public void saveWorkoutSync(long startTime,
                                List<WorkoutExercise> active,
                                List<Integer> skippedExerciseIds) {
        long endTime = System.currentTimeMillis();
        Workout workout = new Workout(startTime, endTime,
                com.example.jym.util.DateUtils.toDbDate(startTime));
        long workoutId = workoutDao.insert(workout);

        for (WorkoutExercise we : active) {
            we.workoutId = (int) workoutId;
            we.skipped = false;
            workoutExerciseDao.insert(we);
        }

        for (int exerciseId : skippedExerciseIds) {
            WorkoutExercise we = new WorkoutExercise();
            we.workoutId = (int) workoutId;
            we.exerciseId = exerciseId;
            we.skipped = true;
            we.isCustom = false;
            workoutExerciseDao.insert(we);
        }
    }

    /** Синхронно получить последнюю тренировку. */
    public Workout getLastWorkoutSync() {
        return workoutDao.getLastWorkout();
    }

    /** Синхронно получить последние значения для упражнения. */
    public WorkoutExercise getLastValuesForExerciseSync(int exerciseId) {
        return workoutExerciseDao.getLastValuesForExercise(exerciseId);
    }

    /** Синхронно получить ID custom-упражнений. */
    public List<Integer> getCustomExerciseIdsSync() {
        return workoutExerciseDao.getAllCustomExerciseIds();
    }

    /** Синхронно получить ID упражнений за сегодня. */
    public List<Integer> getTodayExerciseIdsSync() {
        return workoutExerciseDao.getExerciseIdsByDate(
                com.example.jym.util.DateUtils.todayDbDate());
    }


    /**
     * Удалить тренировку вместе со всеми её упражнениями.
     * Вызывается с экрана истории при удалении записи.
     */
    public void deleteWorkout(int workoutId) {
        executor.execute(() -> {
            // Сначала удаляем все упражнения этой тренировки,
            // потом — саму тренировку (иначе останутся «сироты»).
            workoutExerciseDao.deleteByWorkoutId(workoutId);
            workoutDao.deleteById(workoutId);
        });
    }

    /** Активные тренировки-шаблоны (для диалога смены). */
    public List<WorkoutSet> getActiveSetsSync() {
        return workoutSetDao.getActiveSync();
    }

    /** Переключить активность тренировки-шаблона. */
    public void toggleSetActive(int setId, boolean active) {
        executor.execute(() -> workoutSetDao.setActive(setId, active));
    }

}
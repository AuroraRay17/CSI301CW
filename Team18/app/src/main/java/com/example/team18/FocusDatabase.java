package com.example.team18;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

public class FocusDatabase extends SQLiteOpenHelper {
    public static class Task {
        public final long id;
        public final String title;
        public final boolean done;
        public final int goal;
        public final String note;
        Task(long id, String title, boolean done, int goal, String note) {
            this.id = id;
            this.title = title;
            this.done = done;
            this.goal = goal;
            this.note = note;
        }
    }

    public static class Session {
        public final String taskTitle;
        public final int minutes;
        public final long finishedAt;
        public final int seconds;
        public final boolean completed;
        final FocusTime.Mode mode;

        Session(String taskTitle, int minutes, long finishedAt, int seconds, boolean completed,
                FocusTime.Mode mode) {
            this.taskTitle = taskTitle;
            this.minutes = minutes;
            this.finishedAt = finishedAt;
            this.seconds = seconds;
            this.completed = completed;
            this.mode = mode;
        }
    }

    public static class WeekReview {
        public final int[] dailyMinutes = new int[7];
        final long[] dailySeconds = new long[7];
        long lifetimeSeconds;
        public final String[] dayLabels = new String[7];
        public final List<Session> recentSessions = new ArrayList<>();
        public final List<Session> allSessions = new ArrayList<>();
        public int totalMinutes;
        public int totalSessions;
        public int todayMinutes;
        public int todaySessions;
    }

    static final class RoundResult {
        final boolean inserted;
        final int earnedCoins;

        RoundResult(boolean inserted, int earnedCoins) {
            this.inserted = inserted;
            this.earnedCoins = earnedCoins;
        }
    }

    static final class PlanResult {
        final String taskTitle;
        final int seconds;
        final boolean completed;
        final int earnedCoins;

        PlanResult(String taskTitle, int seconds, boolean completed, int earnedCoins) {
            this.taskTitle = taskTitle;
            this.seconds = seconds;
            this.completed = completed;
            this.earnedCoins = earnedCoins;
        }
    }

    enum PurchaseResult {
        PURCHASED, ALREADY_OWNED, INSUFFICIENT_FUNDS
    }

    public FocusDatabase(Context context) {
        super(context, "focus.db", null, 6);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, done INTEGER NOT NULL DEFAULT 0, goal INTEGER NOT NULL DEFAULT 1, note TEXT NOT NULL DEFAULT '')");
        db.execSQL("CREATE TABLE sessions (id INTEGER PRIMARY KEY AUTOINCREMENT, task_title TEXT NOT NULL, minutes INTEGER NOT NULL, finished_at INTEGER NOT NULL, seconds INTEGER NOT NULL DEFAULT 0, completed INTEGER NOT NULL DEFAULT 1, task_id INTEGER NOT NULL DEFAULT -1, round_id TEXT, timer_mode TEXT NOT NULL DEFAULT 'COUNTDOWN', coins_earned INTEGER NOT NULL DEFAULT 0, plan_id TEXT)");
        db.execSQL("CREATE UNIQUE INDEX sessions_round_id ON sessions(round_id)");
        createPetItemsTable(db);
        createFocusPlansTable(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE tasks ADD COLUMN goal INTEGER NOT NULL DEFAULT 1");
            db.execSQL("ALTER TABLE tasks ADD COLUMN note TEXT NOT NULL DEFAULT ''");
            db.execSQL("ALTER TABLE sessions ADD COLUMN seconds INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE sessions ADD COLUMN completed INTEGER NOT NULL DEFAULT 1");
            db.execSQL("UPDATE sessions SET seconds=minutes*60");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN task_id INTEGER NOT NULL DEFAULT -1");
            db.execSQL("UPDATE sessions SET task_id=COALESCE((SELECT id FROM tasks WHERE title=sessions.task_title ORDER BY id LIMIT 1), -1)");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN round_id TEXT");
            db.execSQL("CREATE UNIQUE INDEX sessions_round_id ON sessions(round_id)");
        }
        if (oldVersion < 5) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN timer_mode TEXT NOT NULL DEFAULT 'COUNTDOWN'");
            db.execSQL("ALTER TABLE sessions ADD COLUMN coins_earned INTEGER NOT NULL DEFAULT 0");
            createPetItemsTable(db);
            if (oldVersion == 4) {
                long completedSeconds = DatabaseUtils.longForQuery(db,
                        "SELECT COALESCE(SUM(seconds), 0) FROM sessions WHERE completed=1", null);
                for (PetItem item : PetItem.values()) {
                    if (item.unlockedBeforeUpgrade(completedSeconds)) insertPetItem(db, item, 0);
                }
            }
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE sessions ADD COLUMN plan_id TEXT");
            createFocusPlansTable(db);
        }
    }

    private void createFocusPlansTable(SQLiteDatabase db) {
        db.execSQL("CREATE INDEX sessions_plan_id ON sessions(plan_id)");
        db.execSQL("CREATE TABLE focus_plan_rewards (plan_id TEXT PRIMARY KEY NOT NULL,"
                + " task_title TEXT NOT NULL, seconds INTEGER NOT NULL,"
                + " completed INTEGER NOT NULL, coins_earned INTEGER NOT NULL)");
    }

    private void createPetItemsTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE pet_items (item_key TEXT PRIMARY KEY NOT NULL, coins_spent INTEGER NOT NULL)");
    }

    public long addTask(String title) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        return getWritableDatabase().insert("tasks", null, values);
    }

    public void updateTask(long id, String title) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        getWritableDatabase().update("tasks", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void updateTaskDetails(long id, int goal, String note) {
        ContentValues values = new ContentValues();
        values.put("goal", goal);
        values.put("note", note);
        getWritableDatabase().update("tasks", values, "id=?", new String[]{String.valueOf(id)});
    }

    public int completedForTask(long taskId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM sessions WHERE task_id=? AND completed=1 AND timer_mode='COUNTDOWN'",
                new String[]{String.valueOf(taskId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    public void setDone(long id, boolean done) {
        ContentValues values = new ContentValues();
        values.put("done", done ? 1 : 0);
        getWritableDatabase().update("tasks", values, "id=?", new String[]{String.valueOf(id)});
    }

    public void deleteTask(long id) {
        getWritableDatabase().delete("tasks", "id=?", new String[]{String.valueOf(id)});
    }

    public List<Task> tasks() {
        List<Task> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(
                "tasks", new String[]{"id", "title", "done", "goal", "note"},
                null, null, null, null, "done ASC, id DESC")) {
            while (cursor.moveToNext()) {
                result.add(new Task(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) != 0, cursor.getInt(3), cursor.getString(4)));
            }
        }
        return result;
    }

    public Task task(long id) {
        try (Cursor cursor = getReadableDatabase().query(
                "tasks", new String[]{"id", "title", "done", "goal", "note"},
                "id=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return new Task(cursor.getLong(0), cursor.getString(1), cursor.getInt(2) != 0, cursor.getInt(3), cursor.getString(4));
            }
        }
        return null;
    }

    long totalFocusedSeconds() {
        return DatabaseUtils.longForQuery(getReadableDatabase(),
                "SELECT COALESCE(SUM(seconds), 0) FROM sessions", null);
    }

    long coinBalance() {
        return coinBalance(getReadableDatabase());
    }

    private long coinBalance(SQLiteDatabase db) {
        return DatabaseUtils.longForQuery(db,
                "SELECT " + DeveloperTools.coinCreditSql(db)
                        + "(SELECT COALESCE(SUM(coins_earned), 0) FROM sessions)"
                        + " + (SELECT COALESCE(SUM(coins_earned), 0) FROM focus_plan_rewards)"
                        + " - (SELECT COALESCE(SUM(coins_spent), 0) FROM pet_items)", null);
    }

    EnumSet<PetItem> ownedPetItems() {
        EnumSet<PetItem> items = EnumSet.noneOf(PetItem.class);
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT item_key FROM pet_items", null)) {
            while (cursor.moveToNext()) {
                String key = cursor.getString(0);
                for (PetItem item : PetItem.values()) {
                    if (item.key.equals(key)) items.add(item);
                }
            }
        }
        return items;
    }

    PurchaseResult purchasePetItem(PetItem item) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            boolean owned = DatabaseUtils.longForQuery(db,
                    "SELECT COUNT(*) FROM pet_items WHERE item_key=?", new String[]{item.key}) > 0;
            if (owned) return PurchaseResult.ALREADY_OWNED;
            if (coinBalance(db) < item.price) return PurchaseResult.INSUFFICIENT_FUNDS;
            insertPetItem(db, item, item.price);
            db.setTransactionSuccessful();
            return PurchaseResult.PURCHASED;
        } finally {
            db.endTransaction();
        }
    }

    private void insertPetItem(SQLiteDatabase db, PetItem item, int coinsSpent) {
        ContentValues values = new ContentValues();
        values.put("item_key", item.key);
        values.put("coins_spent", coinsSpent);
        db.insertOrThrow("pet_items", null, values);
    }

    Boolean savedRoundCompleted(String roundId) {
        try (Cursor cursor = getReadableDatabase().query("sessions", new String[]{"completed"},
                "round_id=?", new String[]{roundId}, null, null, null)) {
            return cursor.moveToFirst() ? cursor.getInt(0) != 0 : null;
        }
    }

    private boolean hasSavedRound(SQLiteDatabase db, String roundId) {
        return DatabaseUtils.longForQuery(db,
                "SELECT COUNT(*) FROM sessions WHERE round_id=?", new String[]{roundId}) != 0;
    }

    RoundResult saveFocusRound(String roundId, long taskId, String taskTitle, int seconds,
                               long finishedAt, FocusTime.Mode mode, boolean completed, String planId) {
        int coins = planId == null ? FocusTime.earnedCoins(seconds, mode, completed) : 0;
        ContentValues values = new ContentValues();
        values.put("round_id", roundId);
        values.put("plan_id", planId);
        values.put("task_id", taskId);
        values.put("task_title", taskTitle);
        values.put("minutes", seconds / 60);
        values.put("finished_at", finishedAt);
        values.put("seconds", seconds);
        values.put("completed", completed ? 1 : 0);
        values.put("timer_mode", mode.name());
        values.put("coins_earned", coins);
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            if (hasSavedRound(db, roundId)) {
                db.setTransactionSuccessful();
                return new RoundResult(false, 0);
            }
            db.insertOrThrow("sessions", null, values);
            if (planId != null && !completed) {
                // The partial round and an early settlement must survive or roll back together.
                insertFocusPlanReward(db, planId, taskTitle, false);
            }
            db.setTransactionSuccessful();
            return new RoundResult(true, coins);
        } finally {
            db.endTransaction();
        }
    }

    PlanResult savedFocusPlan(String planId) {
        try (Cursor cursor = getReadableDatabase().query("focus_plan_rewards",
                new String[]{"task_title", "seconds", "completed", "coins_earned"},
                "plan_id=?", new String[]{planId}, null, null, null)) {
            return cursor.moveToFirst()
                    ? new PlanResult(cursor.getString(0), cursor.getInt(1), cursor.getInt(2) != 0,
                            cursor.getInt(3))
                    : null;
        }
    }

    void settleFocusPlan(String planId, String taskTitle, boolean completed) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            insertFocusPlanReward(db, planId, taskTitle, completed);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    private void insertFocusPlanReward(SQLiteDatabase db, String planId, String taskTitle,
                                       boolean completed) {
        int seconds = (int) DatabaseUtils.longForQuery(db,
                "SELECT COALESCE(SUM(seconds), 0) FROM sessions WHERE plan_id=?", new String[]{planId});
        ContentValues values = new ContentValues();
        values.put("plan_id", planId);
        values.put("task_title", taskTitle);
        values.put("seconds", seconds);
        values.put("completed", completed ? 1 : 0);
        values.put("coins_earned", FocusTime.earnedPlanCoins(seconds, completed));
        db.insertWithOnConflict("focus_plan_rewards", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public String todayRecords(long todayStart, long tomorrowStart) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        int total = 0;
        try (Cursor cursor = getReadableDatabase().query(
                "sessions", new String[]{"task_title", "minutes", "seconds", "completed"},
                "finished_at>=? AND finished_at<?",
                new String[]{String.valueOf(todayStart), String.valueOf(tomorrowStart)},
                null, null, "finished_at DESC")) {
            while (cursor.moveToNext()) {
                count++;
                int minutes = cursor.getInt(1);
                total += minutes;
                result.append("• ").append(cursor.getString(0)).append("  ")
                        .append(cursor.getInt(2) / 60).append(" 分 ")
                        .append(cursor.getInt(2) % 60).append(" 秒")
                        .append(cursor.getInt(3) == 0 ? " · 提前结束\n" : " · 已完成\n");
            }
        }
        return count + " 轮 · " + total + " 分钟\n" + (count == 0 ? "完成一轮后会出现在这里。" : result.toString());
    }

    public WeekReview weekReview() {
        WeekReview review = new WeekReview();
        Calendar day = Calendar.getInstance();
        day.set(Calendar.HOUR_OF_DAY, 0);
        day.set(Calendar.MINUTE, 0);
        day.set(Calendar.SECOND, 0);
        day.set(Calendar.MILLISECOND, 0);
        day.add(Calendar.DAY_OF_MONTH, -6);
        long[] boundaries = new long[8];
        for (int i = 0; i < boundaries.length; i++) {
            boundaries[i] = day.getTimeInMillis();
            if (i < 7) review.dayLabels[i] = (day.get(Calendar.MONTH) + 1) + "/" + day.get(Calendar.DAY_OF_MONTH);
            day.add(Calendar.DAY_OF_MONTH, 1);
        }
        try (Cursor cursor = getReadableDatabase().query(
                "sessions", new String[]{"task_title", "minutes", "finished_at", "seconds", "completed", "timer_mode"},
                null, null,
                null, null, "finished_at DESC")) {
            while (cursor.moveToNext()) {
                Session session = new Session(cursor.getString(0), cursor.getInt(1),
                        cursor.getLong(2), cursor.getInt(3), cursor.getInt(4) != 0,
                        FocusTime.Mode.fromStoredValue(cursor.getString(5)));
                review.lifetimeSeconds += session.seconds;
                review.allSessions.add(session);
                if (review.recentSessions.size() < 10) review.recentSessions.add(session);
                if (session.finishedAt < boundaries[0] || session.finishedAt >= boundaries[7]) continue;
                if (session.completed) review.totalSessions++;
                for (int i = 0; i < 7; i++) {
                    if (session.finishedAt >= boundaries[i] && session.finishedAt < boundaries[i + 1]) {
                        review.dailySeconds[i] += session.seconds;
                        if (i == 6) {
                            if (session.completed) review.todaySessions++;
                        }
                        break;
                    }
                }
            }
        }
        long weekSeconds = 0;
        for (int i = 0; i < review.dailySeconds.length; i++) {
            weekSeconds += review.dailySeconds[i];
            review.dailyMinutes[i] = (int) (review.dailySeconds[i] / 60);
        }
        review.todayMinutes = review.dailyMinutes[6];
        review.totalMinutes = (int) (weekSeconds / 60);
        return review;
    }
}

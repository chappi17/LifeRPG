package com.routinequest.app.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String PREFS = "routine_quest";
    private static final String KEY_HABITS = "habits";
    private static final String KEY_EQUIPPED_TITLE = "equipped_title";

    private static final Gson GSON = new Gson();
    private static DataManager instance;

    private final SharedPreferences prefs;
    private List<Habit> habits = new ArrayList<>();
    private String equippedTitleId = "";

    private DataManager(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        load();
    }

    public static DataManager get(Context ctx) {
        if (instance == null) instance = new DataManager(ctx);
        return instance;
    }

    // ── Load / Save ──────────────────────────────
    private void load() {
        String json = prefs.getString(KEY_HABITS, "[]");
        Type type = new TypeToken<List<Habit>>(){}.getType();
        try { habits = GSON.fromJson(json, type); }
        catch (Exception e) { habits = new ArrayList<>(); }
        if (habits == null) habits = new ArrayList<>();
        // sanitize stat field
        for (Habit h : habits) if (h.stat == null) h.stat = "str";
        equippedTitleId = prefs.getString(KEY_EQUIPPED_TITLE, "");
    }

    public void save() {
        prefs.edit()
            .putString(KEY_HABITS, GSON.toJson(habits))
            .putString(KEY_EQUIPPED_TITLE, equippedTitleId)
            .apply();
    }

    // ── Habits CRUD ───────────────────────────────
    public List<Habit> getHabits() { return habits; }

    public void addHabit(Habit h) { habits.add(h); save(); }

    public void removeHabit(String id) {
        habits.removeIf(h -> h.id.equals(id)); save();
    }

    public Habit findHabit(String id) {
        for (Habit h : habits) if (h.id.equals(id)) return h;
        return null;
    }

    public void toggle(String habitId, String date) {
        Habit h = findHabit(habitId);
        if (h == null) return;
        h.setDone(date, !h.isDoneOn(date));
        save();
    }

    // ── Titles ────────────────────────────────────
    public String getEquippedTitleId() { return equippedTitleId; }

    public void equipTitle(String id) {
        equippedTitleId = id;
        prefs.edit().putString(KEY_EQUIPPED_TITLE, id).apply();
    }

    // ── Backup / Restore ──────────────────────────
    public String exportJson() {
        BackupData bd = new BackupData();
        bd.version = 2;
        bd.exportedAt = new java.util.Date().toString();
        bd.habits = habits;
        bd.equippedTitleId = equippedTitleId;
        return GSON.toJson(bd);
    }

    public boolean importJson(String json) {
        try {
            BackupData bd = GSON.fromJson(json, BackupData.class);
            if (bd.habits == null) return false;
            habits = bd.habits;
            for (Habit h : habits) if (h.stat == null) h.stat = "str";
            if (bd.equippedTitleId != null) equippedTitleId = bd.equippedTitleId;
            save();
            return true;
        } catch (Exception e) { return false; }
    }

    public void resetAll() {
        habits = new ArrayList<>();
        equippedTitleId = "";
        save();
    }

    // ── Inner classes ─────────────────────────────
    private static class BackupData {
        int version;
        String exportedAt;
        List<Habit> habits;
        String equippedTitleId;
    }
}

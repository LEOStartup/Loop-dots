package com.leostartup.loopdots;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Independent habits, durable per-date marks, and migration of the original single habit. */
public final class HabitStore {
    private static final String PREFS = "loop_dots";
    private static final String HABITS = "habits_v2";
    private static final String DONE_PREFIX = "done_v2_";
    public static final int[] COLORS = {
            0xFFFF3030, 0xFF3B82F6, 0xFF22C55E, 0xFFF59E0B,
            0xFFAA66FF, 0xFFEC4899, 0xFF06B6D4, 0xFFFFFFFF
    };

    public static final class Habit {
        public final String id;
        public String name;
        public int color;
        public String icon;
        public Habit(String id, String name, int color, String icon) {
            this.id = id; this.name = name; this.color = color; this.icon = icon;
        }
    }

    private HabitStore() {}

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized List<Habit> all(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.contains(HABITS)) {
            JSONArray initial = new JSONArray();
            JSONObject defaultHabit = new JSONObject();
            try {
                defaultHabit.put("id", "habit_default");
                defaultHabit.put("name", "Hábito");
                defaultHabit.put("color", COLORS[0]);
                defaultHabit.put("icon", "✓");
                initial.put(defaultHabit);
            } catch (Exception ignored) {}
            p.edit().putString(HABITS, initial.toString()).apply();
            // Preserve all the original month/day entries from the previous app.
            for (String key : p.getAll().keySet()) {
                if (key.matches("\\d{4}-\\d{2}")) {
                    Set<String> old = p.getStringSet(key, new HashSet<>());
                    Set<String> migrated = new HashSet<>(done(context, "habit_default"));
                    for (String day : old) {
                        try {
                            int number = Integer.parseInt(day);
                            Calendar c = Calendar.getInstance();
                            c.set(Integer.parseInt(key.substring(0, 4)),
                                    Integer.parseInt(key.substring(5, 7)) - 1, number);
                            if (number >= 1 && number <= c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                                migrated.add(String.format(Locale.US, "%s-%02d", key, number));
                            }
                        } catch (Exception ignored) {}
                    }
                    p.edit().putStringSet(DONE_PREFIX + "habit_default", migrated).apply();
                }
            }
        }
        List<Habit> result = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(p.getString(HABITS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject o = array.getJSONObject(i);
                result.add(new Habit(o.getString("id"), o.optString("name", "Hábito"),
                        o.optInt("color", COLORS[0]), o.optString("icon", "✓")));
            }
        } catch (Exception ignored) {}
        return result;
    }

    public static synchronized Habit get(Context context, String id) {
        for (Habit h : all(context)) if (h.id.equals(id)) return h;
        return null;
    }

    public static synchronized Habit save(Context context, String id, String name, int color, String icon) {
        List<Habit> habits = all(context);
        Habit value = null;
        for (Habit h : habits) if (h.id.equals(id)) value = h;
        if (value == null) {
            value = new Habit("h_" + java.util.UUID.randomUUID().toString(), name, color, icon);
            habits.add(value);
        }
        value.name = name.trim();
        value.color = color;
        value.icon = icon;
        JSONArray array = new JSONArray();
        for (Habit h : habits) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", h.id); o.put("name", h.name);
                o.put("color", h.color); o.put("icon", h.icon);
                array.put(o);
            } catch (Exception ignored) {}
        }
        prefs(context).edit().putString(HABITS, array.toString()).apply();
        return value;
    }

    public static synchronized void delete(Context context, String id) {
        List<Habit> habits = all(context);
        JSONArray array = new JSONArray();
        for (Habit h : habits) {
            if (h.id.equals(id)) continue;
            JSONObject o = new JSONObject();
            try {
                o.put("id", h.id); o.put("name", h.name); o.put("color", h.color);
                o.put("icon", h.icon); array.put(o);
            } catch (Exception ignored) {}
        }
        prefs(context).edit().putString(HABITS, array.toString())
                .remove(DONE_PREFIX + id).apply();
    }

    public static Set<String> done(Context context, String id) {
        return new HashSet<>(prefs(context).getStringSet(DONE_PREFIX + id, new HashSet<>()));
    }

    public static synchronized void setDone(Context context, String id, String date, boolean marked) {
        if (get(context, id) == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) return;
        Set<String> dates = done(context, id);
        if (marked) dates.add(date); else dates.remove(date);
        prefs(context).edit().putStringSet(DONE_PREFIX + id, dates).apply();
    }

    public static String dateKey(Calendar c) {
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    public static String monthKey(Calendar c) {
        return String.format(Locale.US, "%04d-%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1);
    }

    public static int count(Context context, String id) { return done(context, id).size(); }
}

package com.leostartup.loopdots;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.SeekBar;
import android.widget.RadioGroup;
import android.widget.RadioButton;
import org.json.JSONArray;
import java.util.ArrayList;
import java.util.List;

public class WidgetConfigActivity extends Activity {
    private static final String PREFS = "widget_config";
    private static final String PREFIX = "widget_";
    private static final String MULTI_PREFIX = "multi_";
    private static final String MONTH_PREFIX = "month_";
    private static final String GLASS_PREFIX = "glass_";
    private static final String ALPHA_PREFIX = "alpha_";
    public static boolean glass(Context context, int id) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(GLASS_PREFIX + id, false);
    }
    public static int opacity(Context context, int id) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getInt(ALPHA_PREFIX + id, 60);
    }
    public static void saveAppearance(Context context, int id, boolean glass, int opacity) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit()
            .putBoolean(GLASS_PREFIX + id, glass).putInt(ALPHA_PREFIX + id, opacity).apply();
    }
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public static List<String> habitIds(Context context, int widgetId) {
        SharedPreferences p = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        List<String> ids = new ArrayList<>();
        String json = p.getString(MULTI_PREFIX + widgetId, null);
        if (json != null) {
            try {
                JSONArray array = new JSONArray(json);
                for (int i = 0; i < array.length(); i++) {
                    String id = array.optString(i);
                    if (!id.isEmpty() && !ids.contains(id) && HabitStore.get(context, id) != null) ids.add(id);
                }
            } catch (Exception ignored) {}
        } else {
            // Previously installed widgets keep their one selected habit.
            String old = p.getString(PREFIX + widgetId, null);
            if (old != null && HabitStore.get(context, old) != null) ids.add(old);
        }
        return ids;
    }

    public static String habitId(Context context, int widgetId) {
        List<String> ids = habitIds(context, widgetId);
        return ids.isEmpty() ? null : ids.get(0);
    }

    public static void saveHabits(Context context, int widgetId, List<String> ids) {
        JSONArray array = new JSONArray();
        for (String id : ids) if (HabitStore.get(context, id) != null) array.put(id);
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(MULTI_PREFIX + widgetId, array.toString())
                .remove(PREFIX + widgetId).apply();
    }

    public static int monthOffset(Context context, int widgetId) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getInt(MONTH_PREFIX + widgetId, 0);
    }

    public static void changeMonth(Context context, int widgetId, int delta) {
        SharedPreferences p = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        int offset = p.getInt(MONTH_PREFIX + widgetId, 0);
        p.edit().putInt(MONTH_PREFIX + widgetId, Math.max(-1200, Math.min(1200, offset + delta))).apply();
    }

    public static void clear(Context context, int widgetId) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREFIX + widgetId)
                .remove(MULTI_PREFIX + widgetId).remove(MONTH_PREFIX + widgetId)
                .remove(GLASS_PREFIX + widgetId).remove(ALPHA_PREFIX + widgetId).apply();
    }

    private int dp(int n) { return (int) (getResources().getDisplayMetrics().density * n + .5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(36), dp(24), dp(24));
        root.setBackgroundColor(0xFF141519);
        TextView title = new TextView(this);
        title.setText("Escolher hábitos");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Marque vários hábitos. Cada um terá sua própria cor no calendário.");
        subtitle.setTextColor(0xFFAAAAAA);
        subtitle.setPadding(0, dp(9), 0, dp(14));
        root.addView(subtitle);

        TextView appearance = new TextView(this);
        appearance.setText("Aparência do widget");
        appearance.setTextColor(Color.WHITE);
        appearance.setTextSize(18);
        appearance.setPadding(0, dp(12), 0, dp(8));
        root.addView(appearance);
        RadioGroup style = new RadioGroup(this);
        style.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton transparent = new RadioButton(this);
        transparent.setId(android.view.View.generateViewId());
        transparent.setText("Transparente");
        transparent.setTextColor(Color.WHITE);
        RadioButton frosted = new RadioButton(this);
        frosted.setId(android.view.View.generateViewId());
        frosted.setText("Glassmorphism");
        frosted.setTextColor(Color.WHITE);
        style.addView(transparent);
        style.addView(frosted);
        style.check(glass(this, widgetId) ? frosted.getId() : transparent.getId());
        root.addView(style);
        TextView alphaLabel = new TextView(this);
        alphaLabel.setTextColor(Color.WHITE);
        alphaLabel.setPadding(0, dp(12), 0, 0);
        SeekBar alpha = new SeekBar(this);
        alpha.setMax(95);
        alpha.setProgress(opacity(this, widgetId));
        alphaLabel.setText("Intensidade do vidro: " + alpha.getProgress() + "%");
        alpha.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                alphaLabel.setText("Intensidade do vidro: " + progress + "%");
            }
            public void onStartTrackingTouch(SeekBar bar) {}
            public void onStopTrackingTouch(SeekBar bar) {}
        });
        alpha.setEnabled(style.getCheckedRadioButtonId() == frosted.getId());
        style.setOnCheckedChangeListener((group, checkedId) -> {
            boolean enabled = checkedId == frosted.getId();
            alpha.setEnabled(enabled);
            alphaLabel.setAlpha(enabled ? 1f : .45f);
        });
        alphaLabel.setAlpha(alpha.isEnabled() ? 1f : .45f);
        root.addView(alphaLabel);
        root.addView(alpha);
        TextView note = new TextView(this);
        note.setText("Vidro translúcido com borda suave. O Android não permite desfocar o papel de parede diretamente neste widget.");
        note.setTextColor(0xFFAAAAAA);
        note.setTextSize(12);
        root.addView(note);
        ScrollView scroll = new ScrollView(this);
        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(options);
        List<HabitStore.Habit> habits = HabitStore.all(this);
        List<String> selected = habitIds(this, widgetId);
        List<CheckBox> checks = new ArrayList<>();
        for (HabitStore.Habit habit : habits) {
            CheckBox option = new CheckBox(this);
            option.setText(habit.icon + "  " + habit.name);
            option.setTextSize(17);
            option.setTextColor(Color.WHITE);
            option.setButtonTintList(ColorStateList.valueOf(habit.color));
            option.setChecked(selected.contains(habit.id));
            option.setPadding(0, dp(6), 0, dp(6));
            checks.add(option);
            options.addView(option);
        }
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        Button save = new Button(this);
        save.setText("Salvar e atualizar widget");
        save.setAllCaps(false);
        save.setEnabled(!habits.isEmpty() && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID);
        save.setOnClickListener(v -> {
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < checks.size(); i++) if (checks.get(i).isChecked()) ids.add(habits.get(i).id);
            if (ids.isEmpty()) {
                android.widget.Toast.makeText(this, "Selecione ao menos um hábito", android.widget.Toast.LENGTH_SHORT).show();
                return;
            }
            saveHabits(this, widgetId, ids);
            saveAppearance(this, widgetId, style.getCheckedRadioButtonId() == frosted.getId(), alpha.getProgress());
            LoopDotsWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), widgetId);
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
            finish();
        });
        root.addView(save);
        TextView hint = new TextView(this);
        hint.setText(habits.isEmpty() ? "Abra o Loop Dots e crie um hábito primeiro." :
                "Para criar hábitos ou mudar suas cores, abra o aplicativo Loop Dots.");
        hint.setTextColor(0xFFAAAAAA);
        hint.setPadding(0, dp(12), 0, 0);
        root.addView(hint);
        setContentView(root);
    }
}

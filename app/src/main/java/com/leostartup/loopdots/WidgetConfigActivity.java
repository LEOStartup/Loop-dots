package com.leostartup.loopdots;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.List;

public class WidgetConfigActivity extends Activity {
    private static final String PREFS = "widget_config";
    private static final String PREFIX = "widget_";
    private int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

    public static String habitId(Context context, int widgetId) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getString(PREFIX + widgetId, null);
    }
    public static void clear(Context context, int widgetId) {
        context.getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(PREFIX + widgetId).apply();
    }
    private int dp(int n) { return (int) (getResources().getDisplayMetrics().density * n + .5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        setResult(RESULT_CANCELED);
        if (getIntent().getExtras() != null)
            widgetId = getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                    AppWidgetManager.INVALID_APPWIDGET_ID);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(24), dp(42), dp(24), dp(24));
        root.setBackgroundColor(0xFF141519);
        TextView title = new TextView(this);
        title.setText("Escolher hábito");
        title.setTextSize(25);
        title.setTextColor(Color.WHITE);
        root.addView(title);
        TextView subtitle = new TextView(this);
        subtitle.setText("Cada widget acompanha um hábito e sua própria cor.");
        subtitle.setTextColor(0xFFAAAAAA);
        subtitle.setPadding(0, dp(9), 0, dp(14));
        root.addView(subtitle);
        RadioGroup group = new RadioGroup(this);
        List<HabitStore.Habit> habits = HabitStore.all(this);
        String selected = habitId(this, widgetId);
        int index = 0, selectedIndex = -1;
        for (HabitStore.Habit habit : habits) {
            RadioButton option = new RadioButton(this);
            option.setText(habit.icon + "  " + habit.name);
            option.setTextSize(17);
            option.setTextColor(Color.WHITE);
            option.setButtonTintList(android.content.res.ColorStateList.valueOf(habit.color));
            group.addView(option);
            if (habit.id.equals(selected)) selectedIndex = index;
            index++;
        }
        if (group.getChildCount() > 0) group.check(group.getChildAt(
                selectedIndex >= 0 ? selectedIndex : 0).getId());
        // Generate actual radio IDs to support reliable group selection.
        for (int i = 0; i < group.getChildCount(); i++) {
            group.getChildAt(i).setId(android.view.View.generateViewId());
        }
        if (group.getChildCount() > 0) group.check(group.getChildAt(
                selectedIndex >= 0 ? selectedIndex : 0).getId());
        root.addView(group, new LinearLayout.LayoutParams(-1, 0, 1));
        Button save = new Button(this);
        save.setText("Adicionar / atualizar widget");
        save.setAllCaps(false);
        save.setEnabled(!habits.isEmpty() && widgetId != AppWidgetManager.INVALID_APPWIDGET_ID);
        save.setOnClickListener(v -> {
            int checked = group.indexOfChild(group.findViewById(group.getCheckedRadioButtonId()));
            if (checked < 0 || checked >= habits.size()) return;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString(PREFIX + widgetId, habits.get(checked).id).apply();
            AppWidgetManager manager = AppWidgetManager.getInstance(this);
            LoopDotsWidgetProvider.updateWidget(this, manager, widgetId);
            Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
            finish();
        });
        root.addView(save);
        TextView hint = new TextView(this);
        hint.setText(habits.isEmpty() ? "Abra o Loop Dots e crie um hábito primeiro." :
                "Para criar ou mudar a cor de um hábito, abra o aplicativo Loop Dots.");
        hint.setTextColor(0xFFAAAAAA);
        hint.setPadding(0, dp(12), 0, 0);
        root.addView(hint);
        setContentView(root);
    }
}

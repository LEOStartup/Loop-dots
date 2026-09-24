package com.leostartup.loopdots;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.widget.RemoteViews;
import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class LoopDotsWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_TOGGLE = "com.leostartup.loopdots.TOGGLE_DAY";
    public static final String ACTION_MONTH = "com.leostartup.loopdots.CHANGE_MONTH";
    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_HABIT = "habit";
    public static final String EXTRA_WIDGET = "widget";
    public static final String EXTRA_DELTA = "delta";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override public void onAppWidgetOptionsChanged(Context context, AppWidgetManager manager,
            int widgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, manager, widgetId, newOptions);
        updateWidget(context, manager, widgetId);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) WidgetConfigActivity.clear(context, id);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_MONTH.equals(intent.getAction())) {
            int widgetId = intent.getIntExtra(EXTRA_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
            int delta = intent.getIntExtra(EXTRA_DELTA, 0);
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID && (delta == -1 || delta == 1)) {
                WidgetConfigActivity.changeMonth(context, widgetId, delta);
                updateWidget(context, AppWidgetManager.getInstance(context), widgetId);
            }
            return;
        }
        if (!ACTION_TOGGLE.equals(intent.getAction())) return;
        int day = intent.getIntExtra(EXTRA_DAY, -1);
        String month = intent.getStringExtra(EXTRA_MONTH);
        String habitId = intent.getStringExtra(EXTRA_HABIT);
        int widgetId = intent.getIntExtra(EXTRA_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
        Calendar displayed = Calendar.getInstance();
        displayed.set(Calendar.DAY_OF_MONTH, 1);
        displayed.add(Calendar.MONTH, WidgetConfigActivity.monthOffset(context, widgetId));
        // Reject old PendingIntents after navigation or configuration changes.
        if (month != null && month.equals(HabitStore.monthKey(displayed))
                && day > 0 && day <= displayed.getActualMaximum(Calendar.DAY_OF_MONTH)
                && habitId != null && WidgetConfigActivity.habitIds(context, widgetId).contains(habitId)) {
            displayed.set(Calendar.DAY_OF_MONTH, day);
            String key = HabitStore.dateKey(displayed);
            Set<String> done = HabitStore.done(context, habitId);
            HabitStore.setDone(context, habitId, key, !done.contains(key));
        }
        refreshAll(context);
    }

    public static void refreshAll(Context context) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, LoopDotsWidgetProvider.class));
        for (int id : ids) updateWidget(context, manager, id);
    }

    public static void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        Calendar month = Calendar.getInstance();
        month.set(Calendar.DAY_OF_MONTH, 1);
        month.add(Calendar.MONTH, WidgetConfigActivity.monthOffset(context, widgetId));
        String monthKey = HabitStore.monthKey(month);
        List<HabitStore.Habit> habits = new ArrayList<>();
        for (String id : WidgetConfigActivity.habitIds(context, widgetId)) {
            HabitStore.Habit habit = HabitStore.get(context, id);
            if (habit != null) habits.add(habit);
        }
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.removeAllViews(R.id.dots_container);
        if (habits.isEmpty()) {
            views.setTextViewText(R.id.habit_title, "Escolher hábitos");
            views.setTextViewText(R.id.total_count, "");
            views.setTextViewText(R.id.month_label, "Toque para configurar");
            views.setViewVisibility(R.id.prev_month, android.view.View.GONE);
            views.setViewVisibility(R.id.next_month, android.view.View.GONE);
            PendingIntent configure = configurationIntent(context, widgetId);
            views.setOnClickPendingIntent(R.id.widget_root, configure);
            manager.updateAppWidget(widgetId, views);
            return;
        }

        Bundle options = manager.getAppWidgetOptions(widgetId);
        int minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280);
        int maxWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, minWidth);
        int minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 220);
        // Width reported by launchers is the relevant usable dp; do not use physical screen width.
        int width = Math.max(160, minWidth > 0 ? minWidth : maxWidth);
        int height = Math.max(90, minHeight);
        int cell = Math.max(17, Math.min(54, (width - 12) / 7));
        // Fit a header, month navigation, one row per habit and its 5 calendar rows.
        int rowHeight = Math.max(17, Math.min(cell, (height - 51) / Math.max(1, habits.size() * 5)));
        int diameter = Math.max(9, Math.min(cell - 5, rowHeight - 3));
        float density = context.getResources().getDisplayMetrics().density;
        int dotPixel = Math.max(24, Math.round(diameter * density));
        Bitmap empty = bitmap(0x66FFFFFF, dotPixel);
        int total = 0;
        for (HabitStore.Habit habit : habits) total += HabitStore.count(context, habit.id);
        views.setTextViewText(R.id.habit_title, habits.size() == 1 ? habits.get(0).name : habits.size() + " hábitos");
        views.setTextViewText(R.id.total_count, total + " dias");
        views.setTextViewText(R.id.month_label, monthLabel(month));
        views.setViewVisibility(R.id.prev_month, android.view.View.VISIBLE);
        views.setViewVisibility(R.id.next_month, android.view.View.VISIBLE);
        views.setOnClickPendingIntent(R.id.habit_title, configurationIntent(context, widgetId));
        views.setOnClickPendingIntent(R.id.prev_month, monthIntent(context, widgetId, -1));
        views.setOnClickPendingIntent(R.id.next_month, monthIntent(context, widgetId, 1));

        int monthDays = month.getActualMaximum(Calendar.DAY_OF_MONTH);
        // For multiple habits use one 7-column calendar per habit, each in its own color.
        // Five rows show all 28-31 dates with a predictable tap target per dot.
        for (HabitStore.Habit habit : habits) {
            RemoteViews label = new RemoteViews(context.getPackageName(), R.layout.habit_label);
            label.setTextViewText(R.id.row_habit_name, habit.icon + "  " + habit.name);
            label.setTextColor(R.id.row_habit_name, habit.color);
            views.addView(R.id.dots_container, label);
            Set<String> marked = HabitStore.done(context, habit.id);
            Bitmap filled = bitmap(habit.color, dotPixel);
            for (int rowIndex = 0; rowIndex < 5; rowIndex++) {
                RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.dot_row);
                row.setInt(R.id.dot_row, "setMinimumHeight", rowHeight);
                for (int column = 0; column < 7; column++) {
                    int day = rowIndex * 7 + column + 1;
                    RemoteViews dot = new RemoteViews(context.getPackageName(), R.layout.dot_item);
                    dot.setInt(R.id.dot_touch, "setMinimumHeight", rowHeight);
                    dot.setInt(R.id.dot_touch, "setMinimumWidth", cell);
                    if (day <= monthDays) {
                        Calendar date = (Calendar) month.clone();
                        date.set(Calendar.DAY_OF_MONTH, day);
                        String key = HabitStore.dateKey(date);
                        dot.setImageViewBitmap(R.id.dot_visual, marked.contains(key) ? filled : empty);
                        dot.setContentDescription(R.id.dot_touch,
                                String.format(Locale.getDefault(), "%s, %02d/%02d/%04d", habit.name,
                                        day, month.get(Calendar.MONTH) + 1, month.get(Calendar.YEAR)));
                        Intent toggle = new Intent(context, LoopDotsWidgetProvider.class);
                        toggle.setAction(ACTION_TOGGLE);
                        toggle.putExtra(EXTRA_DAY, day);
                        toggle.putExtra(EXTRA_MONTH, monthKey);
                        toggle.putExtra(EXTRA_HABIT, habit.id);
                        toggle.putExtra(EXTRA_WIDGET, widgetId);
                        toggle.setData(Uri.parse("loopdots://widget/" + widgetId + "/" + habit.id + "/" + monthKey + "/" + day));
                        PendingIntent pending = PendingIntent.getBroadcast(context, 0, toggle,
                                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                        dot.setOnClickPendingIntent(R.id.dot_touch, pending);
                    } else {
                        dot.setViewVisibility(R.id.dot_visual, android.view.View.INVISIBLE);
                    }
                    row.addView(R.id.dot_row, dot);
                }
                views.addView(R.id.dots_container, row);
            }
        }
        manager.updateAppWidget(widgetId, views);
    }

    private static PendingIntent configurationIntent(Context context, int widgetId) {
        Intent intent = new Intent(context, WidgetConfigActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        intent.setData(Uri.parse("loopdots://configure/" + widgetId));
        return PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent monthIntent(Context context, int widgetId, int delta) {
        Intent intent = new Intent(context, LoopDotsWidgetProvider.class);
        intent.setAction(ACTION_MONTH);
        intent.putExtra(EXTRA_WIDGET, widgetId);
        intent.putExtra(EXTRA_DELTA, delta);
        intent.setData(Uri.parse("loopdots://month/" + widgetId + "/" + delta));
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static Bitmap bitmap(int color, int size) {
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - 1, paint);
        return b;
    }

    private static String monthLabel(Calendar calendar) {
        String name = new DateFormatSymbols(new Locale("pt", "BR")).getMonths()[calendar.get(Calendar.MONTH)];
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1)
                + " " + calendar.get(Calendar.YEAR);
    }
}

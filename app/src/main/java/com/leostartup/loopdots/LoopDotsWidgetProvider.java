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
import android.widget.RemoteViews;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Locale;
import java.util.Set;

public class LoopDotsWidgetProvider extends AppWidgetProvider {
    public static final String ACTION_TOGGLE = "com.leostartup.loopdots.TOGGLE_DAY";
    public static final String EXTRA_DAY = "day";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_HABIT = "habit";
    public static final String EXTRA_WIDGET = "widget";

    @Override public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override public void onDeleted(Context context, int[] ids) {
        for (int id : ids) WidgetConfigActivity.clear(context, id);
    }

    @Override public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (!ACTION_TOGGLE.equals(intent.getAction())) return;
        int day = intent.getIntExtra(EXTRA_DAY, -1);
        String month = intent.getStringExtra(EXTRA_MONTH);
        String habitId = intent.getStringExtra(EXTRA_HABIT);
        int widgetId = intent.getIntExtra(EXTRA_WIDGET, AppWidgetManager.INVALID_APPWIDGET_ID);
        Calendar now = Calendar.getInstance();
        // Never apply a stale tap to another month, another widget, or a removed habit.
        if (day > 0 && day <= now.getActualMaximum(Calendar.DAY_OF_MONTH)
                && HabitStore.monthKey(now).equals(month)
                && habitId != null && habitId.equals(WidgetConfigActivity.habitId(context, widgetId))) {
            Calendar date = (Calendar) now.clone();
            date.set(Calendar.DAY_OF_MONTH, day);
            String key = HabitStore.dateKey(date);
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
        Calendar now = Calendar.getInstance();
        String month = HabitStore.monthKey(now);
        String habitId = WidgetConfigActivity.habitId(context, widgetId);
        HabitStore.Habit habit = habitId == null ? null : HabitStore.get(context, habitId);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        if (habit == null) {
            views.setTextViewText(R.id.habit_title, "Escolher hábito");
            views.setTextViewText(R.id.total_count, "");
            views.setTextViewText(R.id.month_label, "Toque para configurar");
            views.removeAllViews(R.id.dots_container);
            Intent configure = new Intent(context, WidgetConfigActivity.class);
            configure.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            PendingIntent pending = PendingIntent.getActivity(context, widgetId, configure,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(R.id.widget_root, pending);
            manager.updateAppWidget(widgetId, views);
            return;
        }
        views.setTextViewText(R.id.habit_title, habit.name);
        views.setTextViewText(R.id.month_label, monthLabel(now));
        views.setTextViewText(R.id.total_count, HabitStore.count(context, habit.id) + " dias");
        views.setContentDescription(R.id.habit_icon, habit.icon + " " + habit.name);
        views.removeAllViews(R.id.dots_container);
        RemoteViews row = new RemoteViews(context.getPackageName(), R.layout.dot_row);
        Set<String> marked = HabitStore.done(context, habit.id);
        Bitmap filled = bitmap(habit.color);
        Bitmap empty = bitmap(0x55FFFFFF);

        for (int day = 1; day <= now.getActualMaximum(Calendar.DAY_OF_MONTH); day++) {
            if ((day - 1) % 7 == 0 && day > 1) {
                views.addView(R.id.dots_container, row);
                row = new RemoteViews(context.getPackageName(), R.layout.dot_row);
            }
            RemoteViews dot = new RemoteViews(context.getPackageName(), R.layout.dot_item);
            Calendar date = (Calendar) now.clone();
            date.set(Calendar.DAY_OF_MONTH, day);
            String key = HabitStore.dateKey(date);
            dot.setImageViewBitmap(R.id.dot_visual, marked.contains(key) ? filled : empty);
            dot.setContentDescription(R.id.dot_touch,
                    String.format(Locale.getDefault(), "%02d/%02d: %s", day, now.get(Calendar.MONTH) + 1, habit.name));
            Intent toggle = new Intent(context, LoopDotsWidgetProvider.class);
            toggle.setAction(ACTION_TOGGLE);
            toggle.putExtra(EXTRA_DAY, day);
            toggle.putExtra(EXTRA_MONTH, month);
            toggle.putExtra(EXTRA_HABIT, habit.id);
            toggle.putExtra(EXTRA_WIDGET, widgetId);
            // Unique across widgets, dates and habits, even when the same day is tapped.
            toggle.setData(android.net.Uri.parse("loopdots://widget/" + widgetId + "/" + day));
            PendingIntent pending = PendingIntent.getBroadcast(context,
                    0, toggle, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            dot.setOnClickPendingIntent(R.id.dot_touch, pending);
            row.addView(R.id.dot_row, dot);
        }
        views.addView(R.id.dots_container, row);
        // Header opens the editor and day dots toggle exactly their respective date.
        Intent editor = new Intent(context, WidgetConfigActivity.class);
        editor.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        PendingIntent configure = PendingIntent.getActivity(context, widgetId, editor,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.habit_title, configure);
        manager.updateAppWidget(widgetId, views);
    }

    private static Bitmap bitmap(int color) {
        Bitmap b = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawCircle(36f, 36f, 35f, paint);
        return b;
    }

    private static String monthLabel(Calendar calendar) {
        String month = new DateFormatSymbols(new Locale("pt", "BR"))
                .getMonths()[calendar.get(Calendar.MONTH)];
        return month.substring(0, 1).toUpperCase(Locale.ROOT)
                + month.substring(1) + " " + calendar.get(Calendar.YEAR);
    }
}

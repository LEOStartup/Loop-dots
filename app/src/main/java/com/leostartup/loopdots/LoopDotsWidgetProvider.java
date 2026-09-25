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

        if (WidgetConfigActivity.glass(context, widgetId)) {
            views.setInt(android.R.id.background, "setBackgroundColor",
                    glassTint(WidgetConfigActivity.opacity(context, widgetId), WidgetConfigActivity.tone(context, widgetId)));
            views.setViewVisibility(android.R.id.background, android.view.View.VISIBLE);
        } else {
            views.setViewVisibility(android.R.id.background, android.view.View.GONE);
        }
        if (habits.isEmpty()) {
            views.setTextViewText(R.id.habit_title, "Escolher hábitos");
            views.setTextViewText(R.id.total_count, "");
            views.setTextViewText(R.id.month_label, "Toque para configurar");
            views.setViewVisibility(R.id.prev_month, android.view.View.GONE);
            views.setViewVisibility(R.id.next_month, android.view.View.GONE);
            PendingIntent configure = configurationIntent(context, widgetId);
            views.setViewVisibility(R.id.empty_hint, android.view.View.VISIBLE);
            views.setViewVisibility(R.id.habit_list, android.view.View.GONE);
            views.setOnClickPendingIntent(R.id.empty_hint, configure);
            manager.updateAppWidget(widgetId, views);
            return;
        }

        int total = 0;
        for (HabitStore.Habit habit : habits) total += HabitStore.count(context, habit.id);
        views.setTextViewText(R.id.habit_title, habits.size() == 1 ? habits.get(0).name : habits.size() + " hábitos");
        views.setTextViewText(R.id.total_count, total + " dias");
        views.setTextViewText(R.id.month_label, monthLabel(month));
        views.setViewVisibility(R.id.prev_month, android.view.View.VISIBLE);
        views.setViewVisibility(R.id.next_month, android.view.View.VISIBLE);
        views.setViewVisibility(R.id.empty_hint, android.view.View.GONE);
        views.setViewVisibility(R.id.habit_list, android.view.View.VISIBLE);
        views.setOnClickPendingIntent(R.id.habit_title, configurationIntent(context, widgetId));
        views.setOnClickPendingIntent(R.id.prev_month, monthIntent(context, widgetId, -1));
        views.setOnClickPendingIntent(R.id.next_month, monthIntent(context, widgetId, 1));
        Intent service = new Intent(context, HabitRemoteViewsService.class);
        service.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        service.setData(Uri.parse("loopdots://collection/" + widgetId));
        views.setRemoteAdapter(R.id.habit_list, service);
        Intent template = new Intent(context, LoopDotsWidgetProvider.class);
        template.setAction(ACTION_TOGGLE);
        template.putExtra(EXTRA_WIDGET, widgetId);
        template.setData(Uri.parse("loopdots://tap/" + widgetId));
        PendingIntent pendingTemplate = PendingIntent.getBroadcast(context, 0, template,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
        views.setPendingIntentTemplate(R.id.habit_list, pendingTemplate);
        manager.updateAppWidget(widgetId, views);
        manager.notifyAppWidgetViewDataChanged(widgetId, R.id.habit_list);
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

    private static int glassTint(int opacity, int tone) {
        int alpha = Math.max(1, Math.min(254, Math.round(opacity * 255f / 100f)));
        int rgb = tone == 1 ? 230 : tone == 0 ? 115 : 35;
        return android.graphics.Color.argb(alpha, rgb, rgb, rgb);
    }

    private static Bitmap glassBitmap(int opacity, int tone) {
        int size = 160;
        Bitmap b = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        int alpha = Math.round(Math.max(0, Math.min(95, opacity)) * 255f / 100f);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int light = tone == 1 ? 225 : (tone == 0 ? 120 : 90);
        int dark = tone == 1 ? 165 : (tone == 0 ? 66 : 29);
        paint.setShader(new android.graphics.LinearGradient(0, 0, size, size,
                new int[] {android.graphics.Color.argb(alpha, light, light, Math.min(255, light + 4)),
                           android.graphics.Color.argb(alpha, dark, dark, Math.min(255, dark + 9))},
                null, android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRoundRect(2, 2, size - 2, size - 2, 17, 17, paint);
        paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(0.35f);
        paint.setColor(android.graphics.Color.argb(Math.min(35, alpha / 9), 255, 255, 255));
        canvas.drawRoundRect(2, 2, size - 2, size - 2, 17, 17, paint);
        return b;
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

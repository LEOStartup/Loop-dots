package com.leostartup.loopdots;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.widget.RemoteViews;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class LoopDotsWidgetProvider extends AppWidgetProvider {
    private static final String PREFS = "loop_dots";
    private static final String ACTION_TOGGLE = "com.leostartup.loopdots.TOGGLE_DAY";

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateWidget(context, manager, id);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE.equals(intent.getAction())) {
            int day = intent.getIntExtra("day", -1);
            String month = intent.getStringExtra("month");
            Calendar now = Calendar.getInstance();
            String currentMonth = monthKey(now);

            if (day > 0 && currentMonth.equals(month)) {
                toggleDay(context, currentMonth, day);
            }

            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            int[] ids = manager.getAppWidgetIds(
                    new ComponentName(context, LoopDotsWidgetProvider.class));
            for (int id : ids) updateWidget(context, manager, id);
        }
    }

    private void toggleDay(Context context, String monthKey, int day) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> done = new HashSet<>(prefs.getStringSet(monthKey, new HashSet<>()));
        String value = String.valueOf(day);

        if (!done.add(value)) {
            done.remove(value);
        }

        prefs.edit().putStringSet(monthKey, done).apply();
    }

    private void updateWidget(Context context, AppWidgetManager manager, int widgetId) {
        Calendar now = Calendar.getInstance();
        int daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);
        String monthKey = monthKey(now);

        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> done = new HashSet<>(prefs.getStringSet(monthKey, new HashSet<>()));

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget);
        views.setTextViewText(R.id.habit_title, "Hábito");
        views.setTextViewText(R.id.month_label, monthLabel(now));
        views.setTextViewText(R.id.total_count, totalDoneLabel(prefs));

        // Clear previously added rows before rebuilding the entire month.
        views.removeAllViews(R.id.dots_container);
        RemoteViews currentRow = new RemoteViews(context.getPackageName(), R.layout.dot_row);

        Bitmap filledDot = dotBitmap(0xFFFF3030);
        Bitmap emptyDot = dotBitmap(0x55FFFFFF);

        for (int day = 1; day <= daysInMonth; day++) {
            if ((day - 1) % 7 == 0 && day > 1) {
                views.addView(R.id.dots_container, currentRow);
                currentRow = new RemoteViews(context.getPackageName(), R.layout.dot_row);
            }

            RemoteViews dot = new RemoteViews(context.getPackageName(), R.layout.dot_item);
            // Render the selected state into the actual image, rather than relying on
            // a nested drawable resource update that some launchers fail to reapply.
            dot.setImageViewBitmap(R.id.dot_visual,
                    done.contains(String.valueOf(day)) ? filledDot : emptyDot);
            dot.setContentDescription(R.id.dot_touch, "Dia " + day);

            Intent toggle = new Intent(context, LoopDotsWidgetProvider.class)
                    .setAction(ACTION_TOGGLE)
                    .putExtra("day", day)
                    .putExtra("month", monthKey);

            int requestCode = widgetId * 1000 + day;
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    toggle,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            dot.setOnClickPendingIntent(R.id.dot_touch, pendingIntent);
            currentRow.addView(R.id.dot_row, dot);
        }

        views.addView(R.id.dots_container, currentRow);
        manager.updateAppWidget(widgetId, views);
    }

    private Bitmap dotBitmap(int color) {
        Bitmap bitmap = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(color);
        canvas.drawCircle(36f, 36f, 35f, paint);
        return bitmap;
    }

    private String totalDoneLabel(SharedPreferences prefs) {
        int total = 0;
        for (Object value : prefs.getAll().values()) {
            if (value instanceof Set<?>) total += ((Set<?>) value).size();
        }
        return total + " dias";
    }

    private String monthKey(Calendar calendar) {
        return String.format(Locale.US, "%04d-%02d",
                calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1);
    }

    private String monthLabel(Calendar calendar) {
        String month = new DateFormatSymbols(new Locale("pt", "BR"))
                .getMonths()[calendar.get(Calendar.MONTH)];
        return month.substring(0, 1).toUpperCase(Locale.ROOT)
                + month.substring(1) + " " + calendar.get(Calendar.YEAR);
    }
}

package com.leostartup.loopdots;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

public class HabitRemoteViewsService extends RemoteViewsService {
    @Override public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new Factory(getApplicationContext(), intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID));
    }

    private static final class Factory implements RemoteViewsFactory {
        private final Context context;
        private final int widgetId;
        private List<HabitStore.Habit> habits = new ArrayList<>();
        private Calendar month;
        private int dotSize;
        private int columns;

        Factory(Context context, int widgetId) {
            this.context = context;
            this.widgetId = widgetId;
        }

        @Override public void onCreate() { onDataSetChanged(); }
        @Override public void onDestroy() { habits.clear(); }

        @Override public void onDataSetChanged() {
            habits = new ArrayList<>();
            for (String id : WidgetConfigActivity.habitIds(context, widgetId)) {
                HabitStore.Habit habit = HabitStore.get(context, id);
                if (habit != null) habits.add(habit);
            }
            month = Calendar.getInstance();
            month.set(Calendar.DAY_OF_MONTH, 1);
            month.add(Calendar.MONTH, WidgetConfigActivity.monthOffset(context, widgetId));
            android.os.Bundle options = AppWidgetManager.getInstance(context).getAppWidgetOptions(widgetId);
            int width = Math.max(160, options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 280));
            // Do not force overflow on narrow launchers; tap targets remain a full column wide.
            columns = width >= 330 ? 16 : (width >= 260 ? 14 : 10);
            int baseDotSize = Math.max(9, Math.min(20, (width - 30) / (columns + 2)));
            dotSize = Math.max(6, Math.min(28, Math.round(baseDotSize * WidgetConfigActivity.dotScale(context, widgetId) / 100f)));
        }

        @Override public int getCount() { return habits.size(); }
        @Override public int getViewTypeCount() { return 1; }
        @Override public boolean hasStableIds() { return true; }
        @Override public long getItemId(int position) { return habits.get(position).id.hashCode(); }
        @Override public RemoteViews getLoadingView() { return null; }

        @Override public RemoteViews getViewAt(int position) {
            if (position < 0 || position >= habits.size()) return null;
            HabitStore.Habit habit = habits.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_habit);
            views.setTextViewText(R.id.habit_row_title, habit.icon + "  " + habit.name);
            views.setTextColor(R.id.habit_row_title, habit.color);
            float textScale = WidgetConfigActivity.textScale(context, widgetId) / 100f;
            views.setTextViewTextSize(R.id.habit_row_title, android.util.TypedValue.COMPLEX_UNIT_SP, 13f * textScale);
            views.setTextViewTextSize(R.id.habit_row_count, android.util.TypedValue.COMPLEX_UNIT_SP, 12f * textScale);
            views.setTextViewText(R.id.habit_row_count, HabitStore.count(context, habit.id) + " dias");
            views.removeAllViews(R.id.habit_row_dots);
            Set<String> marked = HabitStore.done(context, habit.id);
            int days = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            String monthKey = HabitStore.monthKey(month);
            Bitmap filled = circle(habit.color, dotSize);
            Bitmap empty = circle(0x77FFFFFF, dotSize);
            // Compact horizontal rows adapt to the current launcher width.
            for (int row = 0; row < (days + columns - 1) / columns; row++) {
                RemoteViews week = new RemoteViews(context.getPackageName(), R.layout.widget_week);
                for (int col = 0; col < columns; col++) {
                    int day = row * columns + col + 1;
                    RemoteViews dot = new RemoteViews(context.getPackageName(), R.layout.widget_day);
                    if (day <= days) {
                        Calendar date = (Calendar) month.clone();
                        date.set(Calendar.DAY_OF_MONTH, day);
                        boolean done = marked.contains(HabitStore.dateKey(date));
                        dot.setImageViewBitmap(R.id.day_dot, done ? filled : empty);
                        dot.setContentDescription(R.id.day_touch, habit.name + ", dia " + day);
                        Intent fill = new Intent();
                        fill.putExtra(LoopDotsWidgetProvider.EXTRA_HABIT, habit.id);
                        fill.putExtra(LoopDotsWidgetProvider.EXTRA_DAY, day);
                        fill.putExtra(LoopDotsWidgetProvider.EXTRA_MONTH, monthKey);
                        dot.setOnClickFillInIntent(R.id.day_touch, fill);
                        dot.setOnClickFillInIntent(R.id.day_dot, fill);
                    } else {
                        dot.setViewVisibility(R.id.day_dot, android.view.View.INVISIBLE);
                    }
                    week.addView(R.id.week_row, dot);
                }
                views.addView(R.id.habit_row_dots, week);
            }
            return views;
        }

        private static Bitmap circle(int color, int size) {
            float density = android.content.res.Resources.getSystem().getDisplayMetrics().density;
            int visualPx = Math.max(12, Math.round(size * density));
            int gapPx = Math.max(2, Math.round(2f * density));
            int canvasPx = visualPx + gapPx * 2;
            Bitmap b = Bitmap.createBitmap(canvasPx, canvasPx, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            canvas.drawCircle(canvasPx / 2f, canvasPx / 2f, visualPx / 2f - 1f, p);
            return b;
        }
    }
}

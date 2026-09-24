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
            dotSize = Math.max(10, Math.min(22, (width - 36) / 14));
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
            views.setTextViewText(R.id.habit_row_count, HabitStore.count(context, habit.id) + " dias");
            views.removeAllViews(R.id.habit_row_dots);
            Set<String> marked = HabitStore.done(context, habit.id);
            int days = month.getActualMaximum(Calendar.DAY_OF_MONTH);
            String monthKey = HabitStore.monthKey(month);
            Bitmap filled = circle(habit.color, dotSize);
            Bitmap empty = circle(0x77FFFFFF, dotSize);
            // Fourteen columns, two or three rows, give the horizontal reference its compact density.
            for (int row = 0; row < 3; row++) {
                RemoteViews week = new RemoteViews(context.getPackageName(), R.layout.widget_week);
                for (int col = 0; col < 14; col++) {
                    int day = row * 14 + col + 1;
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
            int px = Math.max(16, Math.round(size *
                    android.content.res.Resources.getSystem().getDisplayMetrics().density));
            Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(b);
            Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
            p.setColor(color);
            canvas.drawCircle(px / 2f, px / 2f, px / 2f - 1f, p);
            return b;
        }
    }
}

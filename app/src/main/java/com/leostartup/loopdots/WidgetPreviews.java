package com.leostartup.loopdots;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

final class WidgetPreviews {
    private static final int VERSION = 1;
    static final int[] LAYOUTS = {R.layout.preview_small, R.layout.preview_compact,
        R.layout.preview_medium, R.layout.preview_grid, R.layout.preview_wide};

    static void publish(Context context) {
        if (Build.VERSION.SDK_INT < 35) return;
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        SharedPreferences prefs = context.getSharedPreferences("widget_previews", 0);
        for (int i = 0; i < LAYOUTS.length; i++) {
            String key = "version_" + i;
            if (prefs.getInt(key, 0) == VERSION) continue;
            try {
                boolean accepted = manager.setWidgetPreview(
                    new ComponentName(context, LoopDotsWidgetProvider.TYPES[i]),
                    AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                    new RemoteViews(context.getPackageName(), LAYOUTS[i]));
                if (accepted) prefs.edit().putInt(key, VERSION).apply();
            } catch (RuntimeException error) {
                android.util.Log.w("LoopDots", "Prévia gerada indisponível; usando prévia XML", error);
            }
        }
    }
}

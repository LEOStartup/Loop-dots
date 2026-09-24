package com.leostartup.loopdots;

import android.app.Activity;
import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout list;
    private int chosenColor = HabitStore.COLORS[0];
    private String chosenIcon = "✓";

    private int dp(float value) { return (int) (getResources().getDisplayMetrics().density * value + .5f); }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(radius)); return g;
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value); v.setTextSize(sp); v.setTextColor(color);
        v.setGravity(Gravity.CENTER_VERTICAL); return v;
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL); return v;
    }

    private void gap(LinearLayout parent, int height) {
        View v = new View(this); parent.addView(v, new LinearLayout.LayoutParams(1, dp(height)));
    }

    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showHome();
    }

    @Override protected void onResume() {
        super.onResume();
        if (list != null) renderList();
    }

    private void showHome() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF101114);
        LinearLayout root = column();
        root.setPadding(dp(22), dp(42), dp(22), dp(28));
        scroll.addView(root);
        TextView title = text("Loop Dots", 30, Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        root.addView(title);
        gap(root, 7);
        root.addView(text("Seus hábitos, um dia de cada vez.", 14, 0xFFB4B4BA));
        gap(root, 24);
        Button add = new Button(this);
        add.setText("+  Novo hábito");
        add.setTextColor(Color.WHITE);
        add.setAllCaps(false);
        add.setBackground(bg(0xFF33353B, 15));
        add.setOnClickListener(v -> editHabit(null));
        root.addView(add, new LinearLayout.LayoutParams(-1, dp(54)));
        gap(root, 24);
        list = column();
        root.addView(list);
        gap(root, 20);
        root.addView(text("Toque em um hábito para editar. Cada widget pode mostrar um hábito diferente.", 12, 0xFF9999A0));
        setContentView(scroll);
        renderList();
    }

    private void renderList() {
        if (list == null) return;
        list.removeAllViews();
        List<HabitStore.Habit> habits = HabitStore.all(this);
        if (habits.isEmpty()) {
            list.addView(text("Nenhum hábito cadastrado. Toque em Novo hábito.", 15, 0xFFBBBBBB));
        }
        for (HabitStore.Habit h : habits) {
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(15), dp(12), dp(12), dp(12));
            item.setBackground(bg(0xFF202126, 16));
            TextView marker = text(h.icon, 23, h.color);
            marker.setGravity(Gravity.CENTER);
            item.addView(marker, new LinearLayout.LayoutParams(dp(42), dp(44)));
            LinearLayout labels = column();
            TextView name = text(h.name, 17, Color.WHITE);
            name.setTypeface(null, Typeface.BOLD);
            labels.addView(name);
            labels.addView(text(HabitStore.count(this, h.id) + " dias concluídos", 12, 0xFFAAAAAA));
            item.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
            TextView edit = text("Editar  ›", 13, 0xFFCCCCCC);
            item.addView(edit);
            item.setOnClickListener(v -> editHabit(h));
            list.addView(item);
            gap(list, 10);
        }
        TextView info = text("Adicione o widget pela tela inicial do Android e selecione o hábito.", 12, 0xFF9999A0);
        info.setPadding(0, dp(10), 0, 0);
        list.addView(info);
    }

    private void editHabit(HabitStore.Habit current) {
        chosenColor = current == null ? HabitStore.COLORS[0] : current.color;
        chosenIcon = current == null ? "✓" : current.icon;
        LinearLayout form = column();
        form.setPadding(dp(20), dp(8), dp(20), 0);
        EditText name = new EditText(this);
        name.setSingleLine(true);
        name.setHint("Ex.: Meditação, correr com o cachorro...");
        name.setText(current == null ? "" : current.name);
        form.addView(name, new LinearLayout.LayoutParams(-1, dp(62)));
        gap(form, 12);
        form.addView(text("Cor das bolinhas", 14, 0xFF777777));
        LinearLayout colors = new LinearLayout(this);
        colors.setGravity(Gravity.CENTER_VERTICAL);
        for (int c : HabitStore.COLORS) {
            TextView chip = text("●", 28, c);
            chip.setGravity(Gravity.CENTER);
            colors.addView(chip, new LinearLayout.LayoutParams(0, dp(46), 1));
            chip.setOnClickListener(v -> {
                chosenColor = c;
                for (int i = 0; i < colors.getChildCount(); i++) {
                    colors.getChildAt(i).setAlpha(colors.getChildAt(i) == chip ? 1f : .36f);
                }
            });
            chip.setAlpha(c == chosenColor ? 1f : .36f);
        }
        form.addView(colors);
        gap(form, 12);
        form.addView(text("Ícone", 14, 0xFF777777));
        final String[] icons = {"✓", "✦", "♥", "★", "☀", "●"};
        LinearLayout iconRow = new LinearLayout(this);
        for (String icon : icons) {
            TextView chip = text(icon, 23, 0xFF333333);
            chip.setGravity(Gravity.CENTER);
            iconRow.addView(chip, new LinearLayout.LayoutParams(0, dp(45), 1));
            chip.setAlpha(icon.equals(chosenIcon) ? 1f : .36f);
            chip.setOnClickListener(v -> {
                chosenIcon = icon;
                for (int i = 0; i < iconRow.getChildCount(); i++) {
                    iconRow.getChildAt(i).setAlpha(iconRow.getChildAt(i) == chip ? 1f : .36f);
                }
            });
        }
        form.addView(iconRow);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(current == null ? "Novo hábito" : "Editar hábito")
                .setView(form)
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", null)
                .create();
        if (current != null) dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Excluir", (d, which) -> {});
        dialog.setOnShowListener(v -> {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(b -> {
                String value = name.getText().toString().trim();
                if (value.isEmpty()) {
                    name.setError("Informe o nome do hábito");
                    return;
                }
                HabitStore.save(this, current == null ? "" : current.id, value, chosenColor, chosenIcon);
                LoopDotsWidgetProvider.refreshAll(this);
                renderList();
                dialog.dismiss();
            });
            if (current != null) dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(b ->
                    new AlertDialog.Builder(this)
                            .setTitle("Excluir " + current.name + "?")
                            .setMessage("Os registros deste hábito serão apagados.")
                            .setPositiveButton("Excluir", (confirm, which) -> {
                                HabitStore.delete(this, current.id);
                                LoopDotsWidgetProvider.refreshAll(this);
                                renderList(); dialog.dismiss();
                            })
                            .setNegativeButton("Cancelar", null).show());
        });
        dialog.show();
    }
}

package com.leostartup.loopdots;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.view.Gravity;
import android.graphics.Color;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32, 32, 32, 32);
        root.setBackgroundColor(Color.TRANSPARENT);

        TextView title = new TextView(this);
        title.setText("Loop Dots");
        title.setTextSize(24);
        title.setGravity(Gravity.CENTER);

        TextView info = new TextView(this);
        info.setText("Widget de hábitos\n\nAdicione o widget Loop Dots à tela inicial.\n\nPara remover o aplicativo, desinstale normalmente pelas Configurações do Android.");
        info.setTextSize(16);
        info.setGravity(Gravity.CENTER);
        info.setPadding(0, 24, 0, 0);

        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(info, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        setContentView(root);
    }
}

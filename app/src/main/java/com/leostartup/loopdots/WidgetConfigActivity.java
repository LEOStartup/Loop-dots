package com.leostartup.loopdots;
import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.widget.*;
import java.util.*;
import org.json.*;

public class WidgetConfigActivity extends Activity {
    private int widgetId;private static final String PREFS="widget_config";
    public static List<String> habitIds(Context c,int id){android.content.SharedPreferences p=c.getSharedPreferences(PREFS,0);List<String> out=new ArrayList<>();String raw=p.getString("multi_"+id,null);try{if(raw!=null){JSONArray a=new JSONArray(raw);for(int i=0;i<a.length();i++)if(DataStore.habit(c,a.optString(i))!=null)out.add(a.optString(i));}else{String old=p.getString("widget_"+id,null);if(old!=null&&DataStore.habit(c,old)!=null)out.add(old);}}catch(Exception ignored){}return out;}
    public static String habitId(Context c,int id){List<String> a=habitIds(c,id);return a.isEmpty()?null:a.get(0);}
    public static int dotScale(Context c,int id){return c.getSharedPreferences(PREFS,0).getInt("dot_size_"+id,100);}
    public static int textScale(Context c,int id){return c.getSharedPreferences(PREFS,0).getInt("text_size_"+id,100);}
    public static boolean glass(Context c,int id){return c.getSharedPreferences(PREFS,0).getBoolean("glass_"+id,false);}
    public static int opacity(Context c,int id){return c.getSharedPreferences(PREFS,0).getInt("alpha_"+id,80);}
    public static int tone(Context c,int id){return c.getSharedPreferences(PREFS,0).getInt("tone_"+id,2);}
    public static int monthOffset(Context c,int id){return c.getSharedPreferences(PREFS,0).getInt("month_"+id,0);}
    public static int background(Context c,int id){return c.getSharedPreferences(PREFS,0).getInt("background_"+id,glass(c,id)?1:0);}
    public static void changeMonth(Context c,int id,int delta){c.getSharedPreferences(PREFS,0).edit().putInt("month_"+id,monthOffset(c,id)+delta).apply();}
    public static void saveHabits(Context c,int id,List<String> ids){c.getSharedPreferences(PREFS,0).edit().putString("multi_"+id,new JSONArray(ids).toString()).apply();}
    public static void clear(Context c,int id){android.content.SharedPreferences.Editor e=c.getSharedPreferences(PREFS,0).edit();for(String prefix:new String[]{"widget_","multi_","month_","glass_","alpha_","tone_","dot_size_","text_size_","background_"})e.remove(prefix+id);e.apply();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private TextView label(String t,int sp){TextView v=new TextView(this);v.setText(t);v.setTextColor(Color.WHITE);v.setTextSize(sp);v.setPadding(0,dp(12),0,dp(7));return v;}
    @Override public void onCreate(Bundle b){super.onCreate(b);setResult(RESULT_CANCELED);widgetId=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(widgetId==-1){finish();return;}
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(0xff101010);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(22),dp(30),dp(22),dp(30));scroll.addView(root);root.addView(label("Configurar widget",26));
        root.addView(label("Escolha os hábitos",16));JSONArray hs=DataStore.habits(this);List<String> ids=new ArrayList<>();List<CheckBox> checks=new ArrayList<>();List<String> selected=habitIds(this,widgetId);boolean compact=LoopDotsWidgetProvider.kind(this,widgetId)==1;
        for(int i=0;i<hs.length();i++){JSONObject h=hs.optJSONObject(i);if(h==null||h.optBoolean("archived"))continue;String id=h.optString("id");CheckBox cb=new CheckBox(this);cb.setText(h.optString("icon")+"  "+h.optString("name"));cb.setTextColor(Color.WHITE);cb.setTextSize(17);cb.setButtonTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(h.optString("color","#ef4444"))));cb.setChecked(selected.contains(id));root.addView(cb);ids.add(id);checks.add(cb);cb.setOnCheckedChangeListener((v,on)->{if(on&&!compact)for(CheckBox other:checks)if(other!=cb)other.setChecked(false);});}
        root.addView(label(compact?"A lista compacta mostra vários hábitos.":"Este formato mostra um hábito por widget.",12));
        if(ids.isEmpty()){Button create=new Button(this);create.setText("Criar hábito no aplicativo");root.addView(create);create.setOnClickListener(v->startActivity(new Intent(this,MainActivity.class)));}
        root.addView(label("Fundo",16));RadioGroup bg=new RadioGroup(this);RadioButton[] bgButtons=new RadioButton[4];final int[] chosenBg={Math.max(0,Math.min(3,background(this,widgetId)))};String[] names={"Transparente","Translúcido","Escuro","Claro"};for(int i=0;i<4;i++){RadioButton rb=new RadioButton(this);rb.setId(android.view.View.generateViewId());rb.setText(names[i]);rb.setTextColor(Color.WHITE);bg.addView(rb);bgButtons[i]=rb;}bg.check(bgButtons[chosenBg[0]].getId());root.addView(bg);
        TextView aLabel=label("Opacidade: "+opacity(this,widgetId)+"%",15);root.addView(aLabel);SeekBar alpha=new SeekBar(this);alpha.setMax(100);alpha.setProgress(opacity(this,widgetId));root.addView(alpha);
        TextView dLabel=label("Tamanho das marcas: "+dotScale(this,widgetId)+"%",15);root.addView(dLabel);SeekBar dot=new SeekBar(this);dot.setMax(70);dot.setProgress(Math.max(0,Math.min(70,dotScale(this,widgetId)-70)));root.addView(dot);
        TextView tLabel=label("Tamanho do texto: "+textScale(this,widgetId)+"%",15);root.addView(tLabel);SeekBar text=new SeekBar(this);text.setMax(60);text.setProgress(Math.max(0,Math.min(60,textScale(this,widgetId)-70)));root.addView(text);
        Runnable preview=()->{getSharedPreferences(PREFS,0).edit().putInt("background_"+widgetId,chosenBg[0]).putInt("alpha_"+widgetId,alpha.getProgress()).putInt("dot_size_"+widgetId,dot.getProgress()+70).putInt("text_size_"+widgetId,text.getProgress()+70).apply();LoopDotsWidgetProvider.updateWidget(this,AppWidgetManager.getInstance(this),widgetId);};
        SeekBar.OnSeekBarChangeListener listener=new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean user){aLabel.setText("Opacidade: "+alpha.getProgress()+"%");dLabel.setText("Tamanho das marcas: "+(dot.getProgress()+70)+"%");tLabel.setText("Tamanho do texto: "+(text.getProgress()+70)+"%");if(user)preview.run();}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){preview.run();}};alpha.setOnSeekBarChangeListener(listener);dot.setOnSeekBarChangeListener(listener);text.setOnSeekBarChangeListener(listener);bg.setOnCheckedChangeListener((g,checked)->{for(int i=0;i<bgButtons.length;i++)if(bgButtons[i].getId()==checked)chosenBg[0]=i;preview.run();});
        root.addView(label("O tamanho se ajusta ao espaço disponível. Toque no título para abrir o calendário; ⚙ abre esta configuração.",12));Button save=new Button(this);save.setText("Salvar e atualizar widget");save.setAllCaps(false);save.setTextColor(Color.BLACK);save.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xff91c443));root.addView(save,new LinearLayout.LayoutParams(-1,dp(54)));save.setOnClickListener(v->{List<String> chosen=new ArrayList<>();for(int i=0;i<checks.size();i++)if(checks.get(i).isChecked()){chosen.add(ids.get(i));if(!compact)break;}if(chosen.isEmpty()){Toast.makeText(this,"Selecione um hábito",Toast.LENGTH_SHORT).show();return;}saveHabits(this,widgetId,chosen);preview.run();setResult(RESULT_OK,new Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widgetId));finish();});setContentView(scroll);
        root.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(dp(22),dp(22)+insets.getSystemWindowInsetTop(),dp(22),dp(22)+insets.getSystemWindowInsetBottom());return insets;});
    }
}

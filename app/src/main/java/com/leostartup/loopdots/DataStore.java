package com.leostartup.loopdots;
import android.content.Context;
import org.json.*;
import java.util.*;

public final class DataStore {
    private static final String DOC="document_v3";
    public static synchronized JSONObject load(Context c) {
        android.content.SharedPreferences p=c.getSharedPreferences("loop_dots",0);
        String raw=p.getString(DOC,null);
        if(raw!=null)try{return new JSONObject(raw);}catch(Exception e){throw new IllegalStateException("Dados inválidos; restaure seu backup.",e);}
        JSONObject root=new JSONObject();JSONArray habits=new JSONArray();
        try {
            for(HabitStore.Habit h:HabitStore.all(c)){
                JSONObject o=new JSONObject();o.put("id",h.id);o.put("name",h.name);o.put("color",String.format(Locale.US,"#%06X",h.color&0xffffff));o.put("icon",h.icon);
                o.put("description","");o.put("target",1);o.put("showStreak",true);o.put("archived",false);o.put("mode","step");o.put("type","build");o.put("goalPeriod","none");
                JSONObject entries=new JSONObject();String created=today();for(String date:HabitStore.done(c,h.id)){entries.put(date,1);if(date.compareTo(created)<0)created=date;}
                o.put("entries",entries);o.put("created",created);o.put("notes",new JSONObject());o.put("categories",new JSONArray());o.put("schedule",new JSONArray());o.put("reminderDays",new JSONArray());o.put("reminderTime","12:00");habits.put(o);
            }
            root.put("version",3);root.put("habits",habits);root.put("settings",new JSONObject());root.put("categories",new JSONArray("[\"Arte\",\"Estudo\",\"Finanças\",\"Fitness\",\"Nutrição\",\"Saúde\",\"Social\",\"Trabalho\",\"Outro\",\"Manhã\",\"Dia\",\"Noite\"]"));
            // Keep the old habits and dated records intact as a migration fallback.
            if(!p.edit().putString("migration_snapshot_v3",root.toString()).putString(DOC,root.toString()).commit())throw new IllegalStateException("Falha ao gravar migração");
            return root;
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    public static synchronized void save(Context c,String raw) throws JSONException {
        if(raw.length()>10_000_000)throw new JSONException("Backup muito grande");
        JSONObject root=new JSONObject(raw);JSONArray hs=root.getJSONArray("habits");Set<String> ids=new HashSet<>();
        for(int i=0;i<hs.length();i++){JSONObject h=hs.getJSONObject(i);String id=h.getString("id");if(!id.matches("[A-Za-z0-9_-]{1,100}")||!ids.add(id))throw new JSONException("Identificador inválido");if(h.getString("name").length()>300)throw new JSONException("Nome inválido");if(!h.optString("color").matches("#[0-9a-fA-F]{6}"))throw new JSONException("Cor inválida");JSONObject es=h.optJSONObject("entries");if(es==null)throw new JSONException("Registros ausentes");Iterator<String> it=es.keys();while(it.hasNext()){String k=it.next();double value=es.getDouble(k);if(!k.matches("\\d{4}-\\d{2}-\\d{2}")||value<0||!Double.isFinite(value))throw new JSONException("Registro inválido");}}
        android.content.SharedPreferences p=c.getSharedPreferences("loop_dots",0);
        if(!p.edit().putString("previous_document_v3",p.getString(DOC,raw)).putString(DOC,raw).commit())throw new JSONException("Falha ao salvar");
    }
    public static JSONArray habits(Context c){return load(c).optJSONArray("habits");}
    public static JSONObject habit(Context c,String id){JSONArray hs=habits(c);for(int i=0;i<hs.length();i++){JSONObject h=hs.optJSONObject(i);if(h!=null&&id.equals(h.optString("id")))return h;}return null;}
    public static String today(){return dateKey(Calendar.getInstance());}
    public static String dateKey(Calendar d){return String.format(Locale.US,"%04d-%02d-%02d",d.get(Calendar.YEAR),d.get(Calendar.MONTH)+1,d.get(Calendar.DAY_OF_MONTH));}
    public static int count(JSONObject h){int n=0;JSONObject es=h.optJSONObject("entries");if(es==null)return 0;Iterator<String> it=es.keys();while(it.hasNext()){String k=it.next();if(k.compareTo(today())<=0&&es.optDouble(k)>=h.optDouble("target",1))n++;}return n;}
    public static int streak(JSONObject h){int n=0;Calendar d=Calendar.getInstance();if(!done(h,dateKey(d)))d.add(Calendar.DATE,-1);for(int i=0;i<40000;i++){JSONArray plan=h.optJSONArray("schedule");boolean eligible=plan==null||plan.length()==0;for(int j=0;plan!=null&&j<plan.length();j++)if(plan.optInt(j)==d.get(Calendar.DAY_OF_WEEK)-1)eligible=true;if(!eligible){d.add(Calendar.DATE,-1);continue;}if(!done(h,dateKey(d)))break;n++;d.add(Calendar.DATE,-1);}return n;}
    public static boolean done(JSONObject h,String k){JSONObject e=h.optJSONObject("entries");return e!=null&&e.optDouble(k)>=Math.max(1,h.optDouble("target",1));}
    public static synchronized void toggle(Context c,String id,String key){if(key.compareTo(today())>0)return;try{JSONObject root=load(c);JSONArray hs=root.getJSONArray("habits");for(int i=0;i<hs.length();i++){JSONObject h=hs.getJSONObject(i);if(id.equals(h.optString("id"))){JSONObject es=h.getJSONObject("entries");double v=es.optDouble(key),target=Math.max(1,h.optDouble("target",1));es.put(key,v>=target?0:v+1);break;}}save(c,root.toString());}catch(Exception e){android.util.Log.e("LoopDots","Falha ao registrar dia",e);}}
}

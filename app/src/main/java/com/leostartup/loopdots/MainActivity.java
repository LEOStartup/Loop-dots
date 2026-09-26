package com.leostartup.loopdots;
import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.webkit.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    // Widget bitmap generation must not occupy the UI thread during a touch animation.
    private static final ScheduledExecutorService widgetUpdates=Executors.newSingleThreadScheduledExecutor();
    private static ScheduledFuture<?> pendingWidgets;
    private static synchronized void refreshWidgets(Context context){
        Context app=context.getApplicationContext();
        if(pendingWidgets!=null)pendingWidgets.cancel(false);
        pendingWidgets=widgetUpdates.schedule(()->{LoopDotsWidgetProvider.refreshAll(app);ReminderReceiver.schedule(app);},120,TimeUnit.MILLISECONDS);
    }
    private WebView web; private boolean loaded=false; private String initial="";
    @Override public void onCreate(Bundle b){super.onCreate(b);initial=getIntent().getStringExtra("habit");if(initial==null)initial="";
        if(Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(false);else getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        getWindow().setStatusBarColor(0xff101010);getWindow().setNavigationBarColor(0xff030303);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(0xff101010);
        root.setOnApplyWindowInsetsListener((v,insets)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.ime());v.setPadding(i.left,i.top,i.right,i.bottom);}else v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
        web=new WebView(this);web.setBackgroundColor(0xff101010);root.addView(web,new LinearLayout.LayoutParams(-1,-1));setContentView(root);
        WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(false);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setAllowFileAccessFromFileURLs(false);s.setAllowUniversalAccessFromFileURLs(false);s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(),"Android");web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView w,WebResourceRequest r){return !r.getUrl().toString().startsWith("file:///android_asset/");}@Override public void onPageFinished(WebView w,String u){loaded=true;}});
        web.loadUrl("file:///android_asset/index.html");ReminderReceiver.schedule(this);
    }
    @Override protected void onResume(){super.onResume();if(web!=null&&loaded){web.evaluateJavascript("refreshFromNative()",null);ReminderReceiver.schedule(this);}}
    @Override protected void onNewIntent(Intent i){super.onNewIntent(i);setIntent(i);String id=i.getStringExtra("habit");if(id!=null&&web!=null)web.evaluateJavascript("refreshFromNative();openHabit("+JSONObject.quote(id)+")",null);}
    @Override public void onBackPressed(){if(web==null){super.onBackPressed();return;}web.evaluateJavascript("nativeBack()",result->{if(!"true".equals(result))finish();});}
    private void toast(String t){runOnUiThread(()->Toast.makeText(this,t,Toast.LENGTH_LONG).show());}
    private void call(String script){runOnUiThread(()->web.evaluateJavascript(script,null));}
    public final class Bridge {
        @JavascriptInterface public String load(){return DataStore.load(MainActivity.this).toString();}
        @JavascriptInterface public String initialHabit(){return initial;}
        @JavascriptInterface public void overlay(boolean open){}
        @JavascriptInterface public void save(String raw){try{DataStore.save(MainActivity.this,raw);refreshWidgets(MainActivity.this);}catch(Exception e){toast("Falha ao salvar: "+e.getMessage());}}
        @JavascriptInterface public void haptic(){runOnUiThread(()->web.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP));}
        @JavascriptInterface public void notifications(){runOnUiThread(()->{if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},30);});}
        @JavascriptInterface public void openUrl(String url){if(!url.startsWith("https://github.com/LEOStartup/Loop-dots"))return;runOnUiThread(()->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){toast("Nenhum navegador disponível");}});}
        @JavascriptInterface public void share(String text){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,text);startActivity(Intent.createChooser(i,"Compartilhar"));});}
        @JavascriptInterface public void exportBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"LoopDots-backup-"+DataStore.today()+".json");startActivityForResult(i,101);});}
        @JavascriptInterface public void importBackup(){runOnUiThread(()->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","text/plain","application/octet-stream"});startActivityForResult(i,102);});}
        @JavascriptInterface public void pinWidget(int type){runOnUiThread(()->{Class<?>[] types={SmallWidget.class,CompactWidget.class,LoopDotsWidgetProvider.class,GridWidget.class,WideWidget.class};int n=Math.max(0,Math.min(4,type));AppWidgetManager m=AppWidgetManager.getInstance(MainActivity.this);if(m.isRequestPinAppWidgetSupported()){m.requestPinAppWidget(new ComponentName(MainActivity.this,types[n]),null,null);toast("Confirme a adição e toque no widget para configurar.");}else toast("Adicione pela lista de widgets da tela inicial.");});}
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;try{if(request==101){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(DataStore.load(this).toString(2).getBytes(StandardCharsets.UTF_8));}toast("Backup salvo");}else if(request==102){String raw;try(InputStream in=getContentResolver().openInputStream(data.getData());ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buf=new byte[8192];int n,total=0;while((n=in.read(buf))!=-1){total+=n;if(total>10_000_000)throw new IOException("Arquivo muito grande");out.write(buf,0,n);}raw=out.toString("UTF-8");}call("onImport("+JSONObject.quote(raw)+")");}}catch(Exception e){toast("Não foi possível ler ou gravar o backup.");}}
}

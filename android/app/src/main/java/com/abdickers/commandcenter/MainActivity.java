package com.abdickers.commandcenter;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {
    private static final int INK=Color.rgb(10,21,16), GOLD=Color.rgb(225,201,141);
    private final Map<String,WebView> pages=new HashMap<>();
    private String realm;
    private FrameLayout content;
    private Button personal,work;
    private TextView status;
    private ProgressBar progress;
    private boolean desktop=false;

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(INK);getWindow().setNavigationBarColor(INK);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(INK);
        if(Build.VERSION.SDK_INT>=30){
            getWindow().setDecorFitsSystemWindows(true);
            root.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets safe=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());v.setPadding(safe.left,safe.top,safe.right,safe.bottom);return insets;});
        }else getWindow().setFlags(1024,1024);
        LinearLayout heading=new LinearLayout(this);heading.setGravity(Gravity.CENTER_VERTICAL);heading.setPadding(dp(12),dp(8),dp(12),dp(4));
        TextView title=new TextView(this);title.setText("✦  POCKET PALANTÍR");title.setTextColor(GOLD);title.setTypeface(Typeface.SERIF);title.setTextSize(16);
        heading.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        Button pin=button("＋ Widget");pin.setContentDescription("Add command center home-screen widget");pin.setOnClickListener(v->pinWidget());heading.addView(pin);
        root.addView(heading);
        LinearLayout modes=new LinearLayout(this);modes.setPadding(dp(8),0,dp(8),0);
        personal=button("PERSONAL · THE SHIRE");work=button("WORK · WHITE TOWER");
        modes.addView(personal,new LinearLayout.LayoutParams(0,dp(52),1));modes.addView(work,new LinearLayout.LayoutParams(0,dp(52),1));
        personal.setOnClickListener(v->select(Realms.PERSONAL));work.setOnClickListener(v->select(Realms.WORK));root.addView(modes);
        progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        content=new FrameLayout(this);root.addView(content,new LinearLayout.LayoutParams(-1,0,1));
        status=new TextView(this);status.setTextColor(GOLD);status.setTextSize(11);status.setPadding(dp(14),dp(5),dp(14),dp(5));status.setVisibility(View.GONE);root.addView(status);
        LinearLayout tools=new LinearLayout(this);tools.setPadding(dp(4),0,dp(4),dp(4));
        addTool(tools,"←",()->{WebView page=current();if(page.canGoBack())page.goBack();},"Back within dashboard");
        addTool(tools,"Refresh",()->current().reload(),"Refresh selected dashboard");
        addTool(tools,"Browser",()->openBrowser(Uri.parse(Realms.url(realm))),"Open selected command center in your browser for browser sign-in");
        addTool(tools,"Fit / desktop",()->{desktop=!desktop;for(WebView w:pages.values()){w.getSettings().setUseWideViewPort(desktop);w.getSettings().setLoadWithOverviewMode(desktop);}current().reload();},"Toggle mobile and desktop page sizing");
        root.addView(tools);setContentView(root);
        String initial=getIntent().getStringExtra("realm");select(initial==null?RealmWidget.selected(this):initial);
        if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,()->{if(current().canGoBack())current().goBack();else finish();});
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused&&Build.VERSION.SDK_INT>=30){WindowInsetsController controller=getWindow().getInsetsController();if(controller!=null){controller.hide(WindowInsets.Type.statusBars());controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}}}
    private Button button(String text){Button b=new Button(this);b.setText(text);b.setTextColor(GOLD);b.setTextSize(11);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(8),0,dp(8),0);b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(24,43,31)));return b;}
    private void addTool(LinearLayout row,String label,Runnable action,String description){Button b=button(label);b.setContentDescription(description);b.setOnClickListener(v->action.run());row.addView(b,new LinearLayout.LayoutParams(0,dp(48),1));}
    private WebView current(){return pages.get(realm);}
    private void message(String text){status.setText(text);status.setVisibility(text.isEmpty()?View.GONE:View.VISIBLE);}
    private void select(String value){
        realm=Realms.normalize(value);RealmWidget.select(this,realm);
        for(WebView w:pages.values()){w.setVisibility(View.GONE);w.onPause();}
        WebView page=pages.get(realm);
        if(page==null){page=createPage();pages.put(realm,page);content.addView(page,new FrameLayout.LayoutParams(-1,-1));page.loadUrl(Realms.url(realm));}
        page.setVisibility(View.VISIBLE);page.onResume();
        personal.setAlpha(Realms.PERSONAL.equals(realm)?1f:.55f);work.setAlpha(Realms.WORK.equals(realm)?1f:.55f);
        personal.setSelected(Realms.PERSONAL.equals(realm));work.setSelected(Realms.WORK.equals(realm));
        progress.setVisibility(page.getProgress()<100?View.VISIBLE:View.GONE);
        message("");
    }
    @SuppressLint("SetJavaScriptEnabled") private WebView createPage(){
        WebView w=new WebView(this);w.setBackgroundColor(INK);
        WebSettings settings=w.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSafeBrowsingEnabled(true);settings.setSupportMultipleWindows(true);settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(w,false);
        w.setWebViewClient(new WebViewClient(){
            @Override public boolean shouldOverrideUrlLoading(WebView page,WebResourceRequest request){
                if(!request.isForMainFrame())return false;
                Uri url=request.getUrl();if(internal(url))return false;
                if(request.hasGesture())openBrowser(url);
                return true;
            }
            @Override public void onPageStarted(WebView page,String url,android.graphics.Bitmap icon){if(page==current()){progress.setVisibility(View.VISIBLE);message("");}}
            @Override public void onPageFinished(WebView page,String url){CookieManager.getInstance().flush();if(page==current())progress.setVisibility(View.GONE);}
            @Override public void onReceivedError(WebView page,WebResourceRequest request,WebResourceError error){if(request.isForMainFrame()&&page==current())message("Couldn’t load this page. Check your connection, retry, or use Browser below.");}
            @Override public void onReceivedSslError(WebView page,SslErrorHandler handler,SslError error){handler.cancel();if(page==current())message("Secure connection failed. The certificate was not accepted.");}
        });
        w.setWebChromeClient(new WebChromeClient(){
            @Override public void onProgressChanged(WebView page,int value){if(page==current()){progress.setProgress(value);progress.setVisibility(value<100?View.VISIBLE:View.GONE);}}
            @Override public boolean onCreateWindow(WebView view,boolean dialog,boolean gesture,android.os.Message result){
                if(!gesture)return false;
                WebView popup=new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView p,WebResourceRequest request){if(!"about".equals(request.getUrl().getScheme())){openBrowser(request.getUrl());p.destroy();}return true;}});
                ((WebView.WebViewTransport)result.obj).setWebView(popup);result.sendToTarget();return true;
            }
        });
        return w;
    }
    private boolean internal(Uri uri){
        if(!"https".equals(uri.getScheme())||uri.getUserInfo()!=null)return false;
        String host=uri.getHost();
        return "adams-middle-earth-command.spirit-of-76-1921.chatgpt.site".equals(host)
            || ("inkedsage.github.io".equals(host)&&uri.getPath()!=null&&uri.getPath().startsWith("/Homepage/"))
            || "auth.openai.com".equals(host)||"chatgpt.com".equals(host);
    }
    private void openBrowser(Uri uri){
        if(!"https".equals(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null){message("Only secure web links can be opened from this app.");return;}
        try{new CustomTabsIntent.Builder().setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder().setToolbarColor(INK).build()).setShowTitle(true).build().launchUrl(this,uri);}
        catch(android.content.ActivityNotFoundException e){try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(android.content.ActivityNotFoundException noBrowser){message("Install a web browser to open this link.");}}
    }
    private void pinWidget(){
        AppWidgetManager manager=AppWidgetManager.getInstance(this);
        if(manager.isRequestPinAppWidgetSupported())manager.requestPinAppWidget(new ComponentName(this,RealmWidget.class),null,null);
        else message("Long-press your home screen, choose Widgets, then Pocket Palantír.");
    }
    @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);String selected=intent.getStringExtra("realm");select(selected==null?RealmWidget.selected(this):selected);}
    @Override protected void onPause(){super.onPause();for(WebView w:pages.values())w.onPause();CookieManager.getInstance().flush();}
    @Override protected void onResume(){super.onResume();if(current()!=null)current().onResume();}
    @Override protected void onDestroy(){for(WebView w:pages.values())w.destroy();super.onDestroy();}
    @Override public void onBackPressed(){if(current().canGoBack())current().goBack();else super.onBackPressed();}
}

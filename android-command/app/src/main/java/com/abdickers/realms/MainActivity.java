package com.abdickers.realms;

import android.app.Activity;
import android.appwidget.*;
import android.content.ComponentName;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.lang.ref.WeakReference;

/** The app and launcher widget share the same native dashboard renderer and data. */
public final class MainActivity extends Activity {
 private static WeakReference<MainActivity> current=new WeakReference<>(null);
 private FrameLayout root;
 private AppWidgetHostView dashboard;
 AppWidgetHostView dashboardView(){return dashboard;}
 private String lastPage="";
 @Override protected void onCreate(Bundle state){super.onCreate(state);root=new FrameLayout(this);root.setId(R.id.app_dashboard);root.setBackgroundColor(0xFF101C2B);setContentView(root);root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(8,bars.top+8,8,bars.bottom+8);return i;});render();}
 @Override protected void onResume(){super.onResume();current=new WeakReference<>(this);render();}
 @Override protected void onPause(){if(current.get()==this)current.clear();super.onPause();}
 static void redraw(){MainActivity a=current.get();if(a!=null&&!a.isFinishing()&&!a.isDestroyed())a.runOnUiThread(()->{if(a.findViewById(R.id.app_dashboard)!=null)a.render();});}
 private void render(){
  if(root==null||findViewById(R.id.app_dashboard)==null)return;
  int position=0,offset=0;String page=WidgetState.decision(this,0).realm()+"."+WidgetState.section(this,0);ListView previous=dashboard==null?null:dashboard.findViewById(R.id.feed);if(page.equals(lastPage)&&previous!=null){position=previous.getFirstVisiblePosition();if(previous.getChildCount()>0)offset=previous.getChildAt(0).getTop();}
  float density=getResources().getDisplayMetrics().density;Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,Math.round(getResources().getDisplayMetrics().widthPixels/density));options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,Math.round(getResources().getDisplayMetrics().heightPixels/density)-50);
  // RemoteCollectionItems requires an AppWidgetHostView even inside our own Activity.
  // This is an in-process view, not a launcher binding or an allocated widget ID.
  dashboard=new AppWidgetHostView(this);dashboard.setPadding(0,0,0,0);
  ComponentName provider=new ComponentName(this,CommandWidget.class);
  for(AppWidgetProviderInfo info:AppWidgetManager.getInstance(this).getInstalledProviders())if(provider.equals(info.provider)){dashboard.setAppWidget(0,info);break;}
  dashboard.updateAppWidget(CommandWidget.views(this,0,options));root.removeAllViews();root.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));if(page.equals(lastPage)){final int savedPosition=position,savedOffset=offset;dashboard.post(()->{ListView list=dashboard.findViewById(R.id.feed);if(list!=null)list.setSelectionFromTop(savedPosition,savedOffset);});}lastPage=page;
 }
}

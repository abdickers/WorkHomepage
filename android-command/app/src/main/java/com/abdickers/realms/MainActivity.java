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
 @Override protected void onCreate(Bundle state){super.onCreate(state);if(!WidgetState.prefs(this).getBoolean("welcome_seen",false)){startActivity(new android.content.Intent(this,AccountActivity.class));finish();return;}root=new FrameLayout(this);root.setId(R.id.app_dashboard);root.setBackgroundColor(0xFF101C2B);setContentView(root);root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(8,bars.top+8,8,bars.bottom+8);return i;});render();}
 @Override protected void onResume(){super.onResume();current=new WeakReference<>(this);render();}
 @Override protected void onPause(){if(current.get()==this)current.clear();super.onPause();}
 static void redraw(){MainActivity a=current.get();if(a!=null&&!a.isFinishing()&&!a.isDestroyed())a.runOnUiThread(()->{if(a.findViewById(R.id.app_dashboard)!=null)a.render();});}
 private void render(){
  if(root==null||findViewById(R.id.app_dashboard)==null)return;
  int position=0,offset=0;String page=WidgetState.decision(this,0).realm()+"."+WidgetState.section(this,0);int collectionId=WidgetState.section(this,0).equals("tools")?R.id.tool_grid:R.id.feed;AbsListView previous=dashboard==null?null:dashboard.findViewById(collectionId);if(page.equals(lastPage)&&previous!=null){position=previous.getFirstVisiblePosition();if(previous.getChildCount()>0)offset=previous.getChildAt(0).getTop();}
  float density=getResources().getDisplayMetrics().density;Bundle options=new Bundle();options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,Math.round(getResources().getDisplayMetrics().widthPixels/density));options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,Math.round(getResources().getDisplayMetrics().heightPixels/density)-50);
  // RemoteCollectionItems requires an AppWidgetHostView even inside our own Activity.
  // This is an in-process view, not a launcher binding or an allocated widget ID.
  if(dashboard==null){
   dashboard=new AppWidgetHostView(this);dashboard.setPadding(0,0,0,0);
   ComponentName provider=new ComponentName(this,CommandWidget.class);
   for(AppWidgetProviderInfo info:AppWidgetManager.getInstance(this).getInstalledProviders())if(provider.equals(info.provider)){dashboard.setAppWidget(0,info);break;}
   root.addView(dashboard,new FrameLayout.LayoutParams(-1,-1));
  }
  dashboard.updateAppWidget(CommandWidget.views(this,0,options));
  // The app's in-process host is not an allocated launcher widget. Use ordinary
  // Activity navigation for its overview and thermostat rows; launcher widgets
  // retain their collection PendingIntents. Reuse the host so redraws do not
  // detach a row between a user's touch-down and touch-up.
  String realm=WidgetState.decision(this,0).realm(),section=WidgetState.section(this,0);
  AppWidgetHostView appliedHost=dashboard;
  appliedHost.post(()->{
   if(!realm.equals(WidgetState.decision(this,0).realm())||!section.equals(WidgetState.section(this,0)))return;
   ListView list=appliedHost.findViewById(R.id.feed);if(list==null)return;
   if(section.equals("overview"))list.setOnItemClickListener((parent,view,index,rowId)->{
    java.util.List<String> panels=realm.equals("work")?java.util.List.of("beacons","calendar","jira","slack"):java.util.List.of("weather","calendar","hearth","mail");if(index<0||index>=panels.size())return;String target=panels.get(index);WidgetState.section(this,0,target);CommandWidget.update(this,0);if(ConnectionStatus.needs(this,realm,target))startActivity(ConnectionStatus.intent(this,realm,target));else Refresh.onOpen(this,0);
   });
   else if(realm.equals("home")&&section.equals("hearth"))list.setOnItemClickListener((parent,view,index,rowId)->{
    java.util.List<FeedStore.Row> rows=FeedStore.read(this,"home","hearth").rows();if(index<0||index>=rows.size())return;String key=rows.get(index).key();if(!key.startsWith("thermostat:"))return;startActivity(new android.content.Intent(this,EntryActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0).putExtra("realm","home").putExtra("section","hearth").putExtra("key",key));
   });
  });
  if(page.equals(lastPage)){final int savedPosition=position,savedOffset=offset;AppWidgetHostView applied=dashboard;applied.post(()->{AbsListView list=applied.findViewById(collectionId);if(list instanceof ListView)((ListView)list).setSelectionFromTop(savedPosition,savedOffset);else if(list!=null)list.setSelection(savedPosition);});}lastPage=page;
 }
}

package com.abdickers.realms;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

public final class CommandWidget extends AppWidgetProvider {
 static final String ACTION="com.abdickers.realms.WIDGET_ACTION";
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)update(c,id);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,Bundle options){update(c,id);}
 @Override public void onDeleted(Context c,int[] ids){for(int id:ids)WidgetState.remove(c,id);}
 @Override public void onReceive(Context c,Intent intent){
  super.onReceive(c,intent);
  if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())){updateAll(c);return;}
  if(!ACTION.equals(intent.getAction()))return;int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  if(!owns(c,id))return;dispatch(c,id,intent.getStringExtra("operation"));
 }
 static boolean owns(Context c,int id){AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);return info!=null&&new ComponentName(c,CommandWidget.class).equals(info.provider);}
 static void dispatch(Context c,int id,String op){
  if(op==null)return;
  if(Set.of("home","work","auto").contains(op))WidgetState.mode(c,id,op);
  else if(Set.of("overview","calendar","mail","jira","slack","systems","tools","ai").contains(op))WidgetState.section(c,id,op);
  else if(Set.of("previous","next","date").contains(op)){long current=WidgetState.day(c,id),today=LocalDate.now().toEpochDay(),next=op.equals("date")?today:current+(op.equals("next")?1:-1);next=Math.max(today-366,Math.min(today+366,next));WidgetState.prefs(c).edit().putLong(WidgetState.key(id,"day"),next).commit();}
  else if(op.equals("refresh")){Refresh.request(c,id);return;}
  update(c,id);
 }
 static void update(Context c,int id){AppWidgetManager m=AppWidgetManager.getInstance(c);m.updateAppWidget(id,views(c,id,m.getAppWidgetOptions(id)));}
 static void updateAll(Context c){for(int id:AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,CommandWidget.class)))update(c,id);}
 static RemoteViews views(Context c,int id,Bundle options){
  ModePolicy.Decision decision=WidgetState.decision(c,id);String realm=decision.realm(),section=WidgetState.section(c,id);boolean work=realm.equals("work");RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.command_widget);
  v.setImageViewResource(R.id.scene,work?R.drawable.gondor_scene:R.drawable.shire_scene);v.setImageViewResource(R.id.emblem,work?R.drawable.gondor_shield:R.drawable.realm_orb);v.setTextViewText(R.id.title,work?"Gondor Command":"The Shire");v.setTextViewText(R.id.reason,decision.reason()+" · "+(WidgetState.mode(c,id).equals("auto")?"Auto":"Manual"));
  int[] controls={R.id.home,R.id.work,R.id.auto,R.id.overview,R.id.calendar,R.id.jira,R.id.slack,R.id.systems,R.id.mail,R.id.tools,R.id.ai,R.id.previous,R.id.date,R.id.next,R.id.refresh};
  String[] operations={"home","work","auto","overview","calendar","jira","slack","systems","mail","tools","ai","previous","date","next","refresh"};
  for(int n=0;n<controls.length;n++){v.setOnClickPendingIntent(controls[n],action(c,id,operations[n]));boolean selected=operations[n].equals(WidgetState.mode(c,id))||operations[n].equals(section);v.setTextColor(controls[n],selected?0xFFFFD98B:0xFFE4EAF2);}
  Intent settings=new Intent(c,SettingsActivity.class).setData(android.net.Uri.parse("command://settings/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setOnClickPendingIntent(R.id.settings,PendingIntent.getActivity(c,id,settings,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  v.setViewVisibility(R.id.date_controls,section.equals("calendar")?View.VISIBLE:View.GONE);long day=WidgetState.day(c,id);v.setTextViewText(R.id.date,LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("MMM d")));
  List<FeedStore.Row> rows=new ArrayList<>();String status;
  if(section.equals("overview")){
   String[] sources=work?new String[]{"systems","calendar."+day,"jira","slack"}:new String[]{"weather","calendar."+day,"hearth","mail"};
   String[] labels=work?new String[]{"Beacons of Production","Today’s council","Active quests","The Fellowship"}:new String[]{"Weather at Sage Cabin","Today’s calendar","The hearth","The Ravens"};
   int[] icons={R.drawable.icon_beacon,R.drawable.icon_calendar,R.drawable.icon_quest,R.drawable.icon_notes};
   for(int n=0;n<sources.length;n++){FeedStore.Feed f=FeedStore.read(c,realm,sources[n]);String body=f.checkedAt()==0?"Connection needed · choose this panel, then Refresh.":f.rows().isEmpty()?"No matching items at last refresh.":f.rows().get(0).title()+"\n"+f.rows().get(0).body();if(!f.error().isEmpty())body="Unavailable · "+body;rows.add(new FeedStore.Row(labels[n],body,icons[n]));}
   status="Saved overview · select a panel to refresh its feed";
  }else if(section.equals("tools")){
   rows.add(new FeedStore.Row("Tools & Systems","Native service connections are managed in Settings. No website login or cookie is copied into this app.",R.drawable.icon_overview));status="Connections stay separate for Home and Work";
  }else if(section.equals("ai")){
   rows.add(new FeedStore.Row("The AI council","Native chat connection is not configured yet. No AI request has been sent.",R.drawable.icon_council));status="Connection needed";
  }else{
   String source=section.equals("calendar")?"calendar."+day:section;FeedStore.Feed f=FeedStore.read(c,realm,source);rows.addAll(f.rows());
   status=Refresh.running(c,realm,source)?"Refreshing selected feed…":!f.error().isEmpty()?"Unavailable · "+f.error():f.checkedAt()==0?"Not connected · open Settings to connect this source":"Checked "+Instant.ofEpochMilli(f.checkedAt()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))+" · saved on this phone";
   if(rows.isEmpty())rows.add(new FeedStore.Row(f.checkedAt()==0?"Connect your "+section:"No matching items",f.checkedAt()==0?"Once connected, this panel shows information directly in the widget. Refresh requests only this source.":"The last successful read returned no items.",section.equals("calendar")?R.drawable.icon_calendar:R.drawable.gondor_shield));
  }
  v.setTextViewText(R.id.status,status);RemoteViews.RemoteCollectionItems.Builder collection=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
  for(int n=0;n<Math.min(rows.size(),20);n++){FeedStore.Row row=rows.get(n);RemoteViews item=new RemoteViews(c.getPackageName(),R.layout.feed_row);item.setTextViewText(R.id.row_title,row.title());item.setTextViewText(R.id.row_body,row.body());item.setImageViewResource(R.id.row_icon,row.icon());collection.addItem(n,item);}
  v.setRemoteAdapter(R.id.feed,collection.build());
  if(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,390)<300){v.setTextViewTextSize(R.id.title,android.util.TypedValue.COMPLEX_UNIT_SP,17);}
  return v;
 }
 private static PendingIntent action(Context c,int id,String op){Intent i=new Intent(c,CommandWidget.class).setAction(ACTION).setData(android.net.Uri.parse("command://widget/"+id+"/"+op)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("operation",op);return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
}

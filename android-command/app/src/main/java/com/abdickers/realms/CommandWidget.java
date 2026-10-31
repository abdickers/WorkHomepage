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
 static final String TASK_ACTION="com.abdickers.realms.TASK_ACTION";
 static final String ACTION="com.abdickers.realms.WIDGET_ACTION";
 @Override public void onUpdate(Context c,AppWidgetManager manager,int[] ids){for(int id:ids)update(c,id);}
 @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager manager,int id,Bundle options){update(c,id);}
 @Override public void onDeleted(Context c,int[] ids){for(int id:ids)WidgetState.remove(c,id);}
 @Override public void onReceive(Context c,Intent intent){
  super.onReceive(c,intent);
  if(Intent.ACTION_TIME_CHANGED.equals(intent.getAction())||Intent.ACTION_TIMEZONE_CHANGED.equals(intent.getAction())){updateAll(c);return;}
  if(TASK_ACTION.equals(intent.getAction())){int widget=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);String realm=intent.getStringExtra("realm");if(owns(c,widget)&&WidgetState.decision(c,widget).realm().equals(realm)&&WidgetState.section(c,widget).equals("priorities")){LocalBoard.toggle(c,realm,intent.getStringExtra("key"));update(c,widget);}return;}
  if(!ACTION.equals(intent.getAction()))return;int id=intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);
  if(!owns(c,id))return;dispatch(c,id,intent.getStringExtra("operation"));
 }
 static boolean owns(Context c,int id){if(id==0)return true;AppWidgetProviderInfo info=AppWidgetManager.getInstance(c).getAppWidgetInfo(id);return info!=null&&new ComponentName(c,CommandWidget.class).equals(info.provider);}
 static void dispatch(Context c,int id,String op){
  if(op==null)return;
  if(Set.of("home","work","auto").contains(op)){WidgetState.mode(c,id,op);if(!Panels.forRealm(WidgetState.decision(c,id).realm()).contains(WidgetState.section(c,id)))WidgetState.section(c,id,"overview");}
  else if(op.equals("previous_panel")||op.equals("next_panel")){List<String> panels=Panels.forRealm(WidgetState.decision(c,id).realm());int at=panels.indexOf(WidgetState.section(c,id));WidgetState.section(c,id,panels.get(Math.floorMod(at+(op.equals("next_panel")?1:-1),panels.size())));}
  else if(Panels.ALL.contains(op))WidgetState.section(c,id,op);
  else if(op.equals("date"))WidgetState.prefs(c).edit().remove(WidgetState.key(id,"day")).commit();
  else if(Set.of("previous","next").contains(op)){long current=WidgetState.day(c,id),today=LocalDate.now().toEpochDay(),next=current+(op.equals("next")?1:-1);next=Math.max(today-366,Math.min(today+366,next));WidgetState.prefs(c).edit().putLong(WidgetState.key(id,"day"),next).commit();}
  else if(op.equals("refresh")){Refresh.request(c,id);return;}
  Refresh.onOpen(c,id);
  update(c,id);
 }
 static void update(Context c,int id){if(id==0){MainActivity.redraw();return;}AppWidgetManager m=AppWidgetManager.getInstance(c);m.updateAppWidget(id,views(c,id,m.getAppWidgetOptions(id)));}
 static void updateAll(Context c){for(int id:AppWidgetManager.getInstance(c).getAppWidgetIds(new ComponentName(c,CommandWidget.class)))update(c,id);MainActivity.redraw();}
 static RemoteViews views(Context c,int id,Bundle options){
  ModePolicy.Decision decision=WidgetState.decision(c,id);String realm=decision.realm(),section=WidgetState.section(c,id);boolean work=realm.equals("work");RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.command_widget);
  v.setImageViewResource(R.id.scene,work?R.drawable.gondor_scene:R.drawable.shire_scene);v.setImageViewResource(R.id.emblem,work?R.drawable.gondor_shield:R.drawable.realm_orb);v.setTextViewText(R.id.title,work?"Gondor Command":"The Shire");v.setTextViewText(R.id.reason,(WidgetState.mode(c,id).equals("auto")?"Auto":"Manual")+" · "+decision.reason());
  int[] frameViews={R.id.frame_tl,R.id.frame_tr,R.id.frame_bl,R.id.frame_br,R.id.frame_top,R.id.frame_bottom,R.id.frame_left,R.id.frame_right};
  int[] frameArt=work?new int[]{R.drawable.gondor_frame_tl,R.drawable.gondor_frame_tr,R.drawable.gondor_frame_bl,R.drawable.gondor_frame_br,R.drawable.gondor_frame_top,R.drawable.gondor_frame_bottom,R.drawable.gondor_frame_left,R.drawable.gondor_frame_right}:new int[]{R.drawable.elven_frame_tl,R.drawable.elven_frame_tr,R.drawable.elven_frame_bl,R.drawable.elven_frame_br,R.drawable.elven_frame_top,R.drawable.elven_frame_bottom,R.drawable.elven_frame_left,R.drawable.elven_frame_right};for(int n=0;n<frameViews.length;n++)v.setImageViewResource(frameViews[n],frameArt[n]);
  int[] controls={R.id.home,R.id.work,R.id.auto,R.id.overview,R.id.calendar,R.id.jira,R.id.slack,R.id.systems,R.id.mail,R.id.tools,R.id.ai,R.id.previous,R.id.date,R.id.next,R.id.refresh};
  String[] operations={"home","work","auto","overview","calendar",work?"jira":"hearth",work?"slack":"camera",work?"systems":"weather","mail","tools","ai","previous","date","next","refresh"};v.setTextViewText(R.id.jira,work?"Tasks":"Hearth");v.setTextViewText(R.id.slack,work?"Slack":"Gate");v.setTextViewText(R.id.systems,work?"Systems":"Weather");
  for(int n=0;n<controls.length;n++){v.setOnClickPendingIntent(controls[n],action(c,id,operations[n]));boolean selected=operations[n].equals(WidgetState.mode(c,id))||operations[n].equals(section);v.setTextColor(controls[n],selected?0xFFFFD98B:0xFFE4EAF2);}
  v.setOnClickPendingIntent(R.id.previous_panel,action(c,id,"previous_panel"));v.setOnClickPendingIntent(R.id.next_panel,action(c,id,"next_panel"));v.setTextViewText(R.id.current_panel,switch(section){case "jira"->"Active quests";case "systems"->"Systems";case "mail"->"The Ravens";case "ai"->"AI council";default->section.substring(0,1).toUpperCase()+section.substring(1);});
  boolean compact=options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,390)<300||options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,700)<450;
  v.setViewVisibility(R.id.compact_navigation,compact?View.VISIBLE:View.GONE);v.setViewVisibility(R.id.navigation_primary,compact?View.GONE:View.VISIBLE);v.setViewVisibility(R.id.navigation_secondary,compact?View.GONE:View.VISIBLE);
  v.setViewVisibility(R.id.reason,compact?View.GONE:View.VISIBLE);v.setViewVisibility(R.id.status,compact?View.GONE:View.VISIBLE);
  Intent more=new Intent(c,CaptureActivity.class).setData(android.net.Uri.parse("command://choose/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("choose",true);v.setTextViewText(R.id.ai,"More");v.setOnClickPendingIntent(R.id.ai,PendingIntent.getActivity(c,id,more,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  Intent settings=new Intent(c,SettingsActivity.class).setData(android.net.Uri.parse("command://settings/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setOnClickPendingIntent(R.id.settings,PendingIntent.getActivity(c,id,settings,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  v.setViewVisibility(R.id.date_controls,section.equals("calendar")?View.VISIBLE:View.GONE);long day=WidgetState.day(c,id);v.setTextViewText(R.id.date,LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("MMM d")));
  List<FeedStore.Row> rows=new ArrayList<>();String status;
  if(section.equals("overview")){
   String[] sources=work?new String[]{"beacons","calendar."+day,"jira","slack"}:new String[]{"weather","calendar."+day,"hearth","mail"};
   String[] labels=work?new String[]{"Beacons of Production","Today’s council","Active quests","The Fellowship"}:new String[]{"Weather at Sage Cabin","Today’s calendar","The hearth","The Ravens"};
   int[] icons=work?new int[]{R.drawable.icon_beacon,R.drawable.icon_calendar,R.drawable.icon_quest,R.drawable.icon_notes}:new int[]{R.drawable.elven_weather,R.drawable.elven_calendar,R.drawable.elven_home,R.drawable.elven_raven};
   for(int n=0;n<sources.length;n++){FeedStore.Feed f=FeedStore.read(c,realm,sources[n]);String body=f.checkedAt()==0?"Choose this panel to load its information.":f.rows().isEmpty()?"No matching items at last refresh.":f.rows().get(0).title()+"\n"+f.rows().get(0).body();if(!f.error().isEmpty())body="Unavailable · "+body;rows.add(new FeedStore.Row(labels[n],body,icons[n],"panel:"+sources[n].split("\\.")[0]));}
   status="Saved overview · select a panel to refresh its feed";
  }else if(Panels.local(section)){
   rows.addAll(section.equals("tools")?ToolLinks.rows(c,realm):LocalBoard.rows(c,realm,section));status=Panels.title(section)+" · saved on this phone";
   if(rows.isEmpty())rows.add(new FeedStore.Row("Your "+Panels.title(section),"Use Add below to capture your first item.",work?R.drawable.icon_notes:R.drawable.elven_raven,"add"));
   Intent add=new Intent(c,CaptureActivity.class).setData(android.net.Uri.parse("command://add/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setTextViewText(R.id.refresh,section.equals("tools")?"Add link":"Add");v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getActivity(c,id,add,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  }else if(section.equals("ai")){
   rows.add(new FeedStore.Row("Plan my day","Compose a question in your AI app",R.drawable.brand_openai,"plan"));rows.add(new FeedStore.Row("Investigate an integration","Review saved production context before sharing",R.drawable.brand_claude,"incident"));rows.add(new FeedStore.Row("Find the words","Draft a message with your AI companion",R.drawable.brand_gemini,"message"));status="The Council · choose an AI companion";
   Intent ask=new Intent(c,CouncilActivity.class).setData(android.net.Uri.parse("command://council/"+id)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id);v.setTextViewText(R.id.refresh,"Ask Council");v.setOnClickPendingIntent(R.id.refresh,PendingIntent.getActivity(c,id,ask,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE));
  }else{
   String source=section.equals("calendar")?"calendar."+day:section;FeedStore.Feed f=FeedStore.read(c,realm,source);for(int n=0;n<f.rows().size();n++){FeedStore.Row row=f.rows().get(n);rows.add(new FeedStore.Row(row.title(),row.body(),row.icon(),row.key().isEmpty()?Integer.toString(n):row.key()));}
   if(compact&&!f.error().isEmpty())rows.add(0,new FeedStore.Row("Refresh unavailable",f.error(),work?R.drawable.gondor_shield:R.drawable.elven_orb,"error"));
   v.setTextViewText(R.id.refresh,Refresh.running(c,realm,source)?"Loading…":"Refresh");
   status=Refresh.running(c,realm,source)?"Refreshing selected feed…":!f.error().isEmpty()?"Unavailable · "+f.error():f.checkedAt()==0?(ConnectionStatus.needs(c,realm,section)?"Tap Connect to set up this panel":"Tap Refresh to load this panel"):"Checked "+Instant.ofEpochMilli(f.checkedAt()).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, h:mm a"))+" · saved on this phone";
   if(rows.isEmpty())rows.add(new FeedStore.Row(f.checkedAt()==0?(ConnectionStatus.needs(c,realm,section)?"Connect your ":"Load ")+Panels.title(section):"No matching items",f.checkedAt()==0?"Once connected, this panel shows information directly in the widget. Refresh requests only this source.":"The last successful read returned no items.",section.equals("calendar")?R.drawable.icon_calendar:R.drawable.gondor_shield,f.checkedAt()==0?(ConnectionStatus.needs(c,realm,section)?"connect":"refresh"):"empty"));
  }
  if(!section.equals("overview")&&!Panels.local(section)&&!section.equals("ai")&&ConnectionStatus.needs(c,realm,section)){v.setTextViewText(R.id.refresh,"Connect");v.setOnClickPendingIntent(R.id.refresh,ConnectionStatus.action(c,id,section));}
  v.setTextViewText(R.id.status,status);
  boolean tools=section.equals("tools");int collectionView=tools?R.id.tool_grid:R.id.feed;v.setViewVisibility(R.id.feed,tools?View.GONE:View.VISIBLE);v.setViewVisibility(R.id.tool_grid,tools?View.VISIBLE:View.GONE);
  boolean togglePriority=section.equals("priorities")&&!LocalBoard.all(c,realm,"priorities").isEmpty();
  Intent template=new Intent(c,togglePriority?CommandWidget.class:EntryActivity.class).setAction(togglePriority?TASK_ACTION:"com.abdickers.realms.ENTRY").setData(android.net.Uri.parse("command://entries/"+id+"/"+realm+"/"+section)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("realm",realm).putExtra("section",section);
  PendingIntent entry=togglePriority?PendingIntent.getBroadcast(c,id,template,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE):PendingIntent.getActivity(c,id,template,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_MUTABLE);v.setPendingIntentTemplate(collectionView,entry);
  RemoteViews.RemoteCollectionItems.Builder collection=new RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1);
  for(int n=0;n<Math.min(rows.size(),200);n++){FeedStore.Row row=rows.get(n);RemoteViews item=new RemoteViews(c.getPackageName(),tools?R.layout.tool_tile:R.layout.feed_row);item.setTextViewText(R.id.row_title,row.title().substring(0,Math.min(200,row.title().length())));if(!tools)item.setTextViewText(R.id.row_body,row.body().substring(0,Math.min(1200,row.body().length())));item.setImageViewResource(R.id.row_icon,row.icon());item.setOnClickFillInIntent(R.id.row,new Intent().putExtra("key",row.key().isEmpty()?Integer.toString(n):row.key()));collection.addItem(n,item);}
  v.setRemoteAdapter(collectionView,collection.build());
  if(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,390)<300){v.setTextViewTextSize(R.id.title,android.util.TypedValue.COMPLEX_UNIT_SP,17);v.setTextViewText(R.id.title,work?"Gondor":"The Shire");}
  return v;
 }
 private static PendingIntent action(Context c,int id,String op){Intent i=new Intent(c,CommandWidget.class).setAction(ACTION).setData(android.net.Uri.parse("command://widget/"+id+"/"+op)).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,id).putExtra("operation",op);return PendingIntent.getBroadcast(c,0,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
}

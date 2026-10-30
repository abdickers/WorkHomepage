package com.abdickers.realms;

import android.content.Context;
import androidx.work.*;

final class Refresh {
 static void request(Context context,int widgetId){
  Context c=context.getApplicationContext();String realm=WidgetState.decision(c,widgetId).realm(),source=WidgetState.section(c,widgetId);long day=WidgetState.day(c,widgetId);
  if(source.equals("overview")){source=realm.equals("work")?"systems":"weather";}
  if(Panels.local(source)||source.equals("ai")){CommandWidget.updateAll(c);return;}
  final String selected=source,cacheSource=source.equals("calendar")?"calendar."+day:source,key=FeedStore.key(realm,cacheSource);
  long now=System.currentTimeMillis(),blocked=WidgetState.prefs(c).getLong("retry."+key,0);
  if(blocked>now){CommandWidget.updateAll(c);return;}
  WidgetState.prefs(c).edit().putLong("retry."+key,now+30000).putLong("pending."+key,now).commit();
  Data input=new Data.Builder().putString("realm",realm).putString("source",selected).putLong("day",day).putLong("generation",Connections.generation(c)).build();
  WorkManager.getInstance(c).enqueueUniqueWork("feed."+key,ExistingWorkPolicy.KEEP,new OneTimeWorkRequest.Builder(FeedWorker.class).addTag("command-feeds").setInputData(input).build());CommandWidget.updateAll(c);
 }
 static boolean running(Context c,String realm,String source){long at=WidgetState.prefs(c).getLong("pending."+FeedStore.key(realm,source),0);return at>0&&System.currentTimeMillis()-at<120000;}
}

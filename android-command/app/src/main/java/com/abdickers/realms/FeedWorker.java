package com.abdickers.realms;

import android.content.Context;
import androidx.work.*;
import java.util.Set;

/** One explicit refresh, durable across receiver/process teardown. Never retries a provider automatically. */
public final class FeedWorker extends Worker {
 public FeedWorker(Context c,WorkerParameters p){super(c,p);}
 @Override public Result doWork(){
  Context c=getApplicationContext();String realm=getInputData().getString("realm"),source=getInputData().getString("source");long day=getInputData().getLong("day",0);
  if(!Set.of("home","work").contains(realm==null?"":realm)||!Set.of("calendar","systems","jira","slack","weather","hearth","mail").contains(source==null?"":source))return Result.failure();
  String cacheSource=source.equals("calendar")?"calendar."+day:source,key=FeedStore.key(realm,cacheSource);
  long generation=getInputData().getLong("generation",-1);if(generation!=Connections.generation(c))return Result.success();
  try{java.util.List<FeedStore.Row> rows=source.equals("calendar")?Feeds.calendar(c,realm,day):Feeds.read(c,realm,source);if(!isStopped())Connections.commitIfCurrent(c,generation,()->FeedStore.success(c,realm,cacheSource,rows));}
  catch(BoundedHttp.RateLimitException e){synchronized(Connections.LOCK){if(generation==Connections.generation(c)&&!isStopped()){WidgetState.prefs(c).edit().putLong("retry."+key,System.currentTimeMillis()+e.delay).commit();FeedStore.failure(c,realm,cacheSource,e.getMessage());}}}
  catch(Exception e){String message=e instanceof IllegalStateException||e instanceof BoundedHttp.ProviderException?e.getMessage():e instanceof org.json.JSONException?"Provider returned an unfamiliar format. Saved data retained.":"Could not read this source. Reconnect in Settings or try again later.";synchronized(Connections.LOCK){if(generation==Connections.generation(c)&&!isStopped())FeedStore.failure(c,realm,cacheSource,message==null?"Connection unavailable. Saved data retained.":message);}}
  finally{Connections.commitIfCurrent(c,generation,()->WidgetState.prefs(c).edit().remove("pending."+key).commit());CommandWidget.updateAll(c);}
  return Result.success();
 }
}

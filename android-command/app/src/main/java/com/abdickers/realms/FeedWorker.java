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
  try{FeedStore.success(c,realm,cacheSource,source.equals("calendar")?Feeds.calendar(c,realm,day):Feeds.read(c,realm,source));}
  catch(BoundedHttp.RateLimitException e){WidgetState.prefs(c).edit().putLong("retry."+key,System.currentTimeMillis()+e.delay).commit();FeedStore.failure(c,realm,cacheSource,e.getMessage());}
  catch(Exception e){String message=e instanceof java.io.IOException?"Could not reach the provider. Saved data retained. Try again later.":e.getMessage();FeedStore.failure(c,realm,cacheSource,message==null?"Connection unavailable. Saved data retained.":message);}
  finally{WidgetState.prefs(c).edit().remove("pending."+key).commit();CommandWidget.updateAll(c);}
  return Result.success();
 }
}

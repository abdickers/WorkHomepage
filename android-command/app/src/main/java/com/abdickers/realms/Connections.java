package com.abdickers.realms;

import android.content.Context;
import androidx.work.WorkManager;

/** Prevent a disconnected or replaced account's in-flight reads from repopulating its cache. */
final class Connections {
 static final Object LOCK=new Object();
 static long generation(Context c){return WidgetState.prefs(c).getLong("connection_generation",0);}
 static boolean commitIfCurrent(Context c,long expected,Runnable commit){synchronized(LOCK){if(expected!=generation(c))return false;commit.run();return true;}}
 static void invalidate(Context c){invalidate(c,null);}
 static void invalidate(Context c,String realm){synchronized(LOCK){WidgetState.prefs(c).edit().putLong("connection_generation",generation(c)+1).commit();WorkManager.getInstance(c).cancelAllWorkByTag("command-feeds");if(realm==null)FeedStore.clear(c);else FeedStore.clearRealm(c,realm);android.content.SharedPreferences.Editor edit=WidgetState.prefs(c).edit();for(String key:WidgetState.prefs(c).getAll().keySet())if(key.startsWith("pending.")||key.startsWith("retry."))edit.remove(key);edit.commit();}}
 private Connections(){}
}

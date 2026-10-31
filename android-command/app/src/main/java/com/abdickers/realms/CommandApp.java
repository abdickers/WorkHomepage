package com.abdickers.realms;

import android.app.*;
import android.net.*;
import android.os.Bundle;

/** Best-effort network reevaluation while this process lives; no location service or API polling. */
public final class CommandApp extends Application implements Application.ActivityLifecycleCallbacks {
 private static volatile int resumed;
 static boolean foreground(){return resumed>0;}
 @Override public void onCreate(){super.onCreate();registerActivityLifecycleCallbacks(this);getSystemService(ConnectivityManager.class).registerDefaultNetworkCallback(new ConnectivityManager.NetworkCallback(){@Override public void onCapabilitiesChanged(Network n,NetworkCapabilities capabilities){CommandWidget.updateAll(CommandApp.this);}@Override public void onLost(Network n){CommandWidget.updateAll(CommandApp.this);}});}
 @Override public void onActivityResumed(Activity a){resumed++;CommandWidget.updateAll(this);}
 @Override public void onActivityPaused(Activity a){resumed=Math.max(0,resumed-1);}
 @Override public void onActivityCreated(Activity a,Bundle b){}
 @Override public void onActivityStarted(Activity a){}
 @Override public void onActivityStopped(Activity a){}
 @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
 @Override public void onActivityDestroyed(Activity a){}
}

package com.abdickers.realms;
import android.content.*;
import android.content.pm.PackageManager;
import android.app.PendingIntent;
import java.util.Set;
final class ConnectionStatus {
 static boolean needs(Context c,String realm,String source){
  if(source.equals("mail"))return !WidgetState.prefs(c).getBoolean(realm+"_google_connected",false);
  if(source.equals("calendar"))return !WidgetState.prefs(c).getBoolean(realm+"_google_calendar_connected",false)&&(c.checkSelfPermission(android.Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED||WidgetState.prefs(c).getString(realm+"_calendar","").isBlank());
  if(Set.of("overview","watch","tools","notes","priorities","ai").contains(source))return false;
  if(realm.equals("work")&&source.equals("slack")&&SlackConnection.connected(c))return false;
  if(realm.equals("work")&&source.equals("jira")&&JiraConnection.connected(c))return false;
  if(!realm.equals("work"))return true;String key=switch(source){case "systems"->"vercel";case "beacons"->"celigo";default->source;};return !c.getSharedPreferences("vault",0).contains(key);
 }
 static Intent intent(Context c,String realm,String source){return new Intent(c,AccountActivity.class).putExtra("realm",realm).putExtra("source",source);}
 static PendingIntent action(Context c,int widget,String source){Intent intent=intent(c,WidgetState.decision(c,widget).realm(),source).setData(android.net.Uri.parse("command://accounts/"+widget+"/"+source));return PendingIntent.getActivity(c,widget,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 private ConnectionStatus(){}
}

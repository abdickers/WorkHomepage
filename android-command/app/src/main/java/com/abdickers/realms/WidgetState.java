package com.abdickers.realms;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.*;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import java.time.*;
import java.util.*;

final class WidgetState {
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("command",Context.MODE_PRIVATE);}
 static String key(int id,String name){return "widget."+id+"."+name;}
 static String mode(Context c,int id){return prefs(c).getString(key(id,"mode"),"auto");}
 static String section(Context c,int id){return prefs(c).getString(key(id,"section"),"overview");}
 static long day(Context c,int id){return prefs(c).getLong(key(id,"day"),LocalDate.now().toEpochDay());}
 static void mode(Context c,int id,String mode){if(Set.of("home","work","auto").contains(mode))prefs(c).edit().putString(key(id,"mode"),mode).commit();}
 static void section(Context c,int id,String section){if(Panels.ALL.contains(section))prefs(c).edit().putString(key(id,"section"),section).commit();}
 static ModePolicy.Decision decision(Context c,int id){
  SharedPreferences p=prefs(c);Set<DayOfWeek> days=new HashSet<>();for(String d:p.getString("days","1,2,3,4,5").split(","))try{days.add(DayOfWeek.of(Integer.parseInt(d)));}catch(Exception ignored){}
  return ModePolicy.choose(mode(c,id),ssid(c),ZonedDateTime.now(),new ModePolicy.Rules(ModePolicy.networks(p.getString("home_wifi","")),ModePolicy.networks(p.getString("work_wifi","")),days,p.getInt("start",540),p.getInt("end",1020),p.getString("fallback","home")));
 }
 static String ssid(Context c){
  if(!prefs(c).getBoolean("wifi_enabled",false))return "";
  // Background identity is intentionally unavailable without the user's separate grant.
  if(!CommandApp.foreground()&&c.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)!=PackageManager.PERMISSION_GRANTED)return "";
  return currentWifi(c);
 }
 /** Only call directly after the user's Use-current-Wi-Fi action in the foreground. */
 static String currentWifi(Context c){
  if(c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED||!((LocationManager)c.getSystemService(Context.LOCATION_SERVICE)).isLocationEnabled())return "";
  try{WifiInfo wifi=c.getSystemService(WifiManager.class).getConnectionInfo();String detected=wifi==null?"":ModePolicy.normalizeSsid(wifi.getSSID());if(!detected.isEmpty())return detected;ConnectivityManager cm=c.getSystemService(ConnectivityManager.class);Network n=cm.getActiveNetwork();NetworkCapabilities cap=cm.getNetworkCapabilities(n);if(cap!=null&&cap.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)&&cap.getTransportInfo() instanceof WifiInfo)return ModePolicy.normalizeSsid(((WifiInfo)cap.getTransportInfo()).getSSID());}catch(SecurityException ignored){}
  return "";
 }
 static void remove(Context c,int id){SharedPreferences.Editor e=prefs(c).edit();for(String k:prefs(c).getAll().keySet())if(k.startsWith("widget."+id+"."))e.remove(k);e.commit();}
 private WidgetState(){}
}

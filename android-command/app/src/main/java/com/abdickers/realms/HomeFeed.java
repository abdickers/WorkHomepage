package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.util.*;
import java.time.*;
import java.time.format.DateTimeFormatter;

final class HomeFeed {
 static List<FeedStore.Row> read(Context c,String source)throws Exception{return source.equals("weather")?weather(HomeConnection.call(c,"/weather/current",null)):devices(HomeConnection.call(c,"/devices",null),source);}
 static String temperature(Object value){return value instanceof Number n?Math.round(n.doubleValue()*9/5+32)+"°F":"Unavailable";}
 static String value(JSONObject data,String key,String unit){Object value=data.opt(key);return value instanceof Number?value+unit:"Unavailable";}
 static List<FeedStore.Row> weather(JSONObject data)throws Exception{
  if(!data.optBoolean("available"))throw new IllegalStateException(data.optString("reason").equals("STALE")?"Your weather station has not reported a fresh reading. Saved weather is unchanged.":"Your weather station is not currently returning observations. Saved weather is unchanged.");
  JSONObject sample=data.getJSONObject("observation");String time=Instant.parse(sample.getString("time")).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("h:mm a"));return List.of(new FeedStore.Row(data.optString("stationName","Sage Cabin")+" · "+value(sample,"tempF","°F"),"Humidity "+value(sample,"humidity","%")+" · wind "+value(sample,"windMph"," mph")+"\nRain today "+value(sample,"rainTodayIn"," in")+" · observed "+time,R.drawable.elven_weather));
 }
 static List<FeedStore.Row> devices(JSONObject data,String source)throws Exception{
  JSONArray devices=data.getJSONArray("devices");List<FeedStore.Row> rows=new ArrayList<>();for(int n=0;n<Math.min(devices.length(),25);n++){JSONObject device=devices.getJSONObject(n);String type=device.optString("type"),id=device.optString("id");if(!id.matches("[A-Za-z0-9_-]{1,256}"))continue;
   if(source.equals("hearth")&&type.equals("THERMOSTAT")){JSONObject setpoint=device.optJSONObject("setpoint");String target=setpoint==null?"":"\nHeat "+temperature(setpoint.opt("heatCelsius"))+" · cool "+temperature(setpoint.opt("coolCelsius"));rows.add(new FeedStore.Row(device.optString("name","The hearth")+" · "+temperature(device.opt("temperature")),device.optString("mode","Unknown mode")+" · "+device.optString("hvac","Unknown state")+" · "+device.optString("online","Unknown connection")+"\nHumidity "+value(device,"humidity","%")+target+"\nTap to adjust thermostat",R.drawable.elven_home,"thermostat:"+id));}
   else if(source.equals("camera")&&Set.of("CAMERA","DOORBELL","DISPLAY").contains(type))rows.add(new FeedStore.Row(device.optString("name","Nest camera"),device.optString("online","Unknown connection")+"\nCamera discovered. Native live video is still being connected.",R.drawable.elven_camera));
  }return rows;
 }
 private HomeFeed(){}
}

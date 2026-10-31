package com.abdickers.realms;
import android.content.Context;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Primary calendar for the explicitly signed-in account; one selected day and one page. */
final class GoogleCalendarFeed {
 interface Reader{JSONObject get(String url,String authorization)throws Exception;}
 static List<FeedStore.Row> read(Context c,String realm,long day)throws Exception{return read(day,ZoneId.systemDefault(),GoogleConnection.token(c,realm,GoogleConnection.CALENDAR_READ),BoundedHttp::get);}
 static List<FeedStore.Row> read(long day,ZoneId zone,String token,Reader reader)throws Exception{
  LocalDate date=LocalDate.ofEpochDay(day);String start=date.atStartOfDay(zone).toInstant().toString(),end=date.plusDays(1).atStartOfDay(zone).toInstant().toString();
  String url="https://www.googleapis.com/calendar/v3/calendars/primary/events?singleEvents=true&orderBy=startTime&maxResults=20&timeMin="+URLEncoder.encode(start,"UTF-8")+"&timeMax="+URLEncoder.encode(end,"UTF-8")+"&fields=items(id,summary,status,start,end,location),nextPageToken";
  JSONObject response=reader.get(url,"Bearer "+token);JSONArray items=response.optJSONArray("items");List<FeedStore.Row> rows=new ArrayList<>();if(items==null)return rows;
  for(int i=0;i<Math.min(20,items.length());i++){JSONObject item=items.getJSONObject(i);if(item.optString("status").equals("cancelled"))continue;JSONObject at=item.getJSONObject("start");String when=at.has("date")?"All day":OffsetDateTime.parse(at.getString("dateTime")).atZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("h:mm a"));String location=item.optString("location","");rows.add(new FeedStore.Row(item.optString("summary","Untitled event"),when+(location.isBlank()?"":" · "+location),R.drawable.brand_calendar));}
  return rows;
 }
 private GoogleCalendarFeed(){}
}

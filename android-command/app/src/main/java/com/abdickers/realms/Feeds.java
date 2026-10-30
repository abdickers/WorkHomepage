package com.abdickers.realms;

import android.Manifest;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import org.json.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Source-owned endpoints and bounded reads. No dashboard-cookie extraction or generic proxy. */
final class Feeds {
 static List<FeedStore.Row> read(Context c,String realm,String source)throws Exception{
  if(source.equals("mail"))return MailFeed.read(c,realm);
  if(source.equals("watch"))return WatchFeed.read();
  if(!"work".equals(realm))throw new IllegalStateException("Personal service connection is not configured yet. Calendar can use this phone’s synced account.");
  return switch(source){case "systems"->vercel(c);case "beacons"->CeligoFeed.read(c);case "jira"->jira(c);case "slack"->slack(c);default->throw new IllegalStateException("This native connection is not configured yet. No request was sent.");};
 }
 static List<FeedStore.Row> calendar(Context c,String realm,long epochDay)throws Exception{
  if(c.checkSelfPermission(Manifest.permission.READ_CALENDAR)!=PackageManager.PERMISSION_GRANTED)throw new IllegalStateException("Allow calendar access in Settings to show this phone’s synced events.");
  String account=WidgetState.prefs(c).getString(realm+"_calendar","").trim();if(account.isEmpty())throw new IllegalStateException("Choose a calendar account in Settings so Home and Work events stay separate.");
  LocalDate date=LocalDate.ofEpochDay(epochDay);ZoneId zone=ZoneId.systemDefault();long start=date.atStartOfDay(zone).toInstant().toEpochMilli(),end=date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli();
  Uri.Builder uri=CalendarContract.Instances.CONTENT_URI.buildUpon();ContentUris.appendId(uri,start);ContentUris.appendId(uri,end);
  String[] cols={CalendarContract.Instances.TITLE,CalendarContract.Instances.BEGIN,CalendarContract.Instances.END,CalendarContract.Instances.ALL_DAY,CalendarContract.Instances.EVENT_LOCATION};
  List<FeedStore.Row> result=new ArrayList<>();
  try(Cursor cursor=c.getContentResolver().query(uri.build(),cols,CalendarContract.Calendars.ACCOUNT_NAME+" = ? AND "+CalendarContract.Instances.VISIBLE+" = 1",new String[]{account},CalendarContract.Instances.BEGIN+" ASC")){
   if(cursor==null)throw new IllegalStateException("Calendar provider is unavailable.");
   while(cursor.moveToNext()&&result.size()<20){String title=cursor.isNull(0)?"Untitled event":cursor.getString(0),when=cursor.getInt(3)==1?"All day":Instant.ofEpochMilli(cursor.getLong(1)).atZone(zone).format(DateTimeFormatter.ofPattern("h:mm a"));String place=cursor.isNull(4)?"":cursor.getString(4);result.add(new FeedStore.Row(title,when+(place.isEmpty()?"":" · "+place),R.drawable.icon_calendar));}
  }
  return result;
 }
 private static String credential(Context c,String source)throws Exception{String token=Vault.get(c,source);if(token.isBlank())throw new IllegalStateException("Connect "+source+" in Settings to read its live feed.");return token;}
 private static List<FeedStore.Row> vercel(Context c)throws Exception{
  String auth="Bearer "+credential(c,"vercel");String team=WidgetState.prefs(c).getString("vercel_team","").trim();if(!team.matches("team_[A-Za-z0-9]+"))throw new IllegalStateException("Set the Vercel team ID in Settings. The team slug alone is not an API team ID.");
  JSONObject project=BoundedHttp.get("https://api.vercel.com/v9/projects/s76h_catalyst?teamId="+team,auth);String id=project.getString("id");if(!id.matches("prj_[A-Za-z0-9]+"))throw new IllegalStateException("Vercel returned an unexpected project identifier.");
  JSONObject response=BoundedHttp.get("https://api.vercel.com/v6/deployments?projectId="+id+"&teamId="+team+"&limit=5",auth);JSONArray deployments=response.getJSONArray("deployments");List<FeedStore.Row> rows=new ArrayList<>();
  for(int i=0;i<Math.min(5,deployments.length());i++){JSONObject d=deployments.getJSONObject(i);JSONObject meta=d.optJSONObject("meta");String commit=meta==null?"":meta.optString("githubCommitMessage");rows.add(new FeedStore.Row("Catalyst · "+d.optString("state","Unknown"),d.optString("target","preview")+" · "+d.optString("url")+(commit.isEmpty()?"":"\n"+commit),R.drawable.icon_beacon));}return rows;
 }
 private static List<FeedStore.Row> jira(Context c)throws Exception{
  String email=WidgetState.prefs(c).getString("jira_email","").trim();if(!email.contains("@"))throw new IllegalStateException("Set the Jira account email in Settings.");String auth="Basic "+android.util.Base64.encodeToString((email+":"+credential(c,"jira")).getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
  String query=java.net.URLEncoder.encode("assignee = currentUser() AND statusCategory != Done ORDER BY priority DESC, updated DESC","UTF-8");
  JSONObject data=BoundedHttp.get("https://76fireworks.atlassian.net/rest/api/3/search/jql?maxResults=20&fields=summary,status,priority&jql="+query,auth);JSONArray issues=data.getJSONArray("issues");List<FeedStore.Row> rows=new ArrayList<>();for(int i=0;i<Math.min(20,issues.length());i++){JSONObject issue=issues.getJSONObject(i),fields=issue.getJSONObject("fields");rows.add(new FeedStore.Row(fields.optString("summary"),issue.optString("key")+" · "+fields.getJSONObject("status").optString("name")+" · "+(fields.optJSONObject("priority")==null?"Unspecified priority":fields.optJSONObject("priority").optString("name","Unspecified priority")),R.drawable.icon_quest));}return rows;
 }
 private static List<FeedStore.Row> slack(Context c)throws Exception{
  String query=java.net.URLEncoder.encode("is:dm -from:me after:"+LocalDate.now().minusDays(7),"UTF-8");
  JSONObject data=BoundedHttp.get("https://slack.com/api/search.messages?query="+query+"&count=20&sort=timestamp&sort_dir=desc&highlight=false","Bearer "+credential(c,"slack"));if(!data.optBoolean("ok"))throw new IllegalStateException("Slack connection needs search access. Update it in Settings.");JSONArray matches=data.getJSONObject("messages").getJSONArray("matches");List<FeedStore.Row> rows=new ArrayList<>();for(int i=0;i<Math.min(20,matches.length());i++){JSONObject m=matches.getJSONObject(i);rows.add(new FeedStore.Row("Direct message · "+m.optString("user"),m.optString("text"),R.drawable.icon_notes));}return rows;
 }
}

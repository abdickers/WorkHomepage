package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.time.LocalDate;
import java.util.*;
import java.net.URLEncoder;

/** One message page; names only for its senders, cached privately for a day. */
final class SlackFeed {
 interface Reader{JSONObject get(String url,String authorization)throws Exception;}
 static List<FeedStore.Row> read(Context c,String token)throws Exception{
  long generation=Connections.generation(c);String scope="legacy:"+generation;
  if(SlackConnection.connected(c)){JSONObject session=new JSONObject(Vault.get(c,SlackConnection.KEY));scope=session.getString("team")+":"+session.getString("user");}
  JSONObject cache;try{cache=new JSONObject(Vault.get(c,"slack_sender_names"));}catch(Exception e){cache=new JSONObject();}
  if(!scope.equals(cache.optString("owner")))cache=new JSONObject().put("owner",scope);
  List<FeedStore.Row> rows=read(LocalDate.now(),System.currentTimeMillis(),token,cache,BoundedHttp::get);
  synchronized(Connections.LOCK){if(generation==Connections.generation(c))Vault.put(c,"slack_sender_names",cache.toString());}
  return rows;
 }
 static List<FeedStore.Row> read(LocalDate today,long now,String token,JSONObject cache,Reader reader)throws Exception{
  String auth="Bearer "+token,query=URLEncoder.encode("is:dm -from:me after:"+today.minusDays(7),"UTF-8");
  JSONObject data=reader.get("https://slack.com/api/search.messages?query="+query+"&count=20&sort=timestamp&sort_dir=desc&highlight=false",auth);
  if(!data.optBoolean("ok"))throw new IllegalStateException("Slack could not read your messages. Review its connection in Accounts.");
  JSONObject names=cache.optJSONObject("names");if(names==null){names=new JSONObject();cache.put("names",names);}List<String> expired=new ArrayList<>();for(Iterator<String> keys=names.keys();keys.hasNext();){String key=keys.next();if(names.getJSONObject(key).optLong("expires")<=now)expired.add(key);}for(String key:expired)names.remove(key);
  JSONArray matches=data.getJSONObject("messages").getJSONArray("matches");Map<String,String> resolved=new HashMap<>();List<FeedStore.Row> rows=new ArrayList<>();boolean limited=cache.optLong("retryAfter")>now;
  for(int n=0;n<Math.min(20,matches.length());n++){
   JSONObject message=matches.getJSONObject(n);String user=message.optString("user"),name=resolved.get(user);
   if(name==null){JSONObject saved=names.optJSONObject(user);name=saved==null?"":saved.optString("name");
    if(name.isBlank()&&!limited&&user.matches("[UW][A-Z0-9]{3,40}"))try{JSONObject result=reader.get("https://slack.com/api/users.info?user="+user,auth),person=result.optJSONObject("user");if(result.optBoolean("ok")&&person!=null&&user.equals(person.optString("id"))){name=displayName(person);if(!name.isBlank())names.put(user,new JSONObject().put("name",name).put("expires",now+86400000));}}catch(BoundedHttp.RateLimitException e){limited=true;cache.put("retryAfter",now+e.delay);}catch(Exception ignored){/* A profile failure must not hide an already retrieved message. */}
    if(name.isBlank())name="Slack member";resolved.put(user,name);
   }
   rows.add(new FeedStore.Row("Direct message · "+name,message.optString("text"),R.drawable.brand_slack));
  }
  while(names.length()>128)names.remove(names.keys().next());return rows;
 }
 static String displayName(JSONObject person){JSONObject profile=person.optJSONObject("profile");for(String name:new String[]{profile==null?"":profile.optString("display_name"),profile==null?"":profile.optString("real_name"),person.optString("real_name"),person.optString("name")})if(!name.isBlank())return name.strip().replaceAll("[\\p{Cntrl}]","").substring(0,Math.min(100,name.strip().replaceAll("[\\p{Cntrl}]","").length()));return "";}
 private SlackFeed(){}
}

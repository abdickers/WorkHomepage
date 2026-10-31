package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.util.*;

final class MailFeed {
 interface Reader {JSONObject get(String url,String authorization)throws Exception;}
 static List<FeedStore.Row> read(Context c,String realm)throws Exception{return read(GoogleConnection.email(c,realm),GoogleConnection.token(c,realm),BoundedHttp::get);}
 static List<FeedStore.Row> read(String expectedEmail,String token,Reader reader)throws Exception {
  String base="https://gmail.googleapis.com/gmail/v1/users/me",auth="Bearer "+token;
  JSONObject profile=reader.get(base+"/profile?fields=emailAddress",auth);
  if(!expectedEmail.equalsIgnoreCase(profile.optString("emailAddress")))throw new IllegalStateException("Google returned another account. Reconnect the selected account in Settings.");
  // One small page; metadata only, no message bodies or attachments, no pagination loop.
  JSONObject list=reader.get(base+"/messages?maxResults=8&q=in%3Ainbox%20is%3Aunread%20newer_than%3A7d&fields=messages(id),nextPageToken",auth);
  JSONArray messages=list.optJSONArray("messages");List<FeedStore.Row> rows=new ArrayList<>();
  if(messages==null)return rows;
  for(int i=0;i<Math.min(messages.length(),8);i++){
   String id=messages.getJSONObject(i).getString("id");if(!id.matches("[A-Za-z0-9_-]{1,100}"))throw new IllegalStateException("Google returned an unfamiliar message identifier.");
   JSONObject message=reader.get(base+"/messages/"+id+"?format=metadata&metadataHeaders=Subject&metadataHeaders=From&metadataHeaders=Date&fields=id,snippet,payload(headers)",auth);
   JSONArray headers=message.optJSONObject("payload")==null?null:message.getJSONObject("payload").optJSONArray("headers");String subject="(No subject)",from="",date="";
   if(headers!=null)for(int j=0;j<Math.min(headers.length(),20);j++){JSONObject h=headers.getJSONObject(j);switch(h.optString("name").toLowerCase(Locale.ROOT)){case "subject"->subject=h.optString("value");case "from"->from=h.optString("value");case "date"->date=h.optString("value");}}
   rows.add(new FeedStore.Row(subject,from+"\n"+date+"\n"+decodeSnippet(message.optString("snippet")),R.drawable.brand_gmail));
  }
  if(list.has("nextPageToken"))rows.add(new FeedStore.Row("More unread mail","Showing the latest eight matching messages from the last seven days.",R.drawable.brand_gmail));
  return rows;
 }
 private static String decodeSnippet(String s){return android.text.Html.fromHtml(s,android.text.Html.FROM_HTML_MODE_LEGACY).toString();}
 private MailFeed(){}
}

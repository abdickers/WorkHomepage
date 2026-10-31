package com.abdickers.realms;
import org.json.*;
import org.junit.Test;
import java.time.LocalDate;
import java.util.*;
import static org.junit.Assert.*;

public class SlackFeedTest {
 JSONObject messages()throws Exception{return new JSONObject().put("ok",true).put("messages",new JSONObject().put("matches",new JSONArray().put(new JSONObject().put("user","UTEST1").put("text","SYNTHETIC message 1")).put(new JSONObject().put("user","UTEST1").put("text","SYNTHETIC message 2"))));}
 @Test public void namesUseOneLookupPerSenderAndAreReusedWithoutDirectoryReads()throws Exception{
  JSONObject cache=new JSONObject();List<String> calls=new ArrayList<>();SlackFeed.Reader reader=(url,auth)->{calls.add(url);assertEquals("Bearer SYNTHETIC",auth);if(url.contains("search.messages")){assertTrue(url.contains("count=20"));assertTrue(url.contains("2026-10-24"));return messages();}assertEquals("https://slack.com/api/users.info?user=UTEST1",url);return new JSONObject().put("ok",true).put("user",new JSONObject().put("id","UTEST1").put("profile",new JSONObject().put("display_name","Alex Example").put("real_name","Alex Realname")));};
  List<FeedStore.Row> rows=SlackFeed.read(LocalDate.of(2026,10,31),1000,"SYNTHETIC",cache,reader);assertEquals(2,calls.size());assertEquals("Direct message · Alex Example",rows.get(0).title());assertEquals(rows.get(0).title(),rows.get(1).title());
  calls.clear();SlackFeed.read(LocalDate.of(2026,10,31),2000,"SYNTHETIC",cache,reader);assertEquals(1,calls.size());assertFalse(calls.get(0).contains("users.list"));
 }
 @Test public void profileRateLimitDoesNotLoseMessagesOrExposeRawMemberIds()throws Exception{
  JSONObject cache=new JSONObject();int[] profiles={0};SlackFeed.Reader reader=(url,auth)->{if(url.contains("search.messages"))return messages();profiles[0]++;throw new BoundedHttp.RateLimitException(60000);};
  List<FeedStore.Row> rows=SlackFeed.read(LocalDate.of(2026,10,31),1000,"SYNTHETIC",cache,reader);assertEquals(2,rows.size());assertEquals(1,profiles[0]);assertEquals("Direct message · Slack member",rows.get(0).title());
  SlackFeed.read(LocalDate.of(2026,10,31),2000,"SYNTHETIC",cache,reader);assertEquals(1,profiles[0]);
 }
}

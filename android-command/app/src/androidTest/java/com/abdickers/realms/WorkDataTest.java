package com.abdickers.realms;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class WorkDataTest {
 Context c(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){c().getSharedPreferences("boards",0).edit().clear().commit();}
 @Test public void gmailReadsOneSmallPageOfMetadataAndChecksTheAccountFirst()throws Exception {
  List<String> calls=new ArrayList<>();
  MailFeed.Reader reader=(url,auth)->{calls.add(url);if(url.contains("/profile?"))return new JSONObject().put("emailAddress","work@example.com");if(url.contains("/messages?")){JSONArray ids=new JSONArray();for(int i=0;i<30;i++)ids.put(new JSONObject().put("id","test"+i));return new JSONObject().put("messages",ids).put("nextPageToken","DO_NOT_FETCH");}return new JSONObject().put("snippet","EMULATOR TEST &amp; example").put("payload",new JSONObject().put("headers",new JSONArray().put(new JSONObject().put("name","Subject").put("value","EMULATOR TEST mail"))));};
  List<FeedStore.Row> rows=MailFeed.read("work@example.com","TEST-NOT-A-TOKEN",reader);assertEquals(10,calls.size());assertEquals(9,rows.size());assertTrue(calls.get(1).contains("maxResults=8"));assertTrue(calls.stream().skip(2).allMatch(u->u.contains("format=metadata")&&!u.contains("format=full")));assertTrue(calls.stream().noneMatch(u->u.contains("pageToken=")));assertTrue(rows.get(0).body().contains("TEST & example"));
  calls.clear();assertThrows(IllegalStateException.class,()->MailFeed.read("home@example.com","TEST",reader));assertEquals("Wrong account stops before message discovery",1,calls.size());
 }
 @Test public void celigoKeepsUnknownCountsUnknownAndRejectsAnotherIntegration()throws Exception {
  JSONArray flows=new JSONArray().put(new JSONObject().put("_integrationId",CeligoFeed.INTEGRATION).put("name","EMULATOR TEST flow").put("disabled",false));List<FeedStore.Row> rows=CeligoFeed.parse(flows);assertTrue(rows.get(0).body().contains("unknown"));assertTrue(rows.get(1).body().contains("not reported"));flows.getJSONObject(0).put("numOpenError",3);assertTrue(CeligoFeed.parse(flows).get(0).body().contains("errors: 3"));flows.getJSONObject(0).put("_integrationId","OTHER");assertThrows(IllegalStateException.class,()->CeligoFeed.parse(flows));
 }
 @Test public void nativeNotesPrioritiesAndLinksStayIsolatedAndGetCorrectIcons()throws Exception {
  LocalBoard.save(c(),"work","priorities","","EMULATOR TEST priority",true);LocalBoard.Entry item=LocalBoard.all(c(),"work","priorities").get(0);assertEquals(0,LocalBoard.all(c(),"home","priorities").size());assertFalse(item.done());LocalBoard.toggle(c(),"work",item.id());assertTrue(LocalBoard.all(c(),"work","priorities").get(0).done());LocalBoard.toggle(c(),"work",item.id());assertFalse(LocalBoard.all(c(),"work","priorities").get(0).done());
  LocalBoard.save(c(),"work","notes","","EMULATOR TEST note",false);LocalBoard.Entry note=LocalBoard.all(c(),"work","notes").get(0);LocalBoard.save(c(),"work","notes",note.id(),"Edited note",false);assertEquals(1,LocalBoard.all(c(),"work","notes").size());assertEquals("Edited note",LocalBoard.all(c(),"work","notes").get(0).text());
  int before=ToolLinks.all(c(),"work").size();ToolLinks.add(c(),"work","My repository","https://github.com/abdickers/WorkHomepage");assertEquals(before+1,ToolLinks.all(c(),"work").size());assertEquals(R.drawable.brand_github,ToolLinks.all(c(),"work").get(before).icon());assertEquals(0,ToolLinks.all(c(),"home").size());for(String unsafe:new String[]{"javascript:alert(1)","http://example.com","https://user:pass@example.com/","https://127.0.0.1/"})assertThrows(IllegalArgumentException.class,()->ToolLinks.safe(unsafe));assertNotEquals(R.drawable.brand_github,ToolLinks.icon("https://github.com.evil.test/"));
 }
}

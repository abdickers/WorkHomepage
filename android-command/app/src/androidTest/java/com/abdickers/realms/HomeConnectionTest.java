package com.abdickers.realms;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.*;
import org.junit.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import android.app.Activity;
import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.anything;
import static org.junit.Assert.*;

public class HomeConnectionTest {
 Context c(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){WidgetState.prefs(c()).edit().clear().putBoolean("welcome_seen",true).commit();Vault.clear(c());FeedStore.clear(c());}
 JSONObject fixture()throws Exception{try(var in=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("home-jwe-synthetic.json")){return new JSONObject(new String(in.readAllBytes(),StandardCharsets.UTF_8));}}
 @Test public void serverJoseResponseDecryptsOnAndroidAndRejectsTamperingOrAnotherPhone()throws Exception{
  JSONObject vector=fixture(),pending=vector.getJSONObject("pending");long now=vector.getLong("now");JSONObject payload=HomeConnection.decrypt(vector.getString("jwe"),pending,now);assertEquals("abdickers@gmail.com",payload.getString("email"));assertEquals(pending.getString("state"),payload.getString("state"));
  try{HomeConnection.decrypt(vector.getString("jwe"),HomeConnection.prepare(0,now),now);fail("Another phone cannot decrypt");}catch(java.security.GeneralSecurityException expected){}
  String[] parts=vector.getString("jwe").split("\\.");parts[3]=(parts[3].startsWith("A")?"B":"A")+parts[3].substring(1);try{HomeConnection.decrypt(String.join(".",parts),pending,now);fail("Changed ciphertext must fail");}catch(java.security.GeneralSecurityException expected){}
  payload.put("email","dickersona@76fireworks.com");try{HomeConnection.validate(payload,pending.getString("state"),now);fail("Work owner cannot be installed as Home");}catch(java.security.GeneralSecurityException expected){}
 }
 @Test public void homeInstallVerifiesBothSessionsAndPreservesWorkAndOtherSettings()throws Exception{
  JSONObject vector=fixture(),payload=HomeConnection.decrypt(vector.getString("jwe"),vector.getJSONObject("pending"),vector.getLong("now"));WidgetState.prefs(c()).edit().putBoolean("home_google_connected",true).putBoolean("work_google_connected",true).putString("home_wifi","SYNTHETIC-HOME").commit();Vault.put(c(),SlackConnection.KEY,"SYNTHETIC-KEEP-SLACK");FeedStore.success(c(),"work","slack",List.of(new FeedStore.Row("SYNTHETIC WORK","",R.drawable.brand_slack)));
  List<String> calls=new ArrayList<>();HomeConnection.Transport transport=(path,jwt,session,body)->{calls.add(path);return new JSONObject().put("email","abdickers@gmail.com");};long generation=Connections.generation(c());HomeConnection.install(c(),payload,generation,transport);assertEquals(List.of("/__access/session","/native/session"),calls);assertTrue(HomeConnection.connected(c()));assertFalse(ConnectionStatus.needs(c(),"home","weather"));assertEquals("SYNTHETIC-KEEP-SLACK",Vault.get(c(),SlackConnection.KEY));assertEquals(1,FeedStore.read(c(),"work","slack").rows().size());assertEquals("SYNTHETIC-HOME",WidgetState.prefs(c()).getString("home_wifi",""));assertFalse(c().getSharedPreferences("vault",0).getString(HomeConnection.KEY,"").contains("SYNTHETIC"));
  try{HomeConnection.install(c(),payload,generation,transport);fail("Stale connection generation must fail");}catch(IllegalStateException expected){}
 }
 @Test public void homeFeedsUseStationObservationAndAllowOnlyTrustedThermostatTargets()throws Exception{
  JSONObject observation=new JSONObject().put("available",true).put("stationName","SYNTHETIC Cabin").put("observation",new JSONObject().put("time","2026-10-31T15:00:00Z").put("tempF",68).put("humidity",55).put("windMph",3).put("rainTodayIn",0));assertTrue(HomeFeed.weather(observation).get(0).title().contains("68°F"));observation.put("available",false).put("reason","STALE");try{HomeFeed.weather(observation);fail("Stale station must not be shown as current");}catch(IllegalStateException expected){}
  JSONObject devices=new JSONObject().put("devices",new JSONArray().put(new JSONObject().put("type","THERMOSTAT").put("id","SYNTHETIC_ID").put("temperature",20).put("mode","COOL")).put(new JSONObject().put("type","THERMOSTAT").put("id","../../bad")));List<FeedStore.Row> rows=HomeFeed.devices(devices,"hearth");assertEquals(1,rows.size());assertTrue(rows.get(0).title().contains("68°F"));assertEquals("thermostat:SYNTHETIC_ID",rows.get(0).key());FeedStore.success(c(),"home","hearth",rows);assertEquals("thermostat:SYNTHETIC_ID",FeedStore.read(c(),"home","hearth").rows().get(0).key());
  try{HomeConnection.request("/budget/snapshot","SYNTHETIC.JWT.SIGNATURE","",null);fail("No bank access through this client");}catch(IllegalArgumentException expected){}
 }
 @Test public void savedHearthRowOpensNativeThermostatControlsOnAnActualTap()throws Exception{
  WidgetState.mode(c(),0,"home");WidgetState.section(c(),0,"hearth");Vault.put(c(),HomeConnection.KEY,"{}");
  FeedStore.success(c(),"home","hearth",List.of(new FeedStore.Row("SYNTHETIC Thermostat","68°F · tap to adjust",R.drawable.elven_home,"thermostat:SYNTHETIC_ID")));
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(c(),MainActivity.class))){
   CommandTest.await(()->{boolean[] ready={false};scenario.onActivity(a->{android.widget.ListView list=a.dashboardView().findViewById(R.id.feed);ready[0]=list!=null&&list.getChildCount()>0;});return ready[0];});
   onData(anything()).inAdapterView(withId(R.id.feed)).atPosition(0).perform(click());
   CommandTest.await("A cached thermostat card must open native controls",()->{boolean[] ready={false};InstrumentationRegistry.getInstrumentation().runOnMainSync(()->{for(Activity a:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(a instanceof ThermostatActivity)ready[0]=true;});return ready[0];});
   pressBackUnconditionally();
  }
 }
}

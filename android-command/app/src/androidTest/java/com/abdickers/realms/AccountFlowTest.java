package com.abdickers.realms;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.json.*;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.*;

public class AccountFlowTest {
 Context c(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){WidgetState.prefs(c()).edit().clear().putBoolean("welcome_seen",true).commit();FeedStore.clear(c());Vault.clear(c());c().getSharedPreferences("boards",0).edit().clear().commit();}
 @Test public void actualOverviewCardAndUnconnectedPanelOpenAccountScreen()throws Exception{
  WidgetState.mode(c(),0,"work");WidgetState.section(c(),0,"overview");
  try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(c(),MainActivity.class))){CommandTest.await(()->{final boolean[] ready={false};scenario.onActivity(a->{ListView list=a.dashboardView().findViewById(R.id.feed);ready[0]=list!=null&&list.getChildCount()>0;});return ready[0];});scenario.onActivity(a->{ListView list=a.dashboardView().findViewById(R.id.feed);assertTrue(list.performItemClick(list.getChildAt(0),0,list.getItemIdAtPosition(0)));});onView(withText("Your accounts")).check(matches(isDisplayed()));assertEquals("beacons",WidgetState.section(c(),0));pressBackUnconditionally();}
  WidgetState.section(c(),0,"mail");Intent entry=new Intent(c(),EntryActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,0).putExtra("realm","work").putExtra("section","mail").putExtra("key","connect");
  try(ActivityScenario<EntryActivity> ignored=ActivityScenario.launch(entry)){onView(withId(R.id.sign_in_work)).perform(scrollTo()).check(matches(isDisplayed()));pressBackUnconditionally();}
 }
 @Test public void canceledGoogleSignInShowsRecoveryAndDoesNotConnectEitherAccount()throws Exception{
  try(ActivityScenario<AccountActivity> scenario=ActivityScenario.launch(new Intent(c(),AccountActivity.class))){onView(withId(R.id.sign_in_home)).perform(scrollTo()).check(matches(isDisplayed()));onView(withId(R.id.sign_in_work)).perform(scrollTo()).check(matches(isDisplayed()));scenario.onActivity(a->{a.onActivityResult(GoogleConnection.REQUEST_MAIL,Activity.RESULT_CANCELED,null);TextView feedback=a.findViewById(R.id.account_feedback);assertTrue(feedback.getText().toString().contains("canceled"));assertTrue(a.findViewById(R.id.sign_in_home).isEnabled());assertTrue(a.findViewById(R.id.sign_in_work).isEnabled());});assertFalse(WidgetState.prefs(c()).getBoolean("home_google_connected",false));assertFalse(WidgetState.prefs(c()).getBoolean("work_google_connected",false));onView(withId(R.id.account_feedback)).perform(scrollTo()).check(matches(isDisplayed()));new CommandTest().shot("accounts-canceled-sign-in");}
 }
 @Test public void googleCalendarReadsOnlySelectedDayAndOnePageAcrossDst()throws Exception{
  FeedStore.success(c(),"home","mail",List.of(new FeedStore.Row("HOME TEST","",R.drawable.brand_gmail)));FeedStore.success(c(),"work","jira",List.of(new FeedStore.Row("WORK TEST","",R.drawable.brand_jira)));Connections.invalidate(c(),"home");assertEquals(0,FeedStore.read(c(),"home","mail").rows().size());assertEquals(1,FeedStore.read(c(),"work","jira").rows().size());
  List<String> calls=new ArrayList<>();long day=LocalDate.of(2026,3,8).toEpochDay();List<FeedStore.Row> rows=GoogleCalendarFeed.read(day,ZoneId.of("America/Chicago"),"TEST-NOT-A-TOKEN",(url,auth)->{calls.add(url);JSONArray events=new JSONArray();for(int i=0;i<30;i++)events.put(new JSONObject().put("summary","EMULATOR calendar "+i).put("start",new JSONObject().put("dateTime","2026-03-08T15:00:00Z")));return new JSONObject().put("items",events).put("nextPageToken","DO_NOT_FOLLOW");});assertEquals(1,calls.size());assertTrue(calls.get(0).contains("maxResults=20"));assertTrue(calls.get(0).contains("timeMin=2026-03-08T06%3A00%3A00Z"));assertTrue(calls.get(0).contains("timeMax=2026-03-09T05%3A00%3A00Z"));assertFalse(calls.get(0).contains("pageToken="));assertEquals(20,rows.size());
 }
 @Test public void selectingFreshPanelReusesCacheAndUnconnectedPanelDoesNotEnqueue()throws Exception{
  WidgetState.mode(c(),0,"work");WidgetState.prefs(c()).edit().putBoolean("work_google_connected",true).commit();FeedStore.success(c(),"work","mail",List.of(new FeedStore.Row("SYNTHETIC","",R.drawable.brand_gmail)));
  CommandWidget.dispatch(c(),0,"mail");assertEquals("mail",WidgetState.section(c(),0));assertFalse(Refresh.running(c(),"work","mail"));
  WidgetState.prefs(c()).edit().putBoolean("work_google_connected",false).commit();FeedStore.clear(c());CommandWidget.dispatch(c(),0,"jira");assertFalse(Refresh.running(c(),"work","jira"));assertEquals(0,WidgetState.prefs(c()).getLong("retry.work.jira",0));
 }
}

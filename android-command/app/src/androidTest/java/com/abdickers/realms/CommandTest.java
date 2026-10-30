package com.abdickers.realms;

import android.appwidget.*;
import android.content.*;
import android.graphics.Bitmap;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.util.*;
import java.io.*;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.action.ViewActions.*;

@RunWith(AndroidJUnit4.class)
public class CommandTest {
 Context c(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){WidgetState.prefs(c()).edit().clear().commit();FeedStore.clear(c());Vault.clear(c());c().getSharedPreferences("boards",0).edit().clear().commit();}
 @Test public void credentialsAreEncryptedAndErrorsRetainData()throws Exception{
  String secret="EMULATOR-TEST-NOT-A-REAL-TOKEN";Vault.put(c(),"vercel",secret);assertEquals(secret,Vault.get(c(),"vercel"));assertFalse(c().getSharedPreferences("vault",0).getString("vercel","").contains(secret));
  FeedStore.success(c(),"work","jira",List.of(new FeedStore.Row("TEST task","TEST data",R.drawable.icon_quest)));long checked=FeedStore.read(c(),"work","jira").checkedAt();FeedStore.failure(c(),"work","jira","TEST unavailable");assertEquals("TEST task",FeedStore.read(c(),"work","jira").rows().get(0).title());assertEquals(checked,FeedStore.read(c(),"work","jira").checkedAt());assertEquals(0,FeedStore.read(c(),"home","jira").rows().size());
  long before=Connections.generation(c());Connections.invalidate(c());assertFalse("Old connection cannot repopulate cleared data",Connections.commitIfCurrent(c(),before,()->FeedStore.success(c(),"work","jira",List.of(new FeedStore.Row("OLD DATA","Should never return",R.drawable.icon_quest)))));assertEquals(0,FeedStore.read(c(),"work","jira").rows().size());Vault.clear(c());assertEquals("",Vault.get(c(),"vercel"));
 }
 @Test public void settingsAndAppAreNativeAndFit()throws Exception{
  WidgetState.mode(c(),0,"work");WidgetState.section(c(),0,"jira");FeedStore.success(c(),"work","jira",List.of(new FeedStore.Row("APP CONTENT TEST","Native data must be visible",R.drawable.icon_quest)));
  for(Class<?> activity:new Class<?>[]{MainActivity.class,SettingsActivity.class})try(ActivityScenario<?> scenario=ActivityScenario.launch(new Intent(c(),activity))){idle();if(activity==MainActivity.class){try{await("Native app must display its saved Jira row",()->{final boolean[] found={false};scenario.onActivity(a->{ListView list=((MainActivity)a).dashboardView().findViewById(R.id.feed);found[0]=list!=null&&list.getCount()==1&&list.getChildCount()>0&&((TextView)list.getChildAt(0).findViewById(R.id.row_title)).getText().toString().equals("APP CONTENT TEST");});return found[0];});scenario.onActivity(a->((MainActivity)a).dashboardView().findViewById(R.id.home).performClick());await("App Home mode action",()->WidgetState.mode(c(),0).equals("home"));scenario.onActivity(a->((MainActivity)a).dashboardView().findViewById(R.id.work).performClick());await("App Work mode action",()->WidgetState.mode(c(),0).equals("work"));scenario.onActivity(a->((MainActivity)a).dashboardView().findViewById(R.id.tools).performClick());await("Company tool grid must contain actual tiles",()->{final boolean[] ready={false};scenario.onActivity(a->{GridView grid=((MainActivity)a).dashboardView().findViewById(R.id.tool_grid);ready[0]=grid!=null&&grid.isShown()&&grid.getCount()>=20&&grid.getNumColumns()>=2&&grid.getChildCount()>0;});return ready[0];});shot("native-app-company-tool-grid");}catch(AssertionError e){shot("failed-native-app");scenario.onActivity(a->{ListView list=((MainActivity)a).dashboardView().findViewById(R.id.feed);android.util.Log.e("AppWidgetHostView","TEST APP count="+(list==null?-1:list.getCount())+" children="+(list==null?-1:list.getChildCount())+" mode="+WidgetState.mode(c(),0));});throw e;}}scenario.onActivity(a->{View content=a.findViewById(android.R.id.content);bounds(content,content.getWidth());});shot(activity.getSimpleName());}
 }
 @Test public void realWidgetControlsSwitchRealmAndCalendarWithoutOpeningBrowser()throws Exception{
  InstrumentationRegistry.getInstrumentation().getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");AppWidgetHost host=new AppWidgetHost(c(),914);host.startListening();AppWidgetManager manager=AppWidgetManager.getInstance(c());int id=host.allocateAppWidgetId(),second=host.allocateAppWidgetId();
  try{assertTrue(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c(),CommandWidget.class),options(390,700)));assertTrue(manager.bindAppWidgetIdIfAllowed(second,new ComponentName(c(),CommandWidget.class),options(390,700)));WidgetState.mode(c(),id,"work");WidgetState.mode(c(),second,"home");
   List<FeedStore.Row> rows=new ArrayList<>();for(int n=0;n<20;n++)rows.add(new FeedStore.Row("EMULATOR TEST · Task "+n,"Synthetic test data · In Progress · This list scrolls inside the widget",R.drawable.icon_quest));FeedStore.success(c(),"work","jira",rows);
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(c(),MainActivity.class))){final AppWidgetHostView[] view={null};scenario.onActivity(a->{view[0]=host.createView(a,id,manager.getAppWidgetInfo(id));FrameLayout frame=new FrameLayout(a);frame.setPadding(0,72,0,72);frame.addView(view[0]);a.setContentView(frame);});
    for(int[] size:new int[][]{{390,700},{300,500},{220,340}}){
     int available=Math.round((c().getResources().getDisplayMetrics().widthPixels-32)/c().getResources().getDisplayMetrics().density);int width=Math.min(size[0],available);Bundle options=options(width,size[1]);manager.updateAppWidgetOptions(id,options);WidgetState.section(c(),id,"jira");manager.updateAppWidget(id,CommandWidget.views(c(),id,options));scenario.onActivity(a->{float density=a.getResources().getDisplayMetrics().density;view[0].setLayoutParams(new FrameLayout.LayoutParams(Math.round(width*density),Math.round(size[1]*density)));});idle();
     try{await(()->{final boolean[] visible={false};scenario.onActivity(a->{View refresh=view[0].findViewById(R.id.refresh);visible[0]=refresh!=null&&refresh.isShown()&&refresh.getWidth()>0;});return visible[0];});}catch(AssertionError failure){shot("failed-widget-"+width);throw failure;}
     scenario.onActivity(a->{View refresh=view[0].findViewById(R.id.refresh),feed=view[0].findViewById(R.id.feed);assertTrue("Refresh must be shown at width "+width,refresh.isShown());assertTrue("Feed retains at least 60dp of usable height",feed.getHeight()>=60*a.getResources().getDisplayMetrics().density);bounds(view[0],view[0].getWidth());android.graphics.Rect visible=new android.graphics.Rect();assertTrue("Refresh must intersect the phone screen at width "+width,refresh.getGlobalVisibleRect(visible));assertEquals("Refresh is fully within the phone",refresh.getWidth(),visible.width());});shot("work-widget-"+width);
     if(size[0]==390){try{await(()->{final boolean[] ready={false};scenario.onActivity(a->ready[0]=((ListView)view[0].findViewById(R.id.feed)).getCount()==20);return ready[0];});scenario.onActivity(a->((ListView)view[0].findViewById(R.id.feed)).setSelection(19));await(()->{final boolean[] last={false};scenario.onActivity(a->last[0]=((ListView)view[0].findViewById(R.id.feed)).getLastVisiblePosition()==19);return last[0];});shot("work-widget-scrolled-to-last-task");}catch(AssertionError failure){shot("failed-widget-collection");scenario.onActivity(a->{ListView list=view[0].findViewById(R.id.feed);android.util.Log.e("AppWidgetHostView","TEST collection count="+list.getCount()+" last="+list.getLastVisiblePosition()+" section="+WidgetState.section(c(),id));});throw failure;}}
    }
    // Restore a full-height slot so both full navigation rows are visible for actions.
    int available=Math.round((c().getResources().getDisplayMetrics().widthPixels-32)/c().getResources().getDisplayMetrics().density);Bundle expanded=options(Math.min(390,available),700);manager.updateAppWidgetOptions(id,expanded);manager.updateAppWidget(id,CommandWidget.views(c(),id,expanded));scenario.onActivity(a->view[0].setLayoutParams(new FrameLayout.LayoutParams(Math.round(Math.min(390,available)*a.getResources().getDisplayMetrics().density),Math.round(700*a.getResources().getDisplayMetrics().density))));idle();
    scenario.onActivity(a->assertTrue(view[0].findViewById(R.id.home).performClick()));await(()->WidgetState.mode(c(),id).equals("home"));assertEquals("home",WidgetState.decision(c(),id).realm());assertEquals("home",WidgetState.mode(c(),second));shot("home-widget");
    scenario.onActivity(a->view[0].findViewById(R.id.auto).performClick());await(()->WidgetState.mode(c(),id).equals("auto"));
    scenario.onActivity(a->view[0].findViewById(R.id.calendar).performClick());await(()->WidgetState.section(c(),id).equals("calendar"));idle();long before=WidgetState.day(c(),id);scenario.onActivity(a->view[0].findViewById(R.id.next).performClick());await(()->WidgetState.day(c(),id)==before+1);scenario.onActivity(a->view[0].findViewById(R.id.date).performClick());await(()->WidgetState.day(c(),id)==java.time.LocalDate.now().toEpochDay());
    // Exercise the actual collection fill-in PendingIntent, not just the data model.
    WidgetState.mode(c(),id,"work");WidgetState.section(c(),id,"priorities");LocalBoard.save(c(),"work","priorities","","EMULATOR PRIORITY",false);CommandWidget.update(c(),id);await(()->{final boolean[] ready={false};scenario.onActivity(a->{ListView list=view[0].findViewById(R.id.feed);ready[0]=list!=null&&list.getChildCount()>0&&((TextView)list.getChildAt(0).findViewById(R.id.row_title)).getText().toString().equals("EMULATOR PRIORITY");});return ready[0];});scenario.onActivity(a->{ListView list=view[0].findViewById(R.id.feed);assertTrue(list.performItemClick(list.getChildAt(0),0,list.getItemIdAtPosition(0)));});await(()->LocalBoard.all(c(),"work","priorities").get(0).done());shot("work-widget-priority-completed");
    WidgetState.section(c(),id,"notes");CommandWidget.update(c(),id);await(()->{final boolean[] ready={false};scenario.onActivity(a->ready[0]=((TextView)view[0].findViewById(R.id.refresh)).getText().toString().equals("Add"));return ready[0];});scenario.onActivity(a->view[0].findViewById(R.id.refresh).performClick());onView(withHint("What would you like to capture?")).inRoot(isDialog()).perform(replaceText("EMULATOR NOTE SAVED FROM WIDGET"),closeSoftKeyboard());onView(withText("Save")).inRoot(isDialog()).perform(click());await(()->LocalBoard.all(c(),"work","notes").stream().anyMatch(n->n.text().equals("EMULATOR NOTE SAVED FROM WIDGET")));shot("work-widget-note-created");
    assertEquals("No data pull on navigation",0,WidgetState.prefs(c()).getLong("retry.work.systems",0));
   }
  }finally{host.deleteAppWidgetId(id);host.deleteAppWidgetId(second);host.stopListening();host.deleteHost();InstrumentationRegistry.getInstrumentation().getUiAutomation().dropShellPermissionIdentity();}
 }
 static Bundle options(int w,int h){Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);return b;}
 static void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(450);}
 static void await(java.util.function.BooleanSupplier test){await("Widget action did not arrive",test);}
 static void await(String message,java.util.function.BooleanSupplier test){for(int n=0;n<100&&!test.getAsBoolean();n++)SystemClock.sleep(100);assertTrue(message,test.getAsBoolean());idle();}
 static void bounds(View v,int width){if(v.getVisibility()!=View.VISIBLE||!v.isShown())return;assertTrue(v.getClass().getSimpleName()+" exceeds available width",v.getWidth()<=width);if(v instanceof ViewGroup g)for(int n=0;n<g.getChildCount();n++)bounds(g.getChildAt(n),width);}
 void shot(String name)throws Exception{idle();Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(c().getExternalFilesDir(null),"screenshots");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();}
}

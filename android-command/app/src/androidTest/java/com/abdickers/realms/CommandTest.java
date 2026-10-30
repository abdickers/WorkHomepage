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

@RunWith(AndroidJUnit4.class)
public class CommandTest {
 Context c(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){WidgetState.prefs(c()).edit().clear().commit();FeedStore.clear(c());Vault.clear(c());}
 @Test public void credentialsAreEncryptedAndErrorsRetainData()throws Exception{
  String secret="EMULATOR-TEST-NOT-A-REAL-TOKEN";Vault.put(c(),"vercel",secret);assertEquals(secret,Vault.get(c(),"vercel"));assertFalse(c().getSharedPreferences("vault",0).getString("vercel","").contains(secret));
  FeedStore.success(c(),"work","jira",List.of(new FeedStore.Row("TEST task","TEST data",R.drawable.icon_quest)));long checked=FeedStore.read(c(),"work","jira").checkedAt();FeedStore.failure(c(),"work","jira","TEST unavailable");assertEquals("TEST task",FeedStore.read(c(),"work","jira").rows().get(0).title());assertEquals(checked,FeedStore.read(c(),"work","jira").checkedAt());assertEquals(0,FeedStore.read(c(),"home","jira").rows().size());
  long before=Connections.generation(c());Connections.invalidate(c());assertFalse("Old connection cannot repopulate cleared data",Connections.commitIfCurrent(c(),before,()->FeedStore.success(c(),"work","jira",List.of(new FeedStore.Row("OLD DATA","Should never return",R.drawable.icon_quest)))));assertEquals(0,FeedStore.read(c(),"work","jira").rows().size());Vault.clear(c());assertEquals("",Vault.get(c(),"vercel"));
 }
 @Test public void settingsAndAppAreNativeAndFit()throws Exception{
  for(Class<?> activity:new Class<?>[]{MainActivity.class,SettingsActivity.class})try(ActivityScenario<?> scenario=ActivityScenario.launch(new Intent(c(),activity))){idle();scenario.onActivity(a->{View content=a.findViewById(android.R.id.content);bounds(content,content.getWidth());});shot(activity.getSimpleName());}
 }
 @Test public void realWidgetControlsSwitchRealmAndCalendarWithoutOpeningBrowser()throws Exception{
  InstrumentationRegistry.getInstrumentation().getUiAutomation().adoptShellPermissionIdentity("android.permission.BIND_APPWIDGET");AppWidgetHost host=new AppWidgetHost(c(),914);host.startListening();AppWidgetManager manager=AppWidgetManager.getInstance(c());int id=host.allocateAppWidgetId(),second=host.allocateAppWidgetId();
  try{assertTrue(manager.bindAppWidgetIdIfAllowed(id,new ComponentName(c(),CommandWidget.class),options(390,700)));assertTrue(manager.bindAppWidgetIdIfAllowed(second,new ComponentName(c(),CommandWidget.class),options(390,700)));WidgetState.mode(c(),id,"work");WidgetState.mode(c(),second,"home");
   List<FeedStore.Row> rows=new ArrayList<>();for(int n=0;n<20;n++)rows.add(new FeedStore.Row("EMULATOR TEST · Task "+n,"Synthetic test data · In Progress · This list scrolls inside the widget",R.drawable.icon_quest));FeedStore.success(c(),"work","jira",rows);
   try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(new Intent(c(),MainActivity.class))){final AppWidgetHostView[] view={null};scenario.onActivity(a->{view[0]=host.createView(a,id,manager.getAppWidgetInfo(id));FrameLayout frame=new FrameLayout(a);frame.setPadding(0,72,0,72);frame.addView(view[0]);a.setContentView(frame);});
    for(int[] size:new int[][]{{390,700},{300,500},{220,340}}){
     int available=Math.round((c().getResources().getDisplayMetrics().widthPixels-32)/c().getResources().getDisplayMetrics().density);int width=Math.min(size[0],available);Bundle options=options(width,size[1]);manager.updateAppWidgetOptions(id,options);WidgetState.section(c(),id,"jira");manager.updateAppWidget(id,CommandWidget.views(c(),id,options));scenario.onActivity(a->{float density=a.getResources().getDisplayMetrics().density;view[0].setLayoutParams(new FrameLayout.LayoutParams(Math.round(width*density),Math.round(size[1]*density)));});idle();
     scenario.onActivity(a->{View refresh=view[0].findViewById(R.id.refresh),feed=view[0].findViewById(R.id.feed);assertTrue(refresh.isShown());assertTrue("Feed retains usable height",feed.getHeight()>0);bounds(view[0],view[0].getWidth());android.graphics.Rect visible=new android.graphics.Rect();assertTrue(refresh.getGlobalVisibleRect(visible));assertEquals("Refresh is fully within the phone",refresh.getWidth(),visible.width());});shot("work-widget-"+width);
     if(size[0]==390){await(()->{final boolean[] ready={false};scenario.onActivity(a->ready[0]=((ListView)view[0].findViewById(R.id.feed)).getCount()==20);return ready[0];});scenario.onActivity(a->((ListView)view[0].findViewById(R.id.feed)).setSelection(19));await(()->{final boolean[] last={false};scenario.onActivity(a->last[0]=((ListView)view[0].findViewById(R.id.feed)).getLastVisiblePosition()==19);return last[0];});shot("work-widget-scrolled-to-last-task");}
    }
    // Restore a full-height slot so both full navigation rows are visible for actions.
    int available=Math.round((c().getResources().getDisplayMetrics().widthPixels-32)/c().getResources().getDisplayMetrics().density);Bundle expanded=options(Math.min(390,available),700);manager.updateAppWidgetOptions(id,expanded);manager.updateAppWidget(id,CommandWidget.views(c(),id,expanded));scenario.onActivity(a->view[0].setLayoutParams(new FrameLayout.LayoutParams(Math.round(Math.min(390,available)*a.getResources().getDisplayMetrics().density),Math.round(700*a.getResources().getDisplayMetrics().density))));idle();
    scenario.onActivity(a->assertTrue(view[0].findViewById(R.id.home).performClick()));await(()->WidgetState.mode(c(),id).equals("home"));assertEquals("home",WidgetState.decision(c(),id).realm());assertEquals("home",WidgetState.mode(c(),second));shot("home-widget");
    scenario.onActivity(a->view[0].findViewById(R.id.auto).performClick());await(()->WidgetState.mode(c(),id).equals("auto"));
    scenario.onActivity(a->view[0].findViewById(R.id.calendar).performClick());await(()->WidgetState.section(c(),id).equals("calendar"));idle();long before=WidgetState.day(c(),id);scenario.onActivity(a->view[0].findViewById(R.id.next).performClick());await(()->WidgetState.day(c(),id)==before+1);scenario.onActivity(a->view[0].findViewById(R.id.date).performClick());await(()->WidgetState.day(c(),id)==java.time.LocalDate.now().toEpochDay());
    assertEquals("No data pull on navigation",0,WidgetState.prefs(c()).getLong("retry.work.systems",0));
   }
  }finally{host.deleteAppWidgetId(id);host.deleteAppWidgetId(second);host.stopListening();host.deleteHost();InstrumentationRegistry.getInstrumentation().getUiAutomation().dropShellPermissionIdentity();}
 }
 static Bundle options(int w,int h){Bundle b=new Bundle();b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h);b.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);return b;}
 static void idle(){InstrumentationRegistry.getInstrumentation().waitForIdleSync();SystemClock.sleep(450);}
 static void await(java.util.function.BooleanSupplier test){for(int n=0;n<40&&!test.getAsBoolean();n++)SystemClock.sleep(100);assertTrue("Widget action did not arrive",test.getAsBoolean());idle();}
 static void bounds(View v,int width){assertTrue(v.getClass().getSimpleName()+" exceeds available width",v.getWidth()<=width);if(v instanceof ViewGroup g)for(int n=0;n<g.getChildCount();n++)bounds(g.getChildAt(n),width);}
 void shot(String name)throws Exception{idle();Bitmap b=InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();assertNotNull(b);File dir=new File(c().getExternalFilesDir(null),"screenshots");assertTrue(dir.isDirectory()||dir.mkdirs());try(FileOutputStream out=new FileOutputStream(new File(dir,name+".png"))){assertTrue(b.compress(Bitmap.CompressFormat.PNG,100,out));}b.recycle();}
}

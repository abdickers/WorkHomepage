package com.abdickers.realms;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import java.util.*;

/** Account-first setup. Normal setup never asks the user for developer keys or setup files. */
public final class AccountActivity extends Activity {
 private LinearLayout root;
 private TextView feedback;
 private final List<Button> signInButtons=new ArrayList<>();
 private String pendingRealm="work";
 private boolean busy;
 @Override protected void onCreate(Bundle state){super.onCreate(state);if(state!=null)pendingRealm=state.getString("pending_realm","work");render();if(state!=null)message("If sign-in was interrupted, tap Sign in to try again.");}
 private void render(){
  signInButtons.clear();ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);root.setBackgroundColor(0xFF171D23);scroll.addView(root);setContentView(scroll);scroll.setOnApplyWindowInsetsListener((v,insets)->{android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());root.setPadding(24,bars.top+24,24,bars.bottom+24);return insets;});
  text("Your accounts",26,0xFFE8EDF4);text("Sign in to Home and Work once. The app keeps their information separate.",15,0xFFCFDAE9);
  feedback=new TextView(this);feedback.setId(R.id.account_feedback);feedback.setTextColor(0xFFFFD98B);feedback.setTextSize(15);feedback.setPadding(0,18,0,12);feedback.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);root.addView(feedback);
  String focus=getIntent().getStringExtra("source");if(focus!=null&&!Set.of("mail","calendar","overview").contains(focus))message(Panels.title(focus)+": this service’s app connection is not ready yet. Google sign-in currently connects email and calendar. This is an app limitation; you do not need to configure API keys.");
  for(String realm:"work".equals(getIntent().getStringExtra("realm"))?List.of("work","home"):List.of("home","work")){
   boolean connected=WidgetState.prefs(this).getBoolean(realm+"_google_connected",false),calendar=WidgetState.prefs(this).getBoolean(realm+"_google_calendar_connected",false);
   text(realm.equals("work")?"Work · Gondor":"Home · The Shire",22,0xFFE8EDF4);text(GoogleConnection.expected(realm),15,0xFFCFDAE9);text(connected?(calendar?"Email connected · calendar access allowed":"Email connected · calendar permission needed"):"Not signed in",14,connected?0xFFB3E0CB:0xFFCFDAE9);
   Button sign=button(connected?"Reconnect "+label(realm)+" with Google":"Sign in to "+label(realm)+" with Google",()->signIn(realm));sign.setId(realm.equals("work")?R.id.sign_in_work:R.id.sign_in_home);signInButtons.add(sign);
   if(connected)button("Open "+label(realm)+" dashboard",()->open(realm));
  }
  text("Your other services",19,0xFFE8EDF4);text("Jira, Slack, production flows, deployments, Nest and other services still need the app’s account connection layer. They are not connected by this preview. You should not have to enter keys or exchange setup files.",14,0xFFCFDAE9);
  button("Continue to dashboard",()->{WidgetState.prefs(this).edit().putBoolean("welcome_seen",true).commit();startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();});
 }
 private void signIn(String realm){if(busy)return;pendingRealm=realm;busy=true;for(Button b:signInButtons)b.setEnabled(false);GoogleConnection.connect(this,realm,this::complete,this::failed,this::message);}
 private void complete(){busy=false;for(Button b:signInButtons)b.setEnabled(true);WidgetState.prefs(this).edit().putBoolean("welcome_seen",true).commit();WidgetState.mode(this,0,pendingRealm);render();message(label(pendingRealm)+" connected. Loading your email and selected day’s calendar…");Refresh.enqueue(this,pendingRealm,"mail",WidgetState.day(this,0));if(WidgetState.prefs(this).getBoolean(pendingRealm+"_google_calendar_connected",false))Refresh.enqueue(this,pendingRealm,"calendar",WidgetState.day(this,0));CommandWidget.updateAll(this);}
 private void failed(String reason){busy=false;for(Button b:signInButtons)b.setEnabled(true);message(reason);}
 private void message(String value){if(feedback!=null)feedback.setText(value);}
 private void open(String realm){WidgetState.prefs(this).edit().putBoolean("welcome_seen",true).commit();WidgetState.mode(this,0,realm);WidgetState.section(this,0,"overview");startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}
 private static String label(String realm){return realm.equals("work")?"Work":"Home";}
 private void text(String value,int size,int color){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);t.setPadding(0,14,0,6);root.addView(t);}
 private Button button(String title,Runnable action){Button b=new Button(this);b.setText(title);b.setTextColor(0xFFE8EDF4);b.setBackgroundResource(R.drawable.royal_button);b.setAllCaps(false);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,10,0,8);root.addView(b,lp);b.setOnClickListener(v->action.run());return b;}
 @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(request==GoogleConnection.REQUEST_MAIL){busy=true;for(Button b:signInButtons)b.setEnabled(false);if(result==RESULT_OK&&intent!=null)GoogleConnection.finish(this,pendingRealm,intent,this::complete,this::failed,this::message);else failed("Sign-in was canceled. Your accounts have not been changed. Tap Sign in whenever you are ready.");}}
 @Override protected void onSaveInstanceState(Bundle state){state.putString("pending_realm",pendingRealm);super.onSaveInstanceState(state);}
}

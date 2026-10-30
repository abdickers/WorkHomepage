package com.abdickers.realms;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import java.util.*;

public final class SettingsActivity extends Activity {
 private LinearLayout root;
 private final Map<String,EditText> fields=new LinkedHashMap<>();
 private final Map<String,EditText> secrets=new LinkedHashMap<>();
 private Switch wifi;
 @Override protected void onCreate(Bundle state){super.onCreate(state);ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,32,24,24);root.setBackgroundColor(0xFF101C2B);scroll.addView(root);setContentView(scroll);scroll.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());root.setPadding(24,bars.top+24,24,bars.bottom+24);return i;});
  heading("Connections & switching");label("Work and Home use separate data. Saved information is visible on your unlocked home screen. No banking or Warcraft is connected.");
  heading("Automatic mode");label("Manual Home/Work stays selected until you tap Auto. Known Wi-Fi takes priority over the schedule. Outside work hours, Auto selects Home. Background checks are approximately every 30 minutes and may be delayed by Android.");
  wifi=new Switch(this);wifi.setText("Recognize configured Wi-Fi networks");wifi.setTextColor(0xFFE4EAF2);wifi.setChecked(WidgetState.prefs(this).getBoolean("wifi_enabled",false));root.addView(wifi);
  field("home_wifi","Home Wi-Fi names · one per line","");field("work_wifi","Work Wi-Fi names · one per line","");field("days","Work days · Mon=1 … Sun=7","1,2,3,4,5");field("start_text","Work starts · HH:mm","09:00");field("end_text","Work ends · HH:mm","17:00");
  button("Allow Wi-Fi recognition",()->{new AlertDialog.Builder(this).setMessage("Android treats Wi-Fi names as location-sensitive. This option reads the connected network name only; it does not collect GPS coordinates. Allow location while using the app first. For automatic Wi-Fi selection while the app is closed, separately choose Allow all the time in Android’s app permissions. Without it, schedule and manual switching still work.").setPositiveButton("Continue",(d,w)->requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION},22)).setNegativeButton("Cancel",null).show();});
  button("Android background location settings",()->startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,android.net.Uri.parse("package:"+getPackageName()))));
  heading("Calendar");label("Uses Android’s locally synced calendars. Specify each account email so Work events do not appear in Home. Reads only the selected day, up to 20 events.");field("work_calendar","Work calendar account email","");field("home_calendar","Home calendar account email","");button("Allow calendar access",()->requestPermissions(new String[]{Manifest.permission.READ_CALENDAR},23));
  heading("Work services · advanced connection setup");label("These native readers are not connected to your ChatGPT integrations yet. Only enter credentials you have authorized for this app. They are encrypted with Android Keystore and never included in the APK. Blank fields keep an existing credential. No service is queried merely by opening settings.");
  field("vercel_team","Vercel team ID · team_…","");secret("vercel","Vercel API token");field("jira_email","Jira account email","");secret("jira","Jira API token");secret("slack","Slack user token with search:read");
  button("Save settings",this::save);
  button("Disconnect services & clear saved feeds",()->new AlertDialog.Builder(this).setMessage("Remove this app’s saved credentials and cached feeds? Provider tokens are not revoked at the provider.").setPositiveButton("Clear",(d,w)->{Vault.clear(this);FeedStore.clear(this);CommandWidget.updateAll(this);Toast.makeText(this,"Connections and saved feeds cleared",Toast.LENGTH_LONG).show();}).setNegativeButton("Cancel",null).show());
 }
 private void heading(String title){TextView t=new TextView(this);t.setText(title);t.setTextSize(22);t.setTextColor(0xFFF4E6BE);t.setPadding(0,18,0,10);root.addView(t);}
 private void label(String value){TextView t=new TextView(this);t.setText(value);t.setTextSize(14);t.setTextColor(0xFFCFDAE9);root.addView(t);}
 private void field(String key,String title,String fallback){label(title);EditText input=new EditText(this);input.setTextColor(0xFFE4EAF2);input.setText(WidgetState.prefs(this).getString(key,fallback));if(key.endsWith("wifi")){input.setMinLines(2);input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_MULTI_LINE);}else input.setSingleLine();root.addView(input);fields.put(key,input);}
 private void secret(String key,String title){label(title);EditText input=new EditText(this);input.setTextColor(0xFFE4EAF2);input.setHint("Not changed");input.setHintTextColor(0xFFADBCCC);input.setSingleLine();input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);root.addView(input);secrets.put(key,input);}
 private void button(String title,Runnable action){Button b=new Button(this);b.setText(title);root.addView(b);b.setOnClickListener(v->action.run());}
 private void save(){try{int start=minutes(fields.get("start_text").getText().toString()),end=minutes(fields.get("end_text").getText().toString());if(start==end)throw new IllegalArgumentException("Work start and end must differ.");String days=fields.get("days").getText().toString().trim();if(!days.matches("[1-7](,[1-7])*"))throw new IllegalArgumentException("Use work-day numbers separated by commas, such as 1,2,3,4,5.");Set<String> overlap=new HashSet<>(ModePolicy.networks(fields.get("home_wifi").getText().toString()));overlap.retainAll(ModePolicy.networks(fields.get("work_wifi").getText().toString()));if(!overlap.isEmpty())throw new IllegalArgumentException("A Wi-Fi network cannot belong to both Home and Work.");for(Map.Entry<String,EditText> e:secrets.entrySet()){String value=e.getValue().getText().toString().trim();if(!value.isEmpty())Vault.put(this,e.getKey(),value);e.getValue().setText("");}android.content.SharedPreferences.Editor edit=WidgetState.prefs(this).edit().putBoolean("wifi_enabled",wifi.isChecked()).putInt("start",start).putInt("end",end);for(Map.Entry<String,EditText> e:fields.entrySet())edit.putString(e.getKey(),e.getValue().getText().toString().trim());edit.commit();CommandWidget.updateAll(this);Toast.makeText(this,"Saved. Tap Auto on the widget to use these rules.",Toast.LENGTH_LONG).show();}catch(Exception e){new AlertDialog.Builder(this).setMessage(e instanceof IllegalArgumentException?e.getMessage():"Could not save encrypted credentials. Existing settings retained.").setPositiveButton("OK",null).show();}}
 static int minutes(String raw){if(!raw.matches("(?:[01]\\d|2[0-3]):[0-5]\\d"))throw new IllegalArgumentException("Use 24-hour time, such as 09:00 or 17:30.");return Integer.parseInt(raw.substring(0,2))*60+Integer.parseInt(raw.substring(3));}
}

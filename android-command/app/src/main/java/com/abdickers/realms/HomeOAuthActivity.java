package com.abdickers.realms;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.widget.*;
import androidx.browser.customtabs.CustomTabsIntent;
import org.json.JSONObject;
import java.util.Set;
import java.util.concurrent.Executors;

/** Browser approval on the existing private site; encrypted return belongs to this phone. */
public final class HomeOAuthActivity extends Activity {
 private TextView status;private Button retry;private boolean busy,closed;
 @Override protected void onCreate(Bundle saved){super.onCreate(saved);LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(24,90,24,24);root.setBackgroundColor(0xFF171D23);status=new TextView(this);status.setTextColor(0xFFE8EDF4);status.setTextSize(19);root.addView(status);retry=new Button(this);retry.setText("Connect Home devices & weather");retry.setOnClickListener(v->begin());root.addView(retry);Button back=new Button(this);back.setText("Back to accounts");back.setOnClickListener(v->finish());root.addView(back);setContentView(root);if(getIntent().getData()!=null)receive(getIntent().getData());else if(saved==null)begin();else fail("Finish approval in your browser, or tap Connect to start again.");}
 @Override protected void onNewIntent(Intent intent){super.onNewIntent(intent);setIntent(intent);if(intent.getData()!=null)receive(intent.getData());}
 private void begin(){if(busy)return;if(!WidgetState.prefs(this).getBoolean("home_google_connected",false)){fail("Sign in to Home with Google first.");return;}busy=true;retry.setEnabled(false);status.setText("Opening your private Home sign-in…");long generation=Connections.generation(this);var executor=Executors.newSingleThreadExecutor();executor.execute(()->{try{JSONObject pending=HomeConnection.prepare(generation,System.currentTimeMillis());if(closed)return;Vault.put(this,HomeConnection.PENDING,pending.toString());Uri url=Uri.parse(HomeConnection.ORIGIN+"/native/home").buildUpon().appendQueryParameter("state",pending.getString("state")).appendQueryParameter("key",pending.getString("public")).build();runOnUiThread(()->{if(!closed){busy=false;retry.setEnabled(true);status.setText("Use abdickers@gmail.com in the browser. Approve Connect this phone, then Return to Middle-earth Command.");try{new CustomTabsIntent.Builder().build().launchUrl(this,url);}catch(Exception e){fail("No browser could open Home sign-in. Please try again.");}}});}catch(Exception e){runOnUiThread(()->fail("Home sign-in could not open. Please try again."));}finally{executor.shutdown();}});}
 private void receive(Uri callback){if(busy)return;busy=true;retry.setEnabled(false);status.setText("Checking your private Home connection…");var executor=Executors.newSingleThreadExecutor();executor.execute(()->{try{
  if(!"com.abdickers.realms".equals(callback.getScheme())||!"oauth".equals(callback.getHost())||!"/home".equals(callback.getPath())||callback.getQuery()!=null||callback.getFragment()==null||callback.toString().length()>40000)throw new IllegalArgumentException();
  Uri fragment=Uri.parse("https://native.invalid/?"+callback.getEncodedFragment());if(!fragment.getQueryParameterNames().equals(Set.of("state","response"))||fragment.getQueryParameters("state").size()!=1||fragment.getQueryParameters("response").size()!=1)throw new IllegalArgumentException();
  JSONObject pending=new JSONObject(Vault.get(this,HomeConnection.PENDING));if(!pending.getString("state").equals(fragment.getQueryParameter("state")))throw new IllegalArgumentException();JSONObject payload=HomeConnection.decrypt(fragment.getQueryParameter("response"),pending,System.currentTimeMillis());
  HomeConnection.install(this,payload,pending.getLong("generation"),HomeConnection.NETWORK);runOnUiThread(()->{if(!closed){setResult(RESULT_OK);CommandWidget.updateAll(this);startActivity(new Intent(this,AccountActivity.class).putExtra("realm","home").addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));finish();}});
 }catch(Exception e){runOnUiThread(()->fail(e instanceof IllegalStateException?e.getMessage():"This Home return expired or could not be verified. Tap Connect to start again."));}finally{executor.shutdown();}});}
 private void fail(String message){if(!closed){busy=false;status.setText(message);retry.setEnabled(true);}}
 @Override protected void onDestroy(){closed=true;super.onDestroy();}
}

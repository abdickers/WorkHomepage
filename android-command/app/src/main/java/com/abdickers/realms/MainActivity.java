package com.abdickers.realms;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Native entry and setup. Never launches a website as its dashboard. */
public final class MainActivity extends Activity {
 @Override protected void onCreate(Bundle saved){super.onCreate(saved);show();}
 private void show(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,32,24,24);root.setBackgroundColor(0xFF101C2B);setContentView(root);root.setOnApplyWindowInsetsListener((v,i)->{android.graphics.Insets bars=i.getInsets(WindowInsets.Type.systemBars());v.setPadding(24,bars.top+24,24,bars.bottom+24);return i;});
  ImageView shield=new ImageView(this);shield.setImageResource(R.drawable.gondor_shield);root.addView(shield,new LinearLayout.LayoutParams(-1,160));
  TextView title=new TextView(this);title.setText("Middle-earth Command");title.setTextSize(27);title.setGravity(Gravity.CENTER);title.setTextColor(0xFFF4E6BE);root.addView(title);
  TextView intro=new TextView(this);intro.setText("One interactive widget. Home, Work, or Auto.\n\nAdd it to your home screen, then resize it to fill the available page. Switch realms, navigate dates, scroll your feeds and refresh directly inside the widget.\n\nThis native preview needs separate provider connections; website sign-in does not automatically connect them.");intro.setTextSize(16);intro.setTextColor(0xFFE4EAF2);intro.setPadding(0,24,0,24);root.addView(intro);
  Button add=new Button(this);add.setText("Add Command widget");root.addView(add);add.setOnClickListener(v->{AppWidgetManager manager=getSystemService(AppWidgetManager.class);if(manager.isRequestPinAppWidgetSupported()){manager.requestPinAppWidget(new ComponentName(this,CommandWidget.class),null,null);}else new AlertDialog.Builder(this).setMessage("Long-press your home screen → Widgets → Middle-earth Command.").setPositiveButton("OK",null).show();});
  Button settings=new Button(this);settings.setText("Connections & automatic switching");root.addView(settings);settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
 }
}

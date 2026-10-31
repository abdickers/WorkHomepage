package com.abdickers.realms;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

/** Native composition for the same external AI companion used by the website. */
public final class CouncilActivity extends Activity {
 @Override protected void onCreate(Bundle state){super.onCreate(state);int widget=getIntent().getIntExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_ID,0);if(!CommandWidget.owns(this,widget)){finish();return;}String realm=WidgetState.decision(this,widget).realm(),preset=getIntent().getStringExtra("preset");EditText prompt=new EditText(this);prompt.setMinLines(5);prompt.setMaxLines(14);prompt.setText("incident".equals(preset)?"Help me investigate an integration issue. Separate observed facts from assumptions and suggest the next checks.":"plan".equals(preset)?"Help me prioritize my day. Ask me for any details you need.":"");
  LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(20,8,20,8);TextView hint=new TextView(this);hint.setText("Open a draft in your chosen AI app. You can review it before sending; replies stay in that app.");form.addView(hint);form.addView(prompt);Button context=new Button(this);context.setText("Append saved production summary");form.addView(context);context.setOnClickListener(v->{StringBuilder summary=new StringBuilder("\n\nSaved observations — freshness may vary:\n");for(String source:List.of("beacons","systems")){FeedStore.Feed feed=FeedStore.read(this,realm,source);summary.append(Panels.title(source)).append(feed.checkedAt()==0?": not loaded\n":": checked "+new java.util.Date(feed.checkedAt())+"\n");for(FeedStore.Row row:feed.rows().subList(0,Math.min(3,feed.rows().size())))summary.append(row.title()).append(" · ").append(row.body()).append("\n");}prompt.append(summary.toString());context.setEnabled(false);});
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle("The Council").setView(form).setPositiveButton("Choose AI app",null).setNegativeButton("Cancel",(d,w)->finish()).setOnCancelListener(d->finish()).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{String text=prompt.getText().toString().trim();if(text.isEmpty()||text.length()>12000){prompt.setError("Enter a question up to 12,000 characters.");return;}startActivity(Intent.createChooser(new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Open your AI companion"));finish();}));dialog.show();
 }
}

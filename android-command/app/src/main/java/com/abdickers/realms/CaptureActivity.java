package com.abdickers.realms;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

/** Small native editor, launched only from an explicit widget tap. */
public final class CaptureActivity extends Activity {
 @Override protected void onCreate(Bundle state){super.onCreate(state);int widget=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);if(!CommandWidget.owns(this,widget)){finish();return;}String realm=WidgetState.decision(this,widget).realm(),kind=WidgetState.section(this,widget);
  if(getIntent().getBooleanExtra("choose",false)){String[] labels=Panels.ALL.stream().map(Panels::title).toArray(String[]::new);new AlertDialog.Builder(this).setTitle("Choose a panel").setItems(labels,(d,which)->{WidgetState.section(this,widget,Panels.ALL.get(which));CommandWidget.update(this,widget);finish();}).setOnCancelListener(d->finish()).show();return;}
  if(!Panels.local(kind)){finish();return;}LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(24,8,24,8);EditText text=new EditText(this);text.setHint(kind.equals("tools")?"Link name":"What would you like to capture?");text.setMinLines(kind.equals("tools")?1:3);form.addView(text);EditText url=new EditText(this);CheckBox high=new CheckBox(this);high.setText("High priority");if(kind.equals("tools")){url.setHint("https://…");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);form.addView(url);}if(kind.equals("priorities"))form.addView(high);
  AlertDialog dialog=new AlertDialog.Builder(this).setTitle(kind.equals("tools")?"Add link":kind.equals("notes")?"Add field note":"Add priority").setView(form).setPositiveButton("Save",null).setNegativeButton("Cancel",(d,w)->finish()).setOnCancelListener(d->finish()).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{if(kind.equals("tools"))ToolLinks.add(this,realm,text.getText().toString(),url.getText().toString());else LocalBoard.save(this,realm,kind,"",text.getText().toString(),high.isChecked());CommandWidget.updateAll(this);finish();}catch(Exception e){text.setError(e.getMessage());}}));dialog.show();
 }
}

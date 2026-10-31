package com.abdickers.realms;

import android.app.*;
import android.appwidget.AppWidgetManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.*;
import java.util.*;

/** Resolve tapped entries from trusted local state rather than accepting an arbitrary URL or body. */
public final class EntryActivity extends Activity {
 @Override protected void onCreate(Bundle state){super.onCreate(state);int widget=getIntent().getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,-1);String realm=getIntent().getStringExtra("realm"),section=getIntent().getStringExtra("section"),key=getIntent().getStringExtra("key");if(!CommandWidget.owns(this,widget)||!WidgetState.decision(this,widget).realm().equals(realm)||!WidgetState.section(this,widget).equals(section)){finish();return;}
  if(section.equals("overview")&&key!=null&&key.startsWith("panel:")){String target=key.substring(6);if(Panels.forRealm(realm).contains(target)){WidgetState.section(this,widget,target);CommandWidget.update(this,widget);if(ConnectionStatus.needs(this,realm,target))startActivity(ConnectionStatus.intent(this,realm,target));else Refresh.onOpen(this,widget);}finish();return;}
  if("add".equals(key)&&Panels.local(section)){startActivity(new Intent(this,CaptureActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widget));finish();return;}
  if("connect".equals(key)){startActivity(ConnectionStatus.intent(this,realm,section));finish();return;}
  if("refresh".equals(key)){Refresh.request(this,widget);finish();return;}
  if("empty".equals(key)){new AlertDialog.Builder(this).setTitle(Panels.title(section)).setMessage("There are no matching items for the selected view. You can choose another date or refresh this panel.").setPositiveButton("Refresh",(d,w)->{Refresh.request(this,widget);finish();}).setNegativeButton("Close",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
  if("error".equals(key)){String source=section.equals("calendar")?"calendar."+WidgetState.day(this,widget):section;new AlertDialog.Builder(this).setTitle("This panel needs attention").setMessage(FeedStore.read(this,realm,source).error()).setPositiveButton("Accounts",(d,w)->{startActivity(ConnectionStatus.intent(this,realm,section));finish();}).setNegativeButton("Close",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;}
  if(realm.equals("home")&&section.equals("hearth")&&key!=null&&key.startsWith("thermostat:")&&FeedStore.read(this,"home","hearth").rows().stream().anyMatch(row->row.key().equals(key))){startActivity(new Intent(this,ThermostatActivity.class).putExtra("device",key.substring(11)));finish();return;}
  if(section.equals("tools")){ToolLinks.Link link=ToolLinks.all(this,realm).stream().filter(l->l.id().equals(key)).findFirst().orElse(null);if(link!=null)try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(ToolLinks.safe(link.url()))));}catch(Exception e){Toast.makeText(this,"No app can open that link",Toast.LENGTH_LONG).show();}finish();return;}
  if(section.equals("ai")){startActivity(new Intent(this,CouncilActivity.class).putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,widget).putExtra("preset",key));finish();return;}
  if(section.equals("notes")){
   LocalBoard.Entry entry=LocalBoard.all(this,realm,"notes").stream().filter(e->e.id().equals(key)).findFirst().orElse(null);if(entry==null){finish();return;}EditText text=new EditText(this);text.setText(entry.text());text.setMinLines(4);new AlertDialog.Builder(this).setTitle("Field note · on this phone").setView(text).setPositiveButton("Save",(d,w)->{try{LocalBoard.save(this,realm,"notes",entry.id(),text.getText().toString(),false);CommandWidget.updateAll(this);finish();}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();finish();}}).setNeutralButton("Remove",(d,w)->new AlertDialog.Builder(this).setMessage("Remove this note from this phone?").setPositiveButton("Remove",(a,b)->{LocalBoard.remove(this,realm,"notes",entry.id());CommandWidget.updateAll(this);finish();}).setNegativeButton("Keep",(a,b)->finish()).show()).setNegativeButton("Cancel",(d,w)->finish()).setOnCancelListener(d->finish()).show();return;
  }
  String source=section.equals("calendar")?"calendar."+WidgetState.day(this,widget):section;List<FeedStore.Row> rows=FeedStore.read(this,realm,source).rows();int index;try{index=Integer.parseInt(key);}catch(Exception e){finish();return;}if(index<0||index>=rows.size()){finish();return;}FeedStore.Row row=rows.get(index);new AlertDialog.Builder(this).setTitle(row.title()).setMessage(row.body()).setPositiveButton("Close",(d,w)->finish()).setOnCancelListener(d->finish()).show();
 }
}

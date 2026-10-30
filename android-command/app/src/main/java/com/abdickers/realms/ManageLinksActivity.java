package com.abdickers.realms;
import android.app.*;
import android.os.Bundle;
import android.widget.*;
import java.util.List;

/** Native, realm-specific customization; never changes links on either website. */
public final class ManageLinksActivity extends Activity {
 private String realm;
 @Override protected void onCreate(Bundle state){super.onCreate(state);realm=getIntent().getStringExtra("realm");if(!List.of("work","home").contains(realm==null?"":realm)){finish();return;}showLinks();}
 private void showLinks(){List<ToolLinks.Link> links=ToolLinks.all(this,realm);String[] names=links.stream().map(ToolLinks.Link::title).toArray(String[]::new);new AlertDialog.Builder(this).setTitle((realm.equals("work")?"Work":"Home")+" tools · on this phone").setItems(names,(d,index)->edit(links.get(index))).setNeutralButton("Restore hidden",(d,w)->{try{ToolLinks.restoreHidden(this,realm);CommandWidget.updateAll(this);}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}showLinks();}).setPositiveButton("Done",(d,w)->finish()).setOnCancelListener(d->finish()).show();}
 private void edit(ToolLinks.Link link){LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(24,8,24,8);EditText title=new EditText(this),url=new EditText(this);title.setHint("Link name");title.setText(link.title());url.setHint("https://…");url.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_VARIATION_URI);url.setText(link.url());form.addView(title);form.addView(url);AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Edit tool").setView(form).setPositiveButton("Save",null).setNeutralButton("Hide",(d,w)->{try{ToolLinks.edit(this,realm,link.id(),link.title(),link.url(),true);CommandWidget.updateAll(this);}catch(Exception e){Toast.makeText(this,e.getMessage(),Toast.LENGTH_LONG).show();}showLinks();}).setNegativeButton("Back",(d,w)->showLinks()).setOnCancelListener(d->showLinks()).create();dialog.setOnShowListener(d->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{try{ToolLinks.edit(this,realm,link.id(),title.getText().toString(),url.getText().toString(),false);CommandWidget.updateAll(this);dialog.dismiss();showLinks();}catch(Exception e){url.setError(e.getMessage());}}));dialog.show();}
}

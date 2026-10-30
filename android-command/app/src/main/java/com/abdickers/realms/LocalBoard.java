package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.util.*;

/** Editable phone-local priorities and field notes; never presented as server-synced data. */
final class LocalBoard {
 record Entry(String id,String text,boolean done,boolean high,long created){}
 private static String key(String realm,String kind){if(!Set.of("home","work").contains(realm)||!Set.of("notes","priorities").contains(kind))throw new IllegalArgumentException("Unknown board");return realm+"."+kind;}
 static synchronized List<Entry> all(Context c,String realm,String kind){try{JSONArray a=new JSONArray(c.getSharedPreferences("boards",0).getString(key(realm,kind),"[]"));List<Entry> result=new ArrayList<>();for(int i=0;i<Math.min(a.length(),200);i++){JSONObject e=a.getJSONObject(i);result.add(new Entry(e.getString("id"),e.getString("text"),e.optBoolean("done"),e.optBoolean("high"),e.optLong("created")));}return result;}catch(JSONException e){throw new IllegalStateException("Saved board could not be read. It has not been overwritten.");}}
 static synchronized void save(Context c,String realm,String kind,String id,String text,boolean high){
  text=text.trim();if(text.isEmpty()||text.length()>4000)throw new IllegalArgumentException("Enter between 1 and 4,000 characters.");List<Entry> entries=all(c,realm,kind);Entry previous=entries.stream().filter(e->e.id.equals(id)).findFirst().orElse(null);if(previous==null&&entries.size()>=200)throw new IllegalArgumentException("This board already contains 200 items. Remove an older item first.");
  entries.removeIf(e->e.id.equals(id));entries.add(0,new Entry(previous==null?UUID.randomUUID().toString():id,text,previous!=null&&previous.done,high,previous==null?System.currentTimeMillis():previous.created));write(c,realm,kind,entries);
 }
 static synchronized void toggle(Context c,String realm,String id){List<Entry> entries=all(c,realm,"priorities");for(int i=0;i<entries.size();i++){Entry e=entries.get(i);if(e.id.equals(id)){entries.set(i,new Entry(e.id,e.text,!e.done,e.high,e.created));write(c,realm,"priorities",entries);return;}}}
 static synchronized void remove(Context c,String realm,String kind,String id){List<Entry> entries=all(c,realm,kind);entries.removeIf(e->e.id.equals(id));write(c,realm,kind,entries);}
 private static void write(Context c,String realm,String kind,List<Entry> entries){try{JSONArray a=new JSONArray();for(Entry e:entries)a.put(new JSONObject().put("id",e.id).put("text",e.text).put("done",e.done).put("high",e.high).put("created",e.created));if(!c.getSharedPreferences("boards",0).edit().putString(key(realm,kind),a.toString()).commit())throw new IllegalStateException("The board could not be saved.");}catch(JSONException e){throw new IllegalStateException("The board could not be saved.");}}
 static List<FeedStore.Row> rows(Context c,String realm,String kind){List<Entry> entries=new ArrayList<>(all(c,realm,kind));if(kind.equals("priorities"))entries.sort(Comparator.comparing(Entry::done).thenComparing(e->!e.high));List<FeedStore.Row> rows=new ArrayList<>();for(Entry e:entries){rows.add(new FeedStore.Row((e.done?"✓ ":e.high?"★ ":"")+e.text,(kind.equals("priorities")?(e.done?"Done · tap to reopen":"Tap to mark done"):"Tap to edit")+" · saved on this phone",kind.equals("priorities")?R.drawable.icon_quest:R.drawable.icon_notes,e.id));}return rows;}
 private LocalBoard(){}
}

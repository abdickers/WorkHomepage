package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.util.*;

final class FeedStore {
 record Row(String title,String body,int icon,String key){Row(String title,String body,int icon){this(title,body,icon,"");}}
 record Feed(List<Row> rows,long checkedAt,String error){}
 static String key(String realm,String source){return realm+"."+source;}
 static Feed read(Context c,String realm,String source){
  try{JSONObject o=new JSONObject(c.getSharedPreferences("feeds",0).getString(key(realm,source),"{}"));JSONArray a=o.optJSONArray("rows");List<Row> rows=new ArrayList<>();if(a!=null)for(int i=0;i<Math.min(a.length(),20);i++){JSONObject row=a.getJSONObject(i);rows.add(new Row(row.optString("title"),row.optString("body"),row.optInt("icon",R.drawable.gondor_shield)));}return new Feed(List.copyOf(rows),o.optLong("checkedAt"),o.optString("error"));}catch(JSONException e){return new Feed(List.of(),0,"Saved feed could not be read. Refresh to recover.");}
 }
 static void success(Context c,String realm,String source,List<Row> rows){write(c,realm,source,new Feed(rows,System.currentTimeMillis(),""));}
 static void failure(Context c,String realm,String source,String message){Feed old=read(c,realm,source);write(c,realm,source,new Feed(old.rows,old.checkedAt,message));}
 private static void write(Context c,String realm,String source,Feed f){try{JSONArray rows=new JSONArray();for(Row r:f.rows.subList(0,Math.min(20,f.rows.size())))rows.put(new JSONObject().put("title",r.title.substring(0,Math.min(200,r.title.length()))).put("body",r.body.substring(0,Math.min(1200,r.body.length()))).put("icon",r.icon));JSONObject o=new JSONObject().put("rows",rows).put("checkedAt",f.checkedAt).put("error",f.error);c.getSharedPreferences("feeds",0).edit().putString(key(realm,source),o.toString()).commit();}catch(JSONException e){throw new IllegalArgumentException(e);}}
 static void clear(Context c){c.getSharedPreferences("feeds",0).edit().clear().commit();}
}

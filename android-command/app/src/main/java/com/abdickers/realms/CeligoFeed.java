package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.util.*;

final class CeligoFeed {
 static final String INTEGRATION="6a60d5774b038e24aba608b7";
 static List<FeedStore.Row> read(Context c)throws Exception {
  String token=Vault.get(c,"celigo");if(token.isBlank())throw new IllegalStateException("Connect your Celigo production token in Settings.");
  JSONObject response=BoundedHttp.get("https://api.integrator.io/v1/flows?_integrationId="+INTEGRATION+"&limit=50&include=name,disabled,_integrationId,lastExecutedAt,numOpenError&includeErrorCounts=true","Bearer "+token);
  JSONArray flows=response.optJSONArray("items");if(flows==null)flows=response.optJSONArray("data");if(flows==null)throw new IllegalStateException("Celigo returned an unfamiliar flow list. Previous data retained.");
  return parse(flows);
 }
 static List<FeedStore.Row> parse(JSONArray flows)throws Exception {
  long errors=0;int known=0,disabled=0,n=Math.min(flows.length(),50);List<FeedStore.Row> rows=new ArrayList<>();
  for(int i=0;i<n;i++){
   JSONObject flow=flows.getJSONObject(i);if(!INTEGRATION.equals(flow.optString("_integrationId")))throw new IllegalStateException("Celigo did not confirm the requested production integration. Data was not saved.");
   Object value=flow.opt("numOpenError");boolean valid=value instanceof Number&&((Number)value).doubleValue()>=0&&((Number)value).doubleValue()==((Number)value).intValue();int count=valid?((Number)value).intValue():0;if(valid){known++;errors+=count;}boolean paused=flow.optBoolean("disabled");if(paused)disabled++;
   if(rows.size()<19)rows.add(new FeedStore.Row(flow.optString("name","Unnamed flow"),(paused?"Paused":"Enabled")+" · Open errors: "+(valid?count:"not reported")+"\nLast execution: "+flow.optString("lastExecutedAt","not reported"),valid&&count>0?R.drawable.beacon_error:R.drawable.beacon_unlit));
  }
  rows.add(0,new FeedStore.Row("Beacons of Production",n+" flows inspected · "+disabled+" paused\nReported open errors: "+(known==0?"unknown":errors)+" · counts available for "+known+"/"+n+" flows"+(flows.length()>=50?"\nOne bounded page; additional flows may exist.":""),errors>0?R.drawable.beacon_error:R.drawable.beacon_unlit));return rows;
 }
 private CeligoFeed(){}
}

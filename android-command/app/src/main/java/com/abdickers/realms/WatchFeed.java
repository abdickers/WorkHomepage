package com.abdickers.realms;
import org.json.JSONObject;
import java.util.*;

/** Current public service health only: six small reads, no history or security-catalog scans. */
final class WatchFeed {
 interface Reader {JSONObject get(String url,String authorization)throws Exception;}
 static List<FeedStore.Row> read()throws Exception{return read(BoundedHttp::get);}
 static List<FeedStore.Row> read(Reader reader)throws Exception {
  String[] names={"Cloudflare","Vercel","BigCommerce","NetSuite","Celigo","GitHub"};
  String[] hosts={"www.cloudflarestatus.com","www.vercel-status.com","status.bigcommerce.com","status.netsuite.com","status.celigo.com","www.githubstatus.com"};
  int[] icons={R.drawable.brand_cloudflare,R.drawable.brand_vercel,R.drawable.brand_bigcommerce,R.drawable.brand_netsuite,R.drawable.brand_celigo,R.drawable.brand_github};List<FeedStore.Row> rows=new ArrayList<>();
  for(int i=0;i<hosts.length;i++)try{JSONObject data=reader.get("https://"+hosts[i]+"/api/v2/status.json",""),status=data.getJSONObject("status");String indicator=status.getString("indicator");if(!Set.of("none","minor","major","critical","maintenance").contains(indicator))throw new IllegalStateException();rows.add(new FeedStore.Row(names[i]+" · "+status.getString("description"),"Provider-reported current status · "+indicator+"\n"+hosts[i],icons[i]));}
  catch(BoundedHttp.RateLimitException e){throw e;}
  catch(Exception e){rows.add(new FeedStore.Row(names[i]+" · status unavailable","Could not read this provider. Its health is unknown; other status results may still be available.",icons[i]));}
  return rows;
 }
 private WatchFeed(){}
}

package com.abdickers.realms;

import android.content.Context;
import android.net.Uri;
import net.openid.appauth.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

/** A phone session for the read-only Work API. Provider secrets never reach the phone. */
final class WorkSession {
 static final String ORIGIN="https://76mcp-api-gateway.vercel.app",ISSUER=ORIGIN+"/api/auth",RESOURCE=ORIGIN+"/mcp";
 static final String SCOPE="offline_access command:read",KEY="work_oauth",OWNER="dickersona@76fireworks.com";
 static final Uri REDIRECT=Uri.parse("com.abdickers.realms:/oauth/work");
 private static final Object REFRESH_LOCK=new Object();
 static AuthorizationServiceConfiguration configuration(){return new AuthorizationServiceConfiguration(Uri.parse(ISSUER+"/oauth2/authorize"),Uri.parse(ISSUER+"/oauth2/token"),Uri.parse(ISSUER+"/oauth2/register"));}
 static boolean connected(Context c){return WidgetState.prefs(c).getBoolean("work_session_connected",false)&&c.getSharedPreferences("vault",0).contains(KEY);}
 static boolean sourceReady(Context c,String source){return connected(c)&&WidgetState.prefs(c).getBoolean("work_service_"+source,false);}
 static JSONObject verify(String token)throws Exception{
  JSONObject metadata=BoundedHttp.get(ORIGIN+"/api/command/connections","Bearer "+token);
  if(!OWNER.equalsIgnoreCase(metadata.optString("email")))throw new IllegalStateException("Choose your Work Google account to continue.");
  if(metadata.optJSONArray("connections")==null)throw new IllegalStateException("Work connections are not available yet.");
  return metadata;
 }
 static void save(Context c,AuthState state,JSONObject metadata,long generation)throws Exception{
  synchronized(Connections.LOCK){if(Connections.generation(c)!=generation)throw new IllegalStateException("Connections changed. Please sign in again.");
   Vault.put(c,KEY,state.jsonSerializeString());android.content.SharedPreferences.Editor edit=WidgetState.prefs(c).edit().putBoolean("work_session_connected",true);
   for(String source:List.of("jira","slack","beacons","systems"))edit.putBoolean("work_service_"+source,false);
   JSONArray connections=metadata.getJSONArray("connections");for(int i=0;i<Math.min(4,connections.length());i++){JSONObject row=connections.getJSONObject(i);String source=row.optString("source");if(Set.of("jira","slack","beacons","systems").contains(source))edit.putBoolean("work_service_"+source,row.optBoolean("connected"));}if(!edit.commit())throw new IllegalStateException("Unable to save Work connection.");
  }
 }
 /** Called only by bounded background feed jobs, never the UI thread. Serialize refresh rotation. */
 static String token(Context c)throws Exception{
  synchronized(REFRESH_LOCK){long generation=Connections.generation(c);String saved=Vault.get(c,KEY);if(!connected(c)||saved.isEmpty())throw new IllegalStateException("Sign in to Work to load this panel.");AuthState state=AuthState.jsonDeserialize(saved);
   AuthorizationServiceConfiguration config=state.getAuthorizationServiceConfiguration();if(config==null||!configuration().tokenEndpoint.equals(config.tokenEndpoint)||!configuration().authorizationEndpoint.equals(config.authorizationEndpoint))throw new IllegalStateException("Reconnect your Work account.");
   AuthorizationService service=new AuthorizationService(c.getApplicationContext());CountDownLatch done=new CountDownLatch(1);String[] result=new String[1];boolean[] valid={true};
   try{state.performActionWithFreshTokens(service,NoClientAuthentication.INSTANCE,Map.of("resource",RESOURCE),(access,id,error)->{synchronized(done){if(valid[0]){try{if(error==null&&access!=null){synchronized(Connections.LOCK){if(generation==Connections.generation(c)&&connected(c)){Vault.put(c,KEY,state.jsonSerializeString());result[0]=access;}}}}catch(Exception ignored){}done.countDown();}}});
    if(!done.await(20,TimeUnit.SECONDS)){synchronized(done){valid[0]=false;}throw new IllegalStateException("Work sign-in timed out. Try again.");}if(result[0]==null)throw new IllegalStateException("Work sign-in expired. Sign in again.");return result[0];
   }finally{service.dispose();}
  }
 }
 static List<FeedStore.Row> read(Context c,String source)throws Exception{
  if(!Set.of("jira","slack","beacons","systems").contains(source))throw new IllegalArgumentException("Unsupported Work panel");
  if(!sourceReady(c,source))throw new IllegalStateException("This service is still being connected. Your Work sign-in is saved.");
  JSONObject data=BoundedHttp.get(ORIGIN+"/api/command/"+source,"Bearer "+token(c));JSONArray values=data.getJSONArray("rows");List<FeedStore.Row> rows=new ArrayList<>();int icon=switch(source){case "jira"->R.drawable.icon_quest;case "slack"->R.drawable.icon_notes;default->R.drawable.icon_beacon;};
  for(int i=0;i<Math.min(20,values.length());i++){JSONObject row=values.getJSONObject(i);rows.add(new FeedStore.Row(row.getString("title"),row.getString("body"),icon));}return rows;
 }
 private WorkSession(){}
}

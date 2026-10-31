package com.abdickers.realms;

import android.content.Context;
import org.json.*;
import java.net.URI;
import java.util.*;

/** Public native Slack OAuth. Only the signed-in user's read scopes; never a bot or server key. */
final class SlackConnection {
 static final String KEY="work_slack_session",REDIRECT="com.abdickers.realms:/oauth/slack";
 static final String SCOPES="search:read,users:read,users:read.email";
 private static final Set<String> READ_SCOPES=Set.of("search:read","users:read","users:read.email");
 private static final Object TOKEN_LOCK=new Object();
 interface Transport{JSONObject get(String url,String authorization)throws Exception;JSONObject post(Map<String,String> fields)throws Exception;}
 static final Transport NETWORK=new Transport(){public JSONObject get(String url,String auth)throws Exception{return BoundedHttp.get(url,auth);}public JSONObject post(Map<String,String> fields)throws Exception{return BoundedHttp.slackOAuth(fields);}};
 static boolean connected(Context c){return c.getSharedPreferences("vault",0).contains(KEY);}
 static JSONObject exchange(String code,String verifier,String client,Transport transport,long now)throws Exception{
  if(code==null||code.isBlank()||verifier==null||!verifier.matches("[A-Za-z0-9._~-]{43,128}")||!client.matches("[0-9]+\\.[0-9]+"))throw new IllegalArgumentException("Invalid Slack sign-in");
  JSONObject response=transport.post(Map.of("client_id",client,"grant_type","authorization_code","code",code,"code_verifier",verifier,"redirect_uri",REDIRECT));
  JSONObject session=parse(response,null,now);verify(session,transport);return session;
 }
 static JSONObject parse(JSONObject response,JSONObject previous,long now)throws Exception{
  if(!response.optBoolean("ok")||response.optBoolean("is_enterprise_install"))throw new IllegalStateException("Choose the Spirit of 76 Fireworks workspace.");JSONObject user=response.optJSONObject("authed_user");if(user==null)user=response;
  String access=user.optString("access_token"),refresh=user.optString("refresh_token"),type=user.optString("token_type");long expires=user.optLong("expires_in",0);String scope=user.optString("scope",previous==null?"":previous.getString("scope"));Set<String> granted=new HashSet<>(Arrays.asList(scope.split(",")));
  if(!type.equals("user")||!granted.equals(READ_SCOPES)||access.isBlank()||refresh.isBlank()||access.length()>4096||refresh.length()>4096||access.matches("(?s).*\\s.*")||refresh.matches("(?s).*\\s.*")||expires<60||expires>86400)throw new IllegalStateException("Slack did not return the expected read-only connection. Please reconnect.");
  return new JSONObject().put("access",access).put("refresh",refresh).put("scope",scope).put("expires",now+expires*1000).put("issued",now).put("user",previous==null?"":previous.optString("user")).put("team",previous==null?"":previous.optString("team"));
 }
 static void verify(JSONObject session,Transport transport)throws Exception{
  String authorization="Bearer "+session.getString("access");JSONObject auth=transport.get("https://slack.com/api/auth.test",authorization);String user=auth.optString("user_id"),team=auth.optString("team_id");URI workspace=URI.create(auth.optString("url"));
  if(!auth.optBoolean("ok")||!user.matches("[UW][A-Z0-9]{3,40}")||!team.matches("T[A-Z0-9]{3,40}")||!"https".equals(workspace.getScheme())||!"76fireworks.slack.com".equalsIgnoreCase(workspace.getHost())||workspace.getUserInfo()!=null||workspace.getPort()!=-1||(!session.optString("user").isEmpty()&&!user.equals(session.getString("user")))||(!session.optString("team").isEmpty()&&!team.equals(session.getString("team"))))throw new IllegalStateException("Choose the Spirit of 76 Fireworks Slack workspace.");
  JSONObject profile=transport.get("https://slack.com/api/users.info?user="+user,authorization);JSONObject info=profile.optJSONObject("user"),details=info==null?null:info.optJSONObject("profile");
  if(!profile.optBoolean("ok")||info==null||info.optBoolean("deleted")||details==null||!GoogleConnection.expected("work").equalsIgnoreCase(details.optString("email")))throw new IllegalStateException("Choose your dickersona@76fireworks.com Slack account.");session.put("user",user).put("team",team);
 }
 static void install(Context c,JSONObject session,long generation)throws Exception{
  synchronized(Connections.LOCK){if(generation!=Connections.generation(c))throw new IllegalStateException("Your connections changed. Please connect Slack again.");Vault.put(c,KEY,session.toString());Connections.invalidate(c,"work");WidgetState.prefs(c).edit().putLong("slack_connection_changed",System.currentTimeMillis()).commit();}
 }
 /** Runs in feed jobs only; refresh is serialized and never replays an uncertain rotating token. */
 static String token(Context c)throws Exception{
  synchronized(TOKEN_LOCK){long generation=Connections.generation(c);JSONObject saved=new JSONObject(Vault.get(c,KEY));long now=System.currentTimeMillis();if(saved.optBoolean("refreshing")||saved.optLong("issued")>now+300000)throw new IllegalStateException("Please reconnect Slack in Accounts.");if(saved.getLong("expires")>now+60000)return saved.getString("access");
   synchronized(Connections.LOCK){if(generation!=Connections.generation(c))throw new IllegalStateException("Please reconnect Slack.");saved.put("refreshing",true);Vault.put(c,KEY,saved.toString());}
   JSONObject result=NETWORK.post(Map.of("client_id",BuildConfig.SLACK_CLIENT_ID,"grant_type","refresh_token","refresh_token",saved.getString("refresh")));JSONObject next=parse(result,saved,now);verify(next,NETWORK);
   synchronized(Connections.LOCK){if(generation!=Connections.generation(c))throw new IllegalStateException("Slack was disconnected during refresh.");Vault.put(c,KEY,next.toString());}return next.getString("access");
  }
 }
 private SlackConnection(){}
}

package com.abdickers.realms;

import android.content.Context;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

/** Synthetic responses only. These tests never grant or read an actual Slack account. */
public class SlackConnectionTest {
 Context context(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){Connections.invalidate(context());Vault.clear(context());WidgetState.prefs(context()).edit().clear().putBoolean("welcome_seen",true).commit();}
 JSONObject reply()throws Exception{return new JSONObject().put("ok",true).put("authed_user",new JSONObject().put("token_type","user").put("access_token","SYNTHETIC-ACCESS").put("refresh_token","SYNTHETIC-REFRESH").put("scope",SlackConnection.SCOPES).put("expires_in",43200));}
 SlackConnection.Transport transport(String workspace,String email,List<Map<String,String>> calls){return new SlackConnection.Transport(){public JSONObject post(Map<String,String> fields)throws Exception{calls.add(fields);return reply();}public JSONObject get(String url,String auth)throws Exception{assertEquals("Bearer SYNTHETIC-ACCESS",auth);if(url.endsWith("auth.test"))return new JSONObject().put("ok",true).put("url",workspace).put("user_id","UTEST1").put("team_id","TTEST1");assertEquals("https://slack.com/api/users.info?user=UTEST1",url);return new JSONObject().put("ok",true).put("user",new JSONObject().put("profile",new JSONObject().put("email",email)));}};}
 @Test public void nativeExchangeUsesOnlyPublicPkceAndVerifiesWorkIdentity()throws Exception{
  List<Map<String,String>> calls=new ArrayList<>();JSONObject session=SlackConnection.exchange("SYNTHETIC-CODE","a".repeat(43),"123.456",transport("https://76fireworks.slack.com/","dickersona@76fireworks.com",calls),1000);
  assertEquals(1,calls.size());assertFalse(calls.get(0).containsKey("client_secret"));assertEquals("a".repeat(43),calls.get(0).get("code_verifier"));assertEquals(SlackConnection.REDIRECT,calls.get(0).get("redirect_uri"));assertEquals("UTEST1",session.getString("user"));assertEquals("TTEST1",session.getString("team"));assertEquals(43201000,session.getLong("expires"));
  long generation=Connections.generation(context());SlackConnection.install(context(),session,generation);assertTrue(SlackConnection.connected(context()));assertFalse(ConnectionStatus.needs(context(),"work","slack"));assertTrue(ConnectionStatus.needs(context(),"home","slack"));assertFalse(context().getSharedPreferences("vault",0).getString(SlackConnection.KEY,"").contains("SYNTHETIC-ACCESS"));
 }
 @Test public void wrongWorkspaceOrAccountAndExtraWriteScopesCannotBeInstalled()throws Exception{
  for(String workspace:List.of("https://other.slack.com/","http://76fireworks.slack.com/","https://76fireworks.slack.com.attacker.invalid/")){try{SlackConnection.exchange("CODE","a".repeat(43),"123.456",transport(workspace,"dickersona@76fireworks.com",new ArrayList<>()),1000);fail("Wrong workspace must fail");}catch(IllegalStateException expected){}}
  try{SlackConnection.exchange("CODE","a".repeat(43),"123.456",transport("https://76fireworks.slack.com/","abdickers@gmail.com",new ArrayList<>()),1000);fail("Home identity must fail");}catch(IllegalStateException expected){}
  JSONObject response=reply();response.getJSONObject("authed_user").put("scope",SlackConnection.SCOPES+",chat:write");try{SlackConnection.parse(response,null,1000);fail("Unexpected write scope must fail");}catch(IllegalStateException expected){}
  response=reply();response.getJSONObject("authed_user").put("token_type","bot");try{SlackConnection.parse(response,null,1000);fail("Bot token must fail");}catch(IllegalStateException expected){}
  assertFalse(SlackConnection.connected(context()));
 }
 @Test public void disconnectGenerationBlocksLateCompletionAndUncertainRefreshDoesNotReplay()throws Exception{
  long old=Connections.generation(context());Connections.invalidate(context());try{SlackConnection.install(context(),new JSONObject(),old);fail("Late sign-in must fail");}catch(IllegalStateException expected){}
  assertFalse(SlackConnection.connected(context()));Vault.put(context(),SlackConnection.KEY,new JSONObject().put("refreshing",true).toString());try{SlackConnection.token(context());fail("Uncertain rotation must require reconnect before any network");}catch(IllegalStateException expected){assertTrue(expected.getMessage().contains("reconnect"));}
 }
}

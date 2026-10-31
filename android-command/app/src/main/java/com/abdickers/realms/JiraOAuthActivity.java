package com.abdickers.realms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import net.openid.appauth.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.Executors;

/** Native Jira consent against Atlassian's own service. No customer server changes. */
public final class JiraOAuthActivity extends Activity {
 private static final int AUTHORIZE=8231;private AuthorizationService service;private TextView status;private long generation;private boolean closed,busy;private Button retry;
 @Override protected void onCreate(Bundle state){super.onCreate(state);generation=state==null?Connections.generation(this):state.getLong("generation");LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,70,24,24);root.setBackgroundColor(0xFF171D23);status=new TextView(this);status.setTextColor(0xFFE8EDF4);status.setTextSize(19);status.setText("Connect Jira · 76 Fireworks");root.addView(status);retry=new Button(this);retry.setText("Try Jira sign-in again");retry.setOnClickListener(v->begin());root.addView(retry);Button back=new Button(this);back.setText("Back to accounts");back.setOnClickListener(v->finish());root.addView(back);setContentView(root);service=new AuthorizationService(this);if(state==null)begin();else fail("If Jira sign-in was interrupted, tap Try Jira sign-in again.");}
 private void begin(){
  if(busy)return;
  busy=true;retry.setEnabled(false);
  try{
   if(!WidgetState.prefs(this).getBoolean("work_google_connected",false)){fail("Sign in to Work with Google first, then connect Jira.");return;}
   generation=Connections.generation(this);
   String client=Vault.get(this,"jira_client_id");
   if(!client.isEmpty()){authorize(client);return;}
   status.setText("Opening Atlassian sign-in…");
   var executor=Executors.newSingleThreadExecutor();
   executor.execute(()->{
    try{
     String registered=JiraRegistration.register();
     if(closed)return;
     Vault.put(this,"jira_client_id",registered);
     runOnUiThread(()->{if(!closed)try{authorize(registered);}catch(Exception e){fail("Jira sign-in could not open. Please try again.");}});
    }catch(Exception e){runOnUiThread(()->fail(e instanceof IllegalStateException?e.getMessage():"Atlassian could not be reached. Please try again."));}
    finally{executor.shutdown();}
   });
  }catch(Exception e){fail("Jira sign-in could not open. Please try again.");}
 }
 private void authorize(String client)throws Exception{AuthorizationRequest request=new AuthorizationRequest.Builder(JiraConnection.configuration(),client,ResponseTypeValues.CODE,JiraConnection.REDIRECT).setScope(JiraConnection.SCOPE).setLoginHint(GoogleConnection.expected("work")).setAdditionalParameters(Map.of("resource",JiraMcp.RESOURCE)).build();Vault.put(this,"jira_pending",new JSONObject().put("request",request.jsonSerialize()).put("time",System.currentTimeMillis()).toString());status.setText("Finish connecting Jira in the browser using your Work Google account.");startActivityForResult(service.getAuthorizationRequestIntent(request),AUTHORIZE);}
 @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(request!=AUTHORIZE)return;try{String pending=Vault.get(this,"jira_pending");Vault.put(this,"jira_pending","");AuthorizationResponse response=intent==null?null:AuthorizationResponse.fromIntent(intent);AuthorizationException error=intent==null?null:AuthorizationException.fromIntent(intent);if(result!=RESULT_OK||response==null||error!=null){fail(OAuthFeedback.failure("Jira",error));return;}JSONObject record=new JSONObject(pending);AuthorizationRequest original=AuthorizationRequest.jsonDeserialize(record.getJSONObject("request"));long age=System.currentTimeMillis()-record.getLong("time");if(age<0||age>1800000||!Objects.equals(original.state,response.request.state)||!Objects.equals(original.codeVerifier,response.request.codeVerifier)||!Objects.equals(original.clientId,response.request.clientId)||!JiraConnection.REDIRECT.equals(response.request.redirectUri)||!JiraConnection.configuration().tokenEndpoint.equals(response.request.configuration.tokenEndpoint)||!JiraConnection.SCOPE.equals(response.request.scope))throw new IllegalStateException();String issuer=response.additionalParameters.get("iss");if(issuer!=null&&!JiraConnection.ISSUER.equals(issuer))throw new IllegalStateException();AuthState state=new AuthState(response,null);status.setText("Checking your Jira account and site…");service.performTokenRequest(response.createTokenExchangeRequest(Map.of("resource",JiraMcp.RESOURCE)),NoClientAuthentication.INSTANCE,(tokens,problem)->{if(closed)return;if(tokens==null||problem!=null||tokens.accessToken==null){fail("Jira sign-in could not finish. Please try again.");return;}state.update(tokens,null);var executor=Executors.newSingleThreadExecutor();executor.execute(()->{try{JiraMcp api=new JiraMcp(tokens.accessToken);JiraMcp.verify(api::call);if(closed)return;JiraConnection.install(this,state,generation);runOnUiThread(()->{if(!closed){setResult(RESULT_OK);CommandWidget.updateAll(this);finish();}});}catch(Exception e){runOnUiThread(()->fail(e instanceof IllegalStateException?e.getMessage():"Jira could not be connected. Your other connections are unchanged."));}finally{executor.shutdown();}});});}catch(Exception e){fail("This Jira sign-in expired or could not be verified. Please try again.");}}
 private void fail(String message){busy=false;if(!closed){status.setText(message);retry.setEnabled(true);}}
 @Override protected void onSaveInstanceState(Bundle out){out.putLong("generation",generation);super.onSaveInstanceState(out);}
 @Override protected void onDestroy(){closed=true;if(service!=null)service.dispose();super.onDestroy();}
}

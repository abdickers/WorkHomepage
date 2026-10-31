package com.abdickers.realms;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.*;
import net.openid.appauth.*;
import org.json.JSONObject;
import java.util.*;
import java.util.concurrent.Executors;

/** Browser OAuth with PKCE; the requesting app cannot choose an arbitrary server or account. */
public final class WorkSignInActivity extends Activity {
 private static final int AUTHORIZE=8301;
 private AuthorizationService service;
 private TextView status;
 private long generation;
 private boolean closed;
 @Override protected void onCreate(Bundle saved){super.onCreate(saved);generation=saved==null?Connections.generation(this):saved.getLong("generation");LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,80,32,32);root.setBackgroundColor(0xFF171D23);status=new TextView(this);status.setTextColor(0xFFE8EDF4);status.setTextSize(20);status.setText("Opening Work sign-in…");root.addView(status);Button cancel=new Button(this);cancel.setText("Back to accounts");cancel.setOnClickListener(v->finish());root.addView(cancel);setContentView(root);service=new AuthorizationService(this);if(saved==null)begin();}
 private void begin(){try{String client=Vault.get(this,"work_client_id");if(!client.isEmpty()){authorize(client);return;}
  RegistrationRequest request=new RegistrationRequest.Builder(WorkSession.configuration(),List.of(WorkSession.REDIRECT)).setGrantTypeValues("authorization_code","refresh_token").setResponseTypeValues("code").setTokenEndpointAuthenticationMethod("none").setAdditionalParameters(Map.of("client_name","Middle-earth Command Android","scope",WorkSession.SCOPE)).build();
  service.performRegistrationRequest(request,(response,error)->{if(closed)return;if(error!=null||response==null||response.clientSecret!=null){fail("Work sign-in is not ready yet. Your Google connection is saved; you can safely go back.");return;}try{Vault.put(this,"work_client_id",response.clientId);authorize(response.clientId);}catch(Exception e){fail("Unable to save Work sign-in. Please try again.");}});
 }catch(Exception e){fail("Could not open Work sign-in. Check your connection and try again.");}}
 private void authorize(String client)throws Exception{
  AuthorizationRequest request=new AuthorizationRequest.Builder(WorkSession.configuration(),client,ResponseTypeValues.CODE,WorkSession.REDIRECT).setScope(WorkSession.SCOPE).setLoginHint(WorkSession.OWNER).setAdditionalParameters(Map.of("resource",WorkSession.RESOURCE)).build();
  Vault.put(this,"work_pending_oauth",new JSONObject().put("request",request.jsonSerialize()).put("time",System.currentTimeMillis()).toString());
  startActivityForResult(service.getAuthorizationRequestIntent(request),AUTHORIZE);
 }
 @Override protected void onActivityResult(int request,int result,Intent intent){super.onActivityResult(request,result,intent);if(request!=AUTHORIZE)return;
  try{String pending=Vault.get(this,"work_pending_oauth");Vault.put(this,"work_pending_oauth","");AuthorizationResponse response=intent==null?null:AuthorizationResponse.fromIntent(intent);AuthorizationException error=intent==null?null:AuthorizationException.fromIntent(intent);
   if(result!=RESULT_OK||response==null||error!=null){fail("Work sign-in was canceled or declined. Tap Back to accounts to try again.");return;}
   JSONObject record=new JSONObject(pending);AuthorizationRequest original=AuthorizationRequest.jsonDeserialize(record.getJSONObject("request"));long age=System.currentTimeMillis()-record.getLong("time");
   if(age<0||age>30*60*1000||!Objects.equals(original.state,response.request.state)||!Objects.equals(original.codeVerifier,response.request.codeVerifier)||!WorkSession.REDIRECT.equals(response.request.redirectUri)||!Objects.equals(original.clientId,response.request.clientId)||!WorkSession.configuration().tokenEndpoint.equals(response.request.configuration.tokenEndpoint))throw new IllegalStateException();
   String issuer=response.additionalParameters.get("iss");if(issuer!=null&&!WorkSession.ISSUER.equals(issuer))throw new IllegalStateException();
   status.setText("Finishing Work sign-in…");AuthState state=new AuthState(response,null);
   service.performTokenRequest(response.createTokenExchangeRequest(Map.of("resource",WorkSession.RESOURCE)),NoClientAuthentication.INSTANCE,(tokens,problem)->{
    if(closed)return;if(tokens==null||problem!=null||tokens.accessToken==null){fail("Work sign-in could not finish. Go back and try again.");return;}state.update(tokens,null);
    var executor=Executors.newSingleThreadExecutor();executor.execute(()->{try{JSONObject metadata=WorkSession.verify(tokens.accessToken);if(closed)return;WorkSession.save(this,state,metadata,generation);runOnUiThread(()->{if(!closed){CommandWidget.updateAll(this);setResult(RESULT_OK);finish();}});}catch(Exception e){runOnUiThread(()->fail("Your Work services could not be connected yet. Google email and calendar remain connected."));}finally{executor.shutdown();}});
   });
  }catch(Exception e){fail("Work sign-in expired or could not be verified. Go back and try again.");}
 }
 private void fail(String message){if(!closed)status.setText(message);}
 @Override protected void onSaveInstanceState(Bundle out){out.putLong("generation",generation);super.onSaveInstanceState(out);}
 @Override protected void onDestroy(){closed=true;if(service!=null)service.dispose();super.onDestroy();}
}

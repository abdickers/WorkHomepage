package com.abdickers.realms;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import com.google.android.gms.auth.api.identity.*;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Google owns the token cache. Native consent is account-pinned and never reads browser cookies. */
final class GoogleConnection {
 static final int REQUEST_MAIL=711;
 static final String GMAIL_READ="https://www.googleapis.com/auth/gmail.readonly";
 static String email(Context c,String realm){return WidgetState.prefs(c).getString(realm+"_google_email","").trim();}
 private static AuthorizationRequest request(Context c,String realm) {
  String email=email(c,realm);
  if(!email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))throw new IllegalStateException("Set the "+realm+" Google email in Settings first.");
  return AuthorizationRequest.builder().setAccount(new Account(email,"com.google")).setRequestedScopes(List.of(new Scope(GMAIL_READ))).build();
 }
 static void connect(Activity activity,String realm,Runnable completed,java.util.function.Consumer<String> failed) {
  try {
   Identity.getAuthorizationClient(activity).authorize(request(activity,realm))
    .addOnSuccessListener(result->{if(result.hasResolution()){try{activity.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),REQUEST_MAIL,null,0,0,0);}catch(Exception e){failed.accept("Google consent could not be opened. Try again.");}}else{if(result.getAccessToken()==null||!result.getGrantedScopes().contains(GMAIL_READ)){failed.accept("Google did not grant mail access.");return;}WidgetState.prefs(activity).edit().putBoolean(realm+"_google_connected",true).commit();completed.run();}})
    .addOnFailureListener(error->failed.accept("Google could not authorize this Android app. Its package and signing certificate must be registered in your Google Cloud project, and the account must be on this phone."));
  }catch(IllegalStateException e){failed.accept(e.getMessage());}
 }
 static void finish(Activity activity,String realm,Intent intent,Runnable completed,java.util.function.Consumer<String> failed) {
  try{AuthorizationResult result=Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(intent);if(result.getAccessToken()==null||!result.getGrantedScopes().contains(GMAIL_READ))throw new IllegalStateException();WidgetState.prefs(activity).edit().putBoolean(realm+"_google_connected",true).commit();completed.run();}catch(Exception e){failed.accept("Mail access was not granted. Existing data has not been replaced.");}
 }
 static String token(Context c,String realm)throws Exception {
  if(!WidgetState.prefs(c).getBoolean(realm+"_google_connected",false))throw new IllegalStateException("Connect "+realm+" Gmail in Settings to authorize native read access.");
  AuthorizationResult result;
  try{result=Tasks.await(Identity.getAuthorizationClient(c).authorize(request(c,realm)),15,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("Google authorization is unavailable. Reconnect Gmail in Settings.");}
  if(result.hasResolution()||result.getAccessToken()==null)throw new IllegalStateException("Google needs your consent again. Reconnect Gmail in Settings.");
  return result.getAccessToken();
 }
 private GoogleConnection(){}
}

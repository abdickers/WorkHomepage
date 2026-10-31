package com.abdickers.realms;

import android.accounts.Account;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.*;
import com.google.android.gms.auth.api.identity.*;
import androidx.credentials.*;
import androidx.credentials.exceptions.*;
import com.google.android.libraries.identity.googleid.*;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.*;
import com.google.android.gms.tasks.Tasks;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** User chooses a Google account; its provider-verified email selects the permitted realm. */
final class GoogleConnection {
 static final int REQUEST_MAIL=711;
 static final String GMAIL_READ="https://www.googleapis.com/auth/gmail.readonly";
 static final String CALENDAR_READ="https://www.googleapis.com/auth/calendar.readonly";
 private static final ExecutorService VERIFY=Executors.newFixedThreadPool(2);
 static String expected(String realm){return realm.equals("work")?"dickersona@76fireworks.com":"abdickers@gmail.com";}
 static String email(Context c,String realm){return WidgetState.prefs(c).getString(realm+"_google_email",expected(realm)).trim();}
 private static AuthorizationRequest request(Context c,String realm,String scope){return AuthorizationRequest.builder().setAccount(new Account(email(c,realm),"com.google")).setRequestedScopes(List.of(new Scope(scope))).build();}
 static void connect(Activity a,String realm,Runnable completed,java.util.function.Consumer<String> failed){connect(a,realm,completed,failed,s->{});}
 static void connect(Activity a,String realm,Runnable completed,java.util.function.Consumer<String> failed,java.util.function.Consumer<String> progress){
  if(!Set.of("work","home").contains(realm)){failed.accept("Choose Home or Work first.");return;}
  progress.accept("Opening Google sign-in…");
  int available=GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(a);
  if(available!=ConnectionResult.SUCCESS){failed.accept("Google Play services needs attention before sign-in can open. Update Google Play services on this phone, then try again. ("+available+")");return;}
  Handler handler=new Handler(Looper.getMainLooper());AtomicBoolean waiting=new AtomicBoolean(true);CancellationSignal cancellation=new CancellationSignal();
  Runnable timeout=new Runnable(){public void run(){if(!waiting.get())return;if(!alive(a)){waiting.set(false);cancellation.cancel();return;}if(!a.hasWindowFocus()){handler.postDelayed(this,10000);return;}if(waiting.compareAndSet(true,false)){cancellation.cancel();failed.accept("Google sign-in did not open. Check your internet connection and try again.");}}};handler.postDelayed(timeout,25000);
  try{
   byte[] nonce=new byte[32];new java.security.SecureRandom().nextBytes(nonce);
   // This is only an account selector. Its ID token is neither persisted nor accepted as a session.
   // Authorization and the Gmail profile below establish the actual data access and identity.
   GetSignInWithGoogleOption option=new GetSignInWithGoogleOption.Builder("644801431816-9f7u70h62fipmebva3llupi2o17b1g80.apps.googleusercontent.com").setNonce(android.util.Base64.encodeToString(nonce,android.util.Base64.NO_WRAP|android.util.Base64.URL_SAFE)).build();
   GetCredentialRequest choose=new GetCredentialRequest.Builder().addCredentialOption(option).build();
   CredentialManager.create(a).getCredentialAsync(a,choose,cancellation,a.getMainExecutor(),new CredentialManagerCallback<GetCredentialResponse,GetCredentialException>(){
    public void onResult(GetCredentialResponse response){if(!waiting.compareAndSet(true,false)||!alive(a))return;handler.removeCallbacks(timeout);try{Credential credential=response.getCredential();if(!(credential instanceof CustomCredential)||!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType()))throw new IllegalStateException();String selected=GoogleIdTokenCredential.createFrom(credential.getData()).getId();if(!expected(realm).equalsIgnoreCase(selected)){failed.accept("Choose "+expected(realm)+" for "+(realm.equals("work")?"Work":"Home")+". Tap Sign in to choose again.");return;}authorizeAccount(a,realm,selected,completed,failed,progress);}catch(Exception e){failed.accept("Google could not select this account. Please try again.");}}
    public void onError(GetCredentialException error){if(waiting.compareAndSet(true,false)&&alive(a)){handler.removeCallbacks(timeout);failed.accept(error instanceof GetCredentialCancellationException?"Sign-in was canceled. Tap Sign in whenever you are ready.":"Google’s account chooser could not open. Check that Google Play services is up to date, then try again.");}}
   });
  }catch(Exception e){waiting.set(false);handler.removeCallbacks(timeout);failed.accept(problem(e));}
 }
 private static void authorizeAccount(Activity a,String realm,String selected,Runnable completed,java.util.function.Consumer<String> failed,java.util.function.Consumer<String> progress){
  progress.accept("Requesting read-only email and calendar access…");Handler handler=new Handler(Looper.getMainLooper());AtomicBoolean waiting=new AtomicBoolean(true);Runnable timeout=()->{if(waiting.compareAndSet(true,false)&&alive(a))failed.accept("Google’s permission window did not open. Please try signing in again.");};handler.postDelayed(timeout,25000);
  try{AuthorizationRequest interactive=AuthorizationRequest.builder().setAccount(new Account(selected,"com.google")).setRequestedScopes(List.of(new Scope(GMAIL_READ),new Scope(CALENDAR_READ))).build();Identity.getAuthorizationClient(a).authorize(interactive).addOnSuccessListener(result->{if(!waiting.compareAndSet(true,false)||!alive(a))return;handler.removeCallbacks(timeout);if(result.hasResolution()){try{progress.accept("Finish allowing access in the Google window.");a.startIntentSenderForResult(result.getPendingIntent().getIntentSender(),REQUEST_MAIL,null,0,0,0);}catch(Exception e){failed.accept("Google’s permission window could not open. Please try again.");}}else accept(a,realm,result,completed,failed,progress);}).addOnFailureListener(error->{if(waiting.compareAndSet(true,false)&&alive(a)){handler.removeCallbacks(timeout);failed.accept(problem(error));}});}catch(Exception e){waiting.set(false);handler.removeCallbacks(timeout);failed.accept(problem(e));}
 }

 static void finish(Activity a,String realm,Intent intent,Runnable completed,java.util.function.Consumer<String> failed){finish(a,realm,intent,completed,failed,s->{});}
 static void finish(Activity a,String realm,Intent intent,Runnable completed,java.util.function.Consumer<String> failed,java.util.function.Consumer<String> progress){try{accept(a,realm,Identity.getAuthorizationClient(a).getAuthorizationResultFromIntent(intent),completed,failed,progress);}catch(Exception e){failed.accept(problem(e));}}
 private static void accept(Activity a,String realm,AuthorizationResult result,Runnable completed,java.util.function.Consumer<String> failed,java.util.function.Consumer<String> progress){
  if(result.getAccessToken()==null||!result.getGrantedScopes().contains(GMAIL_READ)){failed.accept("Email access was not allowed. Tap Sign in and allow read-only email access to connect The Ravens.");return;}progress.accept("Checking your account…");long generation=Connections.generation(a);Handler verificationHandler=new Handler(Looper.getMainLooper());AtomicBoolean checking=new AtomicBoolean(true);Runnable verificationTimeout=()->{if(checking.compareAndSet(true,false)&&alive(a))failed.accept("Checking your account took too long. Check your connection and try again.");};verificationHandler.postDelayed(verificationTimeout,25000);
  VERIFY.execute(()->{try{String actual=BoundedHttp.get("https://gmail.googleapis.com/gmail/v1/users/me/profile?fields=emailAddress","Bearer "+result.getAccessToken()).getString("emailAddress");if(!expected(realm).equalsIgnoreCase(actual))throw new IllegalStateException("Choose "+expected(realm)+" for "+(realm.equals("work")?"Work":"Home")+". Your other account has not been changed.");a.runOnUiThread(()->{if(!checking.compareAndSet(true,false)||!alive(a))return;verificationHandler.removeCallbacks(verificationTimeout);synchronized(Connections.LOCK){if(generation!=Connections.generation(a)){failed.accept("Your connections changed while signing in. Please try again.");return;}WidgetState.prefs(a).edit().putString(realm+"_google_email",actual).putBoolean(realm+"_google_connected",true).putBoolean(realm+"_google_calendar_connected",result.getGrantedScopes().contains(CALENDAR_READ)).putString(realm+"_calendar",actual).commit();Connections.invalidate(a,realm);}completed.run();});}catch(Exception e){a.runOnUiThread(()->{if(checking.compareAndSet(true,false)&&alive(a)){verificationHandler.removeCallbacks(verificationTimeout);failed.accept(e instanceof IllegalStateException?e.getMessage():"We could not verify your Google account. Check your connection and try again.");}});}});
 }
 static String token(Context c,String realm)throws Exception{return token(c,realm,GMAIL_READ);}
 static String token(Context c,String realm,String scope)throws Exception{
  String flag=realm+(scope.equals(CALENDAR_READ)?"_google_calendar_connected":"_google_connected");if(!WidgetState.prefs(c).getBoolean(flag,false))throw new IllegalStateException("Sign in to "+(realm.equals("work")?"Work":"Home")+" to connect this panel.");AuthorizationResult result;
  try{result=Tasks.await(Identity.getAuthorizationClient(c).authorize(request(c,realm,scope)),15,TimeUnit.SECONDS);}catch(Exception e){throw new IllegalStateException("Google needs you to reconnect. Open Accounts and tap Sign in.");}
  if(result.hasResolution()||result.getAccessToken()==null||!result.getGrantedScopes().contains(scope))throw new IllegalStateException("Google needs your permission. Open Accounts and tap Sign in.");return result.getAccessToken();
 }
 private static boolean alive(Activity a){return !a.isFinishing()&&!a.isDestroyed();}
 private static String problem(Exception e){int code=e instanceof ApiException?((ApiException)e).getStatusCode():-1;return code==16?"Sign-in was canceled. You can try again whenever you are ready.":"Google sign-in could not finish. Try again; if this continues, the app needs a connection fix. Your existing account has not been changed."+(code<0?"":" (Google "+code+")");}
 private GoogleConnection(){}
}

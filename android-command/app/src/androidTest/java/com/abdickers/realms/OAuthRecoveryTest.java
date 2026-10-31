package com.abdickers.realms;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import net.openid.appauth.*;
import org.json.*;
import org.junit.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.*;
import static androidx.test.espresso.matcher.ViewMatchers.*;
import static androidx.test.espresso.assertion.ViewAssertions.*;

/** Exercises the actual SDK redirect activities, without granting real provider access. */
public class OAuthRecoveryTest {
 Context c(){return InstrumentationRegistry.getInstrumentation().getTargetContext();}
 @Before public void reset(){WidgetState.prefs(c()).edit().clear().putBoolean("welcome_seen",true).commit();Vault.clear(c());}
 @Test public void actualSlackBrowserReturnReachesAppInsteadOfCrashingOrSayingCanceled()throws Exception{
  WidgetState.prefs(c()).edit().putBoolean("work_google_connected",true).commit();
  AtomicInteger browserOpens=new AtomicInteger();
  Instrumentation instrumentation=InstrumentationRegistry.getInstrumentation();
  Instrumentation.ActivityMonitor browser=new Instrumentation.ActivityMonitor(){
   @Override public Instrumentation.ActivityResult onStartActivity(Intent intent){
    Uri url=intent.getData();
    if(url==null||!"slack.com".equals(url.getHost())||!"/oauth/v2/authorize".equals(url.getPath()))return null;
    browserOpens.incrementAndGet();
    assertEquals("S256",url.getQueryParameter("code_challenge_method"));
    Uri redirect=Uri.parse(SlackConnection.REDIRECT).buildUpon().appendQueryParameter("error","access_denied").appendQueryParameter("state",url.getQueryParameter("state")).build();
    // Replace only the external browser in this test. The real Android receiver,
    // SDK management activity and our onActivityResult still run normally.
    new Handler(Looper.getMainLooper()).postDelayed(()->c().startActivity(new Intent(Intent.ACTION_VIEW,redirect).setClassName(c(),"net.openid.appauth.RedirectUriReceiverActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)),150);
    return new Instrumentation.ActivityResult(Activity.RESULT_CANCELED,null);
   }
  };
  instrumentation.addMonitor(browser);
  try(ActivityScenario<SlackOAuthActivity> ignored=ActivityScenario.launch(new Intent(c(),SlackOAuthActivity.class))){
   CommandTest.await("The real SDK callback must return to Slack setup",()->{boolean[] found={false};instrumentation.runOnMainSync(()->{for(Activity activity:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED))if(activity instanceof SlackOAuthActivity){android.view.ViewGroup root=activity.findViewById(android.R.id.content);found[0]=contains(root,"Slack permission was not granted");}});return found[0];});
   assertEquals(1,browserOpens.get());assertFalse(SlackConnection.connected(c()));assertTrue(WidgetState.prefs(c()).getBoolean("work_google_connected",false));
   onView(withText("Try Slack sign-in again")).check(matches(isDisplayed()));
   new CommandTest().shot("slack-browser-return-recovery");
  }finally{instrumentation.removeMonitor(browser);}
 }
 static boolean contains(android.view.View view,String text){if(view instanceof android.widget.TextView label&&label.getText().toString().contains(text))return true;if(view instanceof android.view.ViewGroup group)for(int n=0;n<group.getChildCount();n++)if(contains(group.getChildAt(n),text))return true;return false;}
 @Test public void vendorRegistrationWithUnusedSecretStillUsesOnlyPublicClient()throws Exception{
  JSONObject response=new JSONObject().put("client_id","SYNTHETIC-public-client").put("client_secret","SYNTHETIC-NEVER-STORE").put("client_secret_expires_at",0).put("disabled",false).put("token_endpoint_auth_method","none").put("redirect_uris",new JSONArray().put(JiraConnection.REDIRECT.toString()));
  assertEquals("SYNTHETIC-public-client",JiraRegistration.publicClient(response));assertEquals("none",JiraRegistration.request().getString("token_endpoint_auth_method"));assertFalse(JiraRegistration.request().has("client_secret"));
  for(String method:new String[]{"client_secret_post","client_secret_basic"}){response.put("token_endpoint_auth_method",method);try{JiraRegistration.publicClient(response);fail("Confidential clients cannot be used on the phone");}catch(IllegalStateException expected){}}
 }
 @Test public void providerFailuresAreSafeAndRecoveryDoesNotChangeSavedSettings()throws Exception{
  WidgetState.prefs(c()).edit().putBoolean("work_google_connected",true).putString("home_wifi","SYNTHETIC-SAVED").commit();
  AuthorizationException error=AuthorizationException.fromOAuthTemplate(AuthorizationException.AuthorizationRequestErrors.INVALID_REQUEST,"invalid_request","SECRET-DO-NOT-DISPLAY",Uri.parse("https://example.invalid/private?token=SECRET"));
  String message=OAuthFeedback.failure("Jira",error);assertFalse(message.contains("SECRET"));assertFalse(message.contains("canceled"));assertTrue(message.contains("1:"));
  // Launch without a Google session to suppress real network/consent in this test.
  WidgetState.prefs(c()).edit().putBoolean("work_google_connected",false).commit();
  try(ActivityScenario<JiraOAuthActivity> scenario=ActivityScenario.launch(new Intent(c(),JiraOAuthActivity.class))){scenario.onActivity(a->a.onActivityResult(8231,Activity.RESULT_OK,error.toIntent()));onView(withText(message)).check(matches(isDisplayed()));scenario.recreate();onView(withText("Try Jira sign-in again")).check(matches(isDisplayed()));}
  assertEquals("SYNTHETIC-SAVED",WidgetState.prefs(c()).getString("home_wifi",""));assertFalse(JiraConnection.connected(c()));
 }
}

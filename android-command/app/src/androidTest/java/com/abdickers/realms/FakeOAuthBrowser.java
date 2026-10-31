package com.abdickers.realms;

import android.app.Activity;
import android.os.*;
import android.content.Intent;
import android.net.Uri;
import android.widget.TextView;

/** Real test-only external Activity, standing in for a provider permission page. */
public final class FakeOAuthBrowser extends Activity {
 @Override protected void onCreate(Bundle saved){super.onCreate(saved);TextView label=new TextView(this);label.setText("Emulator provider permission page");setContentView(label);}
 @Override protected void onResume(){super.onResume();new Handler(Looper.getMainLooper()).postDelayed(()->{
  Uri authorization=getIntent().getData();Uri redirect=Uri.parse(authorization.getQueryParameter("redirect_uri")).buildUpon().appendQueryParameter("error","access_denied").appendQueryParameter("state",authorization.getQueryParameter("state")).build();
  startActivity(new Intent(Intent.ACTION_VIEW,redirect).setClassName("com.abdickers.realms","net.openid.appauth.RedirectUriReceiverActivity"));finish();
 },200);}
}

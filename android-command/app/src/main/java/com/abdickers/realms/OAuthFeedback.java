package com.abdickers.realms;

import net.openid.appauth.AuthorizationException;

/** Safe feedback: never display provider descriptions, redirect URLs, codes or tokens. */
final class OAuthFeedback {
 static String failure(String provider,AuthorizationException error){
  if(error==null||error.equals(AuthorizationException.GeneralErrors.USER_CANCELED_AUTH_FLOW))return provider+" returned without completing sign-in. Tap Try again to reconnect.";
  if(error.equals(AuthorizationException.AuthorizationRequestErrors.STATE_MISMATCH))return provider+" returned a sign-in that could not be verified. Please try again.";
  if("access_denied".equals(error.error))return provider+" permission was not granted. Tap Try again when you are ready.";
  return provider+" could not finish sign-in ("+error.type+":"+error.code+"). Your other connections are unchanged. Please try again.";
 }
 private OAuthFeedback(){}
}

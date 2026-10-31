package com.abdickers.realms;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.*;

/** Bounded public-client registration. No provider secret is retained or used. */
final class JiraRegistration {
 static JSONObject request()throws JSONException{return new JSONObject().put("application_type","native").put("client_name","Middle-earth Command Android").put("redirect_uris",new JSONArray(List.of(JiraConnection.REDIRECT.toString()))).put("grant_types",new JSONArray(List.of("authorization_code","refresh_token"))).put("response_types",new JSONArray(List.of("code"))).put("token_endpoint_auth_method","none").put("scope",JiraConnection.SCOPE);}
 static String publicClient(JSONObject response)throws Exception{
  String id=response.getString("client_id");JSONArray redirects=response.getJSONArray("redirect_uris");
  if(response.has("error")||!"none".equals(response.optString("token_endpoint_auth_method"))||id.isBlank()||id.length()>256||!id.matches("[A-Za-z0-9._~-]+")||redirects.length()!=1||!JiraConnection.REDIRECT.toString().equals(redirects.getString(0)))throw new IllegalStateException("Atlassian did not return a valid native app connection. Please try again.");
  // Atlassian can return a client_secret even for auth method none. It is unnecessary
  // for this public PKCE client and must never be stored, logged or sent by the app.
  return id;
 }
 static String register()throws Exception{
  byte[] body=request().toString().getBytes(StandardCharsets.UTF_8);HttpURLConnection connection=(HttpURLConnection)new URI(JiraConnection.ISSUER+"/dcr/register").toURL().openConnection();connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setInstanceFollowRedirects(false);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Content-Type","application/json");connection.setFixedLengthStreamingMode(body.length);
  try{try(OutputStream out=connection.getOutputStream()){out.write(body);}int status=connection.getResponseCode();if(status==429)throw new BoundedHttp.RateLimitException(BoundedHttp.retryAfter(connection.getHeaderField("Retry-After")));if(status!=200&&status!=201)throw new IllegalStateException("Atlassian could not prepare sign-in (HTTP "+status+"). Please try again.");try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[2048];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>32768)throw new IOException("Registration response exceeded its limit");out.write(buffer,0,n);}return publicClient(new JSONObject(out.toString(StandardCharsets.UTF_8.name())));}}finally{connection.disconnect();}
 }
 private JiraRegistration(){}
}

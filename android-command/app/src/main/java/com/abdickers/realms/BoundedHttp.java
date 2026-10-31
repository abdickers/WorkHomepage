package com.abdickers.realms;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.json.*;

final class BoundedHttp {
 static final Set<String> STATUS_HOSTS=Set.of("www.cloudflarestatus.com","www.vercel-status.com","status.bigcommerce.com","status.netsuite.com","status.celigo.com","www.githubstatus.com");
 static final Set<String> HOSTS=Set.of("api.vercel.com","76fireworks.atlassian.net","slack.com","gmail.googleapis.com","www.googleapis.com","api.integrator.io");
 static void validate(URI uri){if(!"https".equals(uri.getScheme())||!(HOSTS.contains(uri.getHost())||STATUS_HOSTS.contains(uri.getHost()))||uri.getRawUserInfo()!=null||uri.getPort()!=-1)throw new IllegalArgumentException("Unsupported provider address");}
 static JSONObject get(String url,String authorization)throws Exception{
  URI uri=URI.create(url);validate(uri);if(authorization.indexOf('\r')>=0||authorization.indexOf('\n')>=0)throw new ProviderException("Credential contains an invalid line break. Reconnect in Settings.");
  if(STATUS_HOSTS.contains(uri.getHost())&&(!authorization.isEmpty()||!"/api/v2/status.json".equals(uri.getPath())||uri.getRawQuery()!=null))throw new IllegalArgumentException("Public status requests cannot carry credentials");
  HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","MiddleEarthCommand/0.3 (+https://github.com/abdickers/WorkHomepage)");if(!authorization.isEmpty())c.setRequestProperty("Authorization",authorization);
  try{int code=c.getResponseCode();if(code==401||code==403)throw new ProviderException("Connection needs authorization. Update it in Settings.");if(code==429)throw new RateLimitException(retryAfter(c.getHeaderField("Retry-After")));if(code<200||code>=300)throw new ProviderException("Provider returned HTTP "+code+". Previous data retained.");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>262144)throw new ProviderException("Provider response exceeded the 256 KB safety limit.");out.write(buffer,0,n);}Object result=new JSONTokener(out.toString(StandardCharsets.UTF_8.name())).nextValue();if(result instanceof JSONArray)return new JSONObject().put("items",result);if(result instanceof JSONObject)return (JSONObject)result;throw new ProviderException("Provider did not return a JSON object or list.");}}finally{c.disconnect();}
 }
 static long retryAfter(String raw){try{return Math.min(86400,Math.max(60,Long.parseLong(raw)))*1000;}catch(Exception e){return 900000;}}
 /** Token exchange only. No general POST transport and no client secret in the phone. */
 static JSONObject slackOAuth(java.util.Map<String,String> fields)throws Exception{
  Set<String> expected="refresh_token".equals(fields.get("grant_type"))?Set.of("client_id","grant_type","refresh_token"):Set.of("client_id","grant_type","code","code_verifier","redirect_uri");
  if(!fields.keySet().equals(expected)||!fields.get("client_id").matches("[0-9]+\\.[0-9]+")||!("refresh_token".equals(fields.get("grant_type"))||"authorization_code".equals(fields.get("grant_type"))))throw new IllegalArgumentException("Invalid Slack authorization request");
  java.util.StringJoiner form=new java.util.StringJoiner("&");for(var field:fields.entrySet()){String value=field.getValue();if(value==null||value.isEmpty()||value.length()>4096||value.contains("\r")||value.contains("\n"))throw new IllegalArgumentException("Invalid Slack authorization value");form.add(URLEncoder.encode(field.getKey(),StandardCharsets.UTF_8)+"="+URLEncoder.encode(value,StandardCharsets.UTF_8));}
  HttpURLConnection connection=(HttpURLConnection)new URI("https://slack.com/api/oauth.v2.access").toURL().openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(10000);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Content-Type","application/x-www-form-urlencoded");byte[] bytes=form.toString().getBytes(StandardCharsets.UTF_8);connection.setFixedLengthStreamingMode(bytes.length);
  try{try(OutputStream out=connection.getOutputStream()){out.write(bytes);}int status=connection.getResponseCode();if(status==429)throw new RateLimitException(retryAfter(connection.getHeaderField("Retry-After")));if(status!=200)throw new ProviderException("Slack sign-in could not finish. Please reconnect.");try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>65536)throw new ProviderException("Slack authorization response is too large.");out.write(buffer,0,n);}JSONObject result=new JSONObject(out.toString(StandardCharsets.UTF_8.name()));if(!result.optBoolean("ok"))throw new ProviderException("Slack did not allow this connection. Please reconnect.");return result;}}finally{connection.disconnect();}
 }
 static final class RateLimitException extends IOException{final long delay;RateLimitException(long delay){super("Provider rate limit reached. Refresh is paused before retrying.");this.delay=delay;}}
 static final class ProviderException extends Exception{ProviderException(String message){super(message);}}
}

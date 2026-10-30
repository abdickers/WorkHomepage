package com.abdickers.realms;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.json.*;

final class BoundedHttp {
 static final Set<String> STATUS_HOSTS=Set.of("www.cloudflarestatus.com","www.vercel-status.com","status.bigcommerce.com","status.netsuite.com","status.celigo.com","www.githubstatus.com");
 static final Set<String> HOSTS=Set.of("api.vercel.com","76fireworks.atlassian.net","slack.com","gmail.googleapis.com","api.integrator.io");
 static void validate(URI uri){if(!"https".equals(uri.getScheme())||!(HOSTS.contains(uri.getHost())||STATUS_HOSTS.contains(uri.getHost()))||uri.getRawUserInfo()!=null||uri.getPort()!=-1)throw new IllegalArgumentException("Unsupported provider address");}
 static JSONObject get(String url,String authorization)throws Exception{
  URI uri=URI.create(url);validate(uri);if(authorization.indexOf('\r')>=0||authorization.indexOf('\n')>=0)throw new ProviderException("Credential contains an invalid line break. Reconnect in Settings.");
  if(STATUS_HOSTS.contains(uri.getHost())&&(!authorization.isEmpty()||!"/api/v2/status.json".equals(uri.getPath())||uri.getRawQuery()!=null))throw new IllegalArgumentException("Public status requests cannot carry credentials");
  HttpURLConnection c=(HttpURLConnection)uri.toURL().openConnection();c.setInstanceFollowRedirects(false);c.setConnectTimeout(10000);c.setReadTimeout(10000);c.setRequestProperty("Accept","application/json");c.setRequestProperty("User-Agent","MiddleEarthCommand/0.3 (+https://github.com/abdickers/WorkHomepage)");if(!authorization.isEmpty())c.setRequestProperty("Authorization",authorization);
  try{int code=c.getResponseCode();if(code==401||code==403)throw new ProviderException("Connection needs authorization. Update it in Settings.");if(code==429)throw new RateLimitException(retryAfter(c.getHeaderField("Retry-After")));if(code<200||code>=300)throw new ProviderException("Provider returned HTTP "+code+". Previous data retained.");try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){if(out.size()+n>262144)throw new ProviderException("Provider response exceeded the 256 KB safety limit.");out.write(buffer,0,n);}Object result=new JSONTokener(out.toString(StandardCharsets.UTF_8.name())).nextValue();if(result instanceof JSONArray)return new JSONObject().put("items",result);if(result instanceof JSONObject)return (JSONObject)result;throw new ProviderException("Provider did not return a JSON object or list.");}}finally{c.disconnect();}
 }
 static long retryAfter(String raw){try{return Math.min(86400,Math.max(60,Long.parseLong(raw)))*1000;}catch(Exception e){return 900000;}}
 static final class RateLimitException extends IOException{final long delay;RateLimitException(long delay){super("Provider rate limit reached. Refresh is paused before retrying.");this.delay=delay;}}
 static final class ProviderException extends Exception{ProviderException(String message){super(message);}}
}

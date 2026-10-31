package com.abdickers.realms;

import android.content.Context;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Uses the existing protected Home service. No Google or station keys enter the APK. */
final class HomeConnection {
 static final String ORIGIN="https://sage-cabin-nest.nest-service.workers.dev",KEY="home_device_session",PENDING="home_device_pending";
 static final Set<String> READS=Set.of("/__access/session","/native/session","/weather/current","/devices");
 interface Transport{JSONObject call(String path,String jwt,String session,JSONObject body)throws Exception;}
 static final Transport NETWORK=HomeConnection::request;
 static boolean connected(Context c){return c.getSharedPreferences("vault",0).contains(KEY);}
 static String b64(byte[] bytes){return Base64.encodeToString(bytes,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
 static byte[] decode(String text,int limit)throws Exception{if(text==null||text.length()>limit||!text.matches("[A-Za-z0-9_-]+"))throw new GeneralSecurityException("Invalid encoded response");return Base64.decode(text,Base64.URL_SAFE|Base64.NO_WRAP|Base64.NO_PADDING);}
 static JSONObject prepare(long generation,long now)throws Exception{
  KeyPairGenerator generator=KeyPairGenerator.getInstance("RSA");generator.initialize(2048);KeyPair pair=generator.generateKeyPair();byte[] state=new byte[32];new SecureRandom().nextBytes(state);StringBuilder hex=new StringBuilder();for(byte b:state)hex.append(String.format(Locale.ROOT,"%02x",b&255));
  return new JSONObject().put("state",hex.toString()).put("private",b64(pair.getPrivate().getEncoded())).put("public",b64(pair.getPublic().getEncoded())).put("issued",now).put("generation",generation);
 }
 static JSONObject decrypt(String compact,JSONObject pending,long now)throws Exception{
  long age=now-pending.getLong("issued");if(age<0||age>300000)throw new GeneralSecurityException("Expired phone connection");
  if(compact==null||compact.length()>32768)throw new GeneralSecurityException("Invalid phone response");String[] parts=compact.split("\\.",-1);if(parts.length!=5)throw new GeneralSecurityException("Invalid encrypted response");
  JSONObject header=new JSONObject(new String(decode(parts[0],1024),StandardCharsets.UTF_8));if(header.length()!=3||!"RSA-OAEP-256".equals(header.optString("alg"))||!"A256GCM".equals(header.optString("enc"))||!"command-home+jwe".equals(header.optString("typ")))throw new GeneralSecurityException("Unsupported encryption");
  PrivateKey privateKey=KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decode(pending.getString("private"),4096)));
  Cipher rsa=Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");rsa.init(Cipher.DECRYPT_MODE,privateKey,new OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT));byte[] key=rsa.doFinal(decode(parts[1],1024)),iv=decode(parts[2],64),tag=decode(parts[4],64);if(key.length!=32||iv.length!=12||tag.length!=16)throw new GeneralSecurityException("Invalid encryption parameters");
  byte[] body=decode(parts[3],24000),ciphertext=Arrays.copyOf(body,body.length+tag.length);System.arraycopy(tag,0,ciphertext,body.length,tag.length);Cipher aes=Cipher.getInstance("AES/GCM/NoPadding");aes.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));aes.updateAAD(parts[0].getBytes(StandardCharsets.US_ASCII));
  JSONObject payload=new JSONObject(new String(aes.doFinal(ciphertext),StandardCharsets.UTF_8));validate(payload,pending.getString("state"),now);return payload;
 }
 static void validate(JSONObject payload,String state,long now)throws Exception{
  if(payload.optInt("v")!=1||!state.equals(payload.optString("state"))||!ORIGIN.equals(payload.optString("origin"))||!GoogleConnection.expected("home").equals(payload.optString("email"))||payload.optLong("expiresAt")<=now||payload.optLong("expiresAt")>now+120000||payload.optLong("accessExpiresAt")<=now+60000||payload.optLong("sessionExpiresAt")<=now||!payload.optString("session").matches("[a-f0-9]{64}")||!payload.optString("accessJwt").matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"))throw new GeneralSecurityException("Home sign-in could not be verified");
 }
 static void install(Context c,JSONObject payload,long generation,Transport transport)throws Exception{
  JSONObject identity=transport.call("/__access/session",payload.getString("accessJwt"),"",null),session=transport.call("/native/session",payload.getString("accessJwt"),payload.getString("session"),null);
  if(!GoogleConnection.expected("home").equals(identity.optString("email"))||!GoogleConnection.expected("home").equals(session.optString("email")))throw new GeneralSecurityException("Choose your personal owner account");
  synchronized(Connections.LOCK){if(generation!=Connections.generation(c)||!WidgetState.prefs(c).getBoolean("home_google_connected",false))throw new IllegalStateException("Your accounts changed. Connect Home again.");Vault.put(c,KEY,payload.toString());Vault.put(c,PENDING,"");Connections.invalidate(c,"home");}
 }
 static JSONObject call(Context c,String path,JSONObject body)throws Exception{
  JSONObject saved=new JSONObject(Vault.get(c,KEY));if(saved.optLong("accessExpiresAt")<=System.currentTimeMillis()+15000)throw new IllegalStateException("Your private Home sign-in expired. Reconnect Home devices & weather in Accounts.");return NETWORK.call(path,saved.getString("accessJwt"),saved.getString("session"),body);
 }
 static JSONObject request(String path,String jwt,String session,JSONObject body)throws Exception{
  if(!(body==null?READS.contains(path):Set.of("/thermostat","/disconnect").contains(path))||jwt.length()>16384||!jwt.matches("[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+")||(!session.isEmpty()&&!session.matches("[a-f0-9]{64}")))throw new IllegalArgumentException("Invalid Home request");
  HttpURLConnection connection=(HttpURLConnection)new URL(ORIGIN+path).openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(10000);connection.setReadTimeout(15000);connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Cookie","CF_Authorization="+jwt);connection.setRequestProperty("Origin",ORIGIN);if(!session.isEmpty())connection.setRequestProperty("Authorization","Bearer "+session);
  try{if(body!=null){byte[] bytes=body.toString().getBytes(StandardCharsets.UTF_8);connection.setRequestMethod("POST");connection.setDoOutput(true);connection.setRequestProperty("Content-Type","application/json");connection.setFixedLengthStreamingMode(bytes.length);try(OutputStream out=connection.getOutputStream()){out.write(bytes);}}
   int status=connection.getResponseCode();if(status==429)throw new BoundedHttp.RateLimitException(BoundedHttp.retryAfter(connection.getHeaderField("Retry-After")));if(status==401||status==403||status>=300&&status<400)throw new IllegalStateException("Reconnect Home devices & weather in Accounts with your personal email.");if(status<200||status>=300)throw new IllegalStateException("Home could not complete this request (HTTP "+status+"). Saved information is unchanged.");if(!String.valueOf(connection.getContentType()).contains("application/json"))throw new IllegalStateException("Home needs a new private-site sign-in. Reconnect it in Accounts.");
   try(InputStream in=connection.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>262144)throw new IOException("Home response exceeded its limit");out.write(buffer,0,count);}return new JSONObject(out.toString(StandardCharsets.UTF_8.name()));}
  }finally{connection.disconnect();}
 }
 private HomeConnection(){}
}

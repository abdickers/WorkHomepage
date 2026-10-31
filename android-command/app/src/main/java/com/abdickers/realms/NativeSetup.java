package com.abdickers.realms;

import android.content.Context;
import android.content.pm.PackageManager;
import android.security.keystore.*;
import android.util.Base64;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateFactory;
import java.security.spec.MGF1ParameterSpec;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;

/** Device-bound, publisher-signed setup files. Private device keys never leave Android Keystore. */
final class NativeSetup {
 private static final String DEVICE_KEY="command-setup-recipient-v1";
 private static final long WINDOW=24*60*60*1000L;
 record Preview(String nonce,long expiresAt,Map<String,String> credentials,Map<String,String> settings){}
 static byte[] unbase(String value){return Base64.decode(value,Base64.NO_WRAP);}
 static String base(byte[] value){return Base64.encodeToString(value,Base64.NO_WRAP);}
 static String digest(byte[] value)throws Exception{StringBuilder result=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(value))result.append(String.format(Locale.ROOT,"%02X",b&255));return result.toString();}
 private static KeyStore store()throws Exception{if(android.os.Build.VERSION.SDK_INT<35)throw new IllegalStateException("Assisted setup requires Android 15 or newer");KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);if(!store.containsAlias(DEVICE_KEY)){KeyPairGenerator generator=KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA,"AndroidKeyStore");generator.initialize(new KeyGenParameterSpec.Builder(DEVICE_KEY,KeyProperties.PURPOSE_DECRYPT).setKeySize(3072).setDigests(KeyProperties.DIGEST_SHA256).setMgf1Digests(KeyProperties.DIGEST_SHA256).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP).build());generator.generateKeyPair();}return store;}
 private static byte[] appCertificate(Context c)throws Exception{android.content.pm.SigningInfo info=c.getPackageManager().getPackageInfo(c.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES).signingInfo;if(info==null||info.hasMultipleSigners())throw new SecurityException("Unsupported app signing identity");return info.getApkContentsSigners()[0].toByteArray();}
 static JSONObject request(Context c)throws Exception {
  byte[] publicKey=store().getCertificate(DEVICE_KEY).getPublicKey().getEncoded(),nonce=new byte[32];new SecureRandom().nextBytes(nonce);String id=base(nonce);long expires=System.currentTimeMillis()+WINDOW;
  c.getSharedPreferences("setup",0).edit().putString("nonce",id).putLong("expires",expires).commit();
  return new JSONObject().put("version",1).put("package",c.getPackageName()).put("realm","work").put("nonce",id).put("expiresAt",expires).put("publicKeySpki",base(publicKey)).put("recipientSha256",digest(publicKey)).put("appSigningSha256",digest(appCertificate(c)));
 }
 static Preview preview(Context c,String file)throws Exception{PublicKey signer=CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(appCertificate(c))).getPublicKey();return decode(c,file,signer);}
 static Preview decode(Context c,String file,PublicKey signer)throws Exception {
  if(file.length()>65536)throw new SecurityException("Setup file is too large");JSONObject outer=new JSONObject(file);if(outer.getInt("version")!=1)throw new SecurityException("Unknown setup version");String encoded=outer.getString("body");Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(signer);verifier.update(encoded.getBytes(StandardCharsets.UTF_8));if(!verifier.verify(unbase(outer.getString("signature"))))throw new SecurityException("Setup signature was not issued by this app’s publisher");
  JSONObject body=new JSONObject(new String(unbase(encoded),StandardCharsets.UTF_8));String nonce=body.getString("nonce"),recipient=body.getString("recipientSha256");checkPending(c,nonce,body.getLong("expiresAt"));if(!c.getPackageName().equals(body.getString("package")))throw new SecurityException("Setup is for another app");KeyStore keyStore=store();if(!digest(keyStore.getCertificate(DEVICE_KEY).getPublicKey().getEncoded()).equals(recipient))throw new SecurityException("Setup is for another phone");
  Cipher rsa=Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");rsa.init(Cipher.DECRYPT_MODE,keyStore.getKey(DEVICE_KEY,null),new OAEPParameterSpec("SHA-256","MGF1",MGF1ParameterSpec.SHA256,PSource.PSpecified.DEFAULT));byte[] key=rsa.doFinal(unbase(body.getString("wrappedKey"))),iv=unbase(body.getString("iv"));if(key.length!=32||iv.length!=12)throw new SecurityException("Invalid setup encryption");Cipher aes=Cipher.getInstance("AES/GCM/NoPadding");aes.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));aes.updateAAD((c.getPackageName()+"\n"+nonce+"\n"+recipient).getBytes(StandardCharsets.UTF_8));JSONObject data;try{data=new JSONObject(new String(aes.doFinal(unbase(body.getString("ciphertext"))),StandardCharsets.UTF_8));}finally{Arrays.fill(key,(byte)0);}
  if(!"work".equals(data.getString("realm")))throw new SecurityException("This setup is not for the Work realm");Map<String,String> credentials=values(data.getJSONObject("credentials"),Set.of("vercel","jira","slack","celigo"));Map<String,String> settings=values(data.optJSONObject("settings"),Set.of("jira_email","vercel_team"));if(credentials.isEmpty())throw new SecurityException("Setup contains no supported connections");String email=settings.get("jira_email");if(email!=null&&!email.toLowerCase(Locale.ROOT).matches("[^\\s@]+@76fireworks\\.com"))throw new SecurityException("Unexpected Jira account domain");String team=settings.get("vercel_team");if(team!=null&&!team.matches("team_[A-Za-z0-9]+"))throw new SecurityException("Invalid Vercel team identifier");return new Preview(nonce,body.getLong("expiresAt"),Map.copyOf(credentials),Map.copyOf(settings));
 }
 private static Map<String,String> values(JSONObject data,Set<String> allowed)throws Exception{Map<String,String> values=new HashMap<>();if(data==null)return values;for(Iterator<String> keys=data.keys();keys.hasNext();){String name=keys.next();if(!allowed.contains(name))throw new SecurityException("Unsupported setup field");Object raw=data.get(name);if(!(raw instanceof String))throw new SecurityException("Invalid setup value");String value=((String)raw).trim();if(value.isEmpty()||value.length()>16000||value.contains("\n")||value.contains("\r"))throw new SecurityException("Invalid setup value");values.put(name,value);}return values;}
 private static void checkPending(Context c,String nonce,long expires){android.content.SharedPreferences pending=c.getSharedPreferences("setup",0);long limit=pending.getLong("expires",0);if(!nonce.equals(pending.getString("nonce",""))||System.currentTimeMillis()>expires||expires>limit||limit-System.currentTimeMillis()>WINDOW+60000)throw new SecurityException("Setup request expired or was already used");}
 static void install(Context c,Preview preview)throws Exception{synchronized(Connections.LOCK){checkPending(c,preview.nonce,preview.expiresAt);Vault.putAll(c,preview.credentials);android.content.SharedPreferences.Editor settings=WidgetState.prefs(c).edit();for(Map.Entry<String,String> e:preview.settings.entrySet())settings.putString(e.getKey(),e.getValue());settings.commit();c.getSharedPreferences("setup",0).edit().clear().commit();Connections.invalidate(c);CommandWidget.updateAll(c);}}
 private NativeSetup(){}
}

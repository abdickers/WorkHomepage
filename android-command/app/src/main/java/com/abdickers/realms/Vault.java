package com.abdickers.realms;

import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** Credentials are encrypted at rest by a non-exportable Android Keystore key. */
final class Vault {
 private static final String ALIAS="command-provider-credentials-v1";
 private static synchronized javax.crypto.SecretKey key()throws Exception{KeyStore ks=KeyStore.getInstance("AndroidKeyStore");ks.load(null);if(!ks.containsAlias(ALIAS)){KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");gen.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());gen.generateKey();}return (javax.crypto.SecretKey)ks.getKey(ALIAS,null);}
 static void putAll(Context c,java.util.Map<String,String> values)throws Exception{java.util.Map<String,String> encrypted=new java.util.HashMap<>();for(java.util.Map.Entry<String,String> entry:values.entrySet()){Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());encrypted.put(entry.getKey(),Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(entry.getValue().getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP));}android.content.SharedPreferences.Editor edit=c.getSharedPreferences("vault",0).edit();for(java.util.Map.Entry<String,String> entry:encrypted.entrySet())edit.putString(entry.getKey(),entry.getValue());if(!edit.commit())throw new IllegalStateException("Credentials could not be saved");}
 static void put(Context c,String name,String value)throws Exception{if(value.isEmpty()){c.getSharedPreferences("vault",0).edit().remove(name).commit();return;}Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());String data=Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)),Base64.NO_WRAP);if(!c.getSharedPreferences("vault",0).edit().putString(name,data).commit())throw new IllegalStateException("Credential could not be saved");}
 static String get(Context c,String name)throws Exception{String data=c.getSharedPreferences("vault",0).getString(name,"");if(data.isEmpty())return "";String[] parts=data.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);}
 static void clear(Context c){c.getSharedPreferences("vault",0).edit().clear().commit();}
}

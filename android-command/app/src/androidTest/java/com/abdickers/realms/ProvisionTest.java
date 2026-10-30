package com.abdickers.realms;
import androidx.test.platform.app.InstrumentationRegistry;
import org.json.JSONObject;
import org.junit.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
public class ProvisionTest {
 @Test public void realPublisherReplyDecryptsOnlyOnceAndRejectsAlteration()throws Exception {
  android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();File file=new File(c.getExternalFilesDir(null),"setup-reply.json");Assume.assumeTrue("Signing secrets unavailable to fork builds",file.isFile());String envelope;try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1)out.write(buffer,0,n);envelope=out.toString(StandardCharsets.UTF_8.name());}
  NativeSetup.Preview preview=NativeSetup.preview(c,envelope);assertEquals("EMULATOR-TEST-NOT-A-REAL-TOKEN",preview.credentials().get("vercel"));JSONObject tampered=new JSONObject(envelope);tampered.put("body",tampered.getString("body")+"A");assertThrows(Exception.class,()->NativeSetup.preview(c,tampered.toString()));
  long expires=c.getSharedPreferences("setup",0).getLong("expires",0);c.getSharedPreferences("setup",0).edit().putLong("expires",0).commit();assertThrows(Exception.class,()->NativeSetup.preview(c,envelope));c.getSharedPreferences("setup",0).edit().putLong("expires",expires).commit();
  NativeSetup.install(c,preview);assertEquals("EMULATOR-TEST-NOT-A-REAL-TOKEN",Vault.get(c,"vercel"));assertThrows(Exception.class,()->NativeSetup.preview(c,envelope));Vault.clear(c);
 }
}

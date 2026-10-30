package com.abdickers.realms;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.Test;
import java.io.*;
import java.nio.charset.StandardCharsets;
/** Public request only. Run separately before the encrypted publisher reply is generated. */
public class ProvisionBootstrap {
 @Test public void exportDeviceRequest()throws Exception{android.content.Context c=InstrumentationRegistry.getInstrumentation().getTargetContext();try(FileOutputStream out=new FileOutputStream(new File(c.getExternalFilesDir(null),"setup-request.json"))){out.write(NativeSetup.request(c).toString().getBytes(StandardCharsets.UTF_8));}}
}

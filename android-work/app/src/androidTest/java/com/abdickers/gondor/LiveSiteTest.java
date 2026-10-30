package com.abdickers.gondor;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class LiveSiteTest {
    @Test public void opensRealWorkWebsiteAndPreservesBrowserSignIn()throws Exception{
        Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();UiDevice device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        Intent launch=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).putExtra("url","https://example.invalid/ignored");context.startActivity(launch);
        assertTrue("Expected real browser toolbar for the protected Work site",device.wait(Until.hasObject(By.res("com.android.chrome","url_bar").textContains("adams-middle-earth-command")),30000));
        UiObject2 address=device.findObject(By.res("com.android.chrome","url_bar"));assertNotNull(address);assertTrue("App must open the fixed Work dashboard",address.getText().contains("adams-middle-earth-command"));
        // Wait for the actual sign-in control, rather than capturing a loading screen.
        assertTrue("Private site sign-in must render",device.wait(Until.hasObject(By.text("Continue with ChatGPT")),45000));
        device.waitForIdle(10000);
        String label=InstrumentationRegistry.getArguments().getString("screenLabel","pixel");File dir=new File(context.getExternalFilesDir(null),"proof");assertTrue(dir.isDirectory()||dir.mkdirs());
        device.dumpWindowHierarchy(new File(dir,label+"-live-site.xml"));assertTrue(device.takeScreenshot(new File(dir,label+"-live-site.png")));
        UiObject2 signIn=device.findObject(By.text("Continue with ChatGPT"));assertNotNull(signIn);signIn.click();
        assertTrue("Sign-in navigation must begin",device.wait(Until.gone(By.text("Continue with ChatGPT")),30000));
        device.waitForIdle(10000);device.dumpWindowHierarchy(new File(dir,label+"-sign-in-navigation.xml"));assertTrue(device.takeScreenshot(new File(dir,label+"-sign-in-navigation.png")));
        try(PrintWriter out=new PrintWriter(new File(dir,label+"-scope.txt"))){out.println("Verified direct launch of the real private Work site in a browser-backed app window. No snapshot UI. No auth bypass. No user credentials supplied; completed sign-in and authenticated controls must be checked on the user's phone.");}
    }
}

package com.abdickers.commandcenter;
import org.junit.Test;
import static org.junit.Assert.*;
public class RealmsTest {
    @Test public void privateWorkLoginStaysInBrowserContext(){
        assertTrue(Realms.usesBrowser(Realms.WORK));
        assertFalse(Realms.usesBrowser(Realms.PERSONAL));
        assertFalse(Realms.usesBrowser("https://evil.example"));
    }
    @Test public void untrustedIntentsCannotChooseArbitraryUrls(){
        assertEquals(Realms.PERSONAL,Realms.normalize(null));
        assertEquals(Realms.PERSONAL_URL,Realms.url("https://evil.example"));
        assertEquals(Realms.WORK_URL,Realms.url("work"));
        assertEquals("The Shire",Realms.title("personal"));
    }
}

package com.abdickers.gondor;
import org.junit.Test;
import static org.junit.Assert.*;
public class WorkSiteTest {
    @Test public void fixedPrivateWorkDestination(){assertEquals("https://adams-middle-earth-command.spirit-of-76-1921.chatgpt.site/?app=work",WorkSite.URL);assertFalse(WorkSite.URL.contains("pocket-sync"));assertFalse(WorkSite.URL.contains("token"));}
}

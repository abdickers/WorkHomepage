package com.abdickers.realms;
import org.junit.Test;
import java.net.URI;
import static org.junit.Assert.*;
public class TransportPolicyTest {
 @Test public void rejectsCredentialLeakTargets(){for(String url:new String[]{"http://api.vercel.com/v9/projects","https://api.vercel.com.evil.test/","https://api.vercel.com@evil.test/","https://user@api.vercel.com/","https://api.vercel.com:8443/","https://127.0.0.1/","https://adams-middle-earth-command.spirit-of-76-1921.chatgpt.site/"})assertThrows(IllegalArgumentException.class,()->BoundedHttp.validate(URI.create(url)));}
 @Test public void allowsOnlyFixedProviderHosts(){for(String host:BoundedHttp.HOSTS)BoundedHttp.validate(URI.create("https://"+host+"/"));}
 @Test public void boundsRateLimitWait(){assertEquals(900000,BoundedHttp.retryAfter(null));assertEquals(60000,BoundedHttp.retryAfter("-1"));assertEquals(86400000,BoundedHttp.retryAfter("9999999"));}
}

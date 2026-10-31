package com.abdickers.realms;

import android.content.Context;
import android.net.Uri;
import net.openid.appauth.*;
import java.util.*;
import java.util.concurrent.*;

final class JiraConnection {
 static final String KEY="work_jira_oauth",ISSUER="https://auth.atlassian.com/VCeDsk8ZHncYF1g234fKtc4lNipbBhu3";
 static final String SCOPE="read:me read:account email offline_access read:jira:agent-interface search:jira:agent-interface";
 static final Uri REDIRECT=Uri.parse("com.abdickers.realms://oauth/jira");private static final Object LOCK=new Object();
 static AuthorizationServiceConfiguration configuration(){return new AuthorizationServiceConfiguration(Uri.parse("https://auth.atlassian.com/authorize"),Uri.parse("https://auth.atlassian.com/oauth/token"),Uri.parse(ISSUER+"/dcr/register"));}
 static boolean connected(Context c){return c.getSharedPreferences("vault",0).contains(KEY);}
 static void validate(AuthState state){Set<String> expected=new HashSet<>(Arrays.asList(SCOPE.split(" ")));Set<String> actual=new HashSet<>(Arrays.asList(String.valueOf(state.getScope()).split(" ")));Set<String> allowed=new HashSet<>(expected);allowed.addAll(List.of("openid","profile"));if(!state.isAuthorized()||state.getAccessToken()==null||!actual.containsAll(Set.of("read:me","read:jira:agent-interface","search:jira:agent-interface"))||!allowed.containsAll(actual))throw new IllegalStateException("Jira did not grant the expected read-only access. Please reconnect.");}
 static void install(Context c,AuthState state,long generation)throws Exception{synchronized(Connections.LOCK){if(generation!=Connections.generation(c))throw new IllegalStateException("Your connections changed. Reconnect Jira.");validate(state);Vault.putAll(c,Map.of(KEY,state.jsonSerializeString(),"jira_refresh_uncertain",""));Connections.invalidate(c,"work");}}
 static String token(Context c)throws Exception{synchronized(LOCK){long generation=Connections.generation(c);String saved=Vault.get(c,KEY);if(saved.isEmpty())throw new IllegalStateException("Connect Jira in Accounts.");if(!Vault.get(c,"jira_refresh_uncertain").isEmpty())throw new IllegalStateException("Please reconnect Jira in Accounts.");AuthState state=AuthState.jsonDeserialize(saved);validate(state);AuthorizationServiceConfiguration config=state.getAuthorizationServiceConfiguration();if(config==null||!configuration().tokenEndpoint.equals(config.tokenEndpoint)||!configuration().authorizationEndpoint.equals(config.authorizationEndpoint))throw new IllegalStateException("Reconnect Jira in Accounts.");if(state.getNeedsTokenRefresh())synchronized(Connections.LOCK){if(generation!=Connections.generation(c))throw new IllegalStateException("Jira was disconnected.");Vault.put(c,"jira_refresh_uncertain","true");}AuthorizationService service=new AuthorizationService(c.getApplicationContext());CountDownLatch done=new CountDownLatch(1);String[] result=new String[1];boolean[] active={true};try{state.performActionWithFreshTokens(service,NoClientAuthentication.INSTANCE,Map.of("resource",JiraMcp.RESOURCE),(access,id,error)->{synchronized(done){if(active[0]){try{if(error==null&&access!=null)synchronized(Connections.LOCK){if(generation==Connections.generation(c)&&connected(c)){validate(state);Vault.putAll(c,Map.of(KEY,state.jsonSerializeString(),"jira_refresh_uncertain",""));result[0]=access;}}}catch(Exception ignored){}done.countDown();}}});if(!done.await(20,TimeUnit.SECONDS)){synchronized(done){active[0]=false;}throw new IllegalStateException("Jira sign-in took too long. Please reconnect.");}if(result[0]==null)throw new IllegalStateException("Reconnect Jira in Accounts.");return result[0];}finally{service.dispose();}}}
 static List<FeedStore.Row> read(Context c)throws Exception{JiraMcp api=new JiraMcp(token(c));return JiraMcp.read(api::call);}
 private JiraConnection(){}
}

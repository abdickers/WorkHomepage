package com.abdickers.realms;

import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class JiraConnectionTest {
 @Test public void siteAndWorkIdentityAreVerifiedWithoutReadingIssues()throws Exception{
  List<String> calls=new ArrayList<>();JiraMcp.verify((name,args)->{calls.add(name);return name.equals("getAccessibleAtlassianResources")?new JSONArray().put(new JSONObject().put("id",JiraMcp.CLOUD).put("url","https://76fireworks.atlassian.net")):new JSONObject().put("email","dickersona@76fireworks.com");});assertEquals(List.of("getAccessibleAtlassianResources","atlassianUserInfo"),calls);
  for(String email:List.of("abdickers@gmail.com","other@76fireworks.com")){try{JiraMcp.verify((name,args)->name.equals("getAccessibleAtlassianResources")?new JSONArray().put(new JSONObject().put("id",JiraMcp.CLOUD).put("url","https://76fireworks.atlassian.net")):new JSONObject().put("email",email));fail("Another account must fail");}catch(IllegalStateException expected){}}
  try{JiraMcp.verify((name,args)->new JSONArray().put(new JSONObject().put("id",JiraMcp.CLOUD).put("url","https://other.atlassian.net")));fail("Other site must fail");}catch(IllegalStateException expected){}
 }
 @Test public void issuesAreOneBoundedPageForOnlyTheFixedSiteAndCurrentUser()throws Exception{
  List<JSONObject> calls=new ArrayList<>();List<FeedStore.Row> rows=JiraMcp.read((name,args)->{assertEquals("searchJiraIssuesUsingJql",name);calls.add(args);JSONArray issues=new JSONArray();for(int n=0;n<35;n++)issues.put(new JSONObject().put("key","TEST-"+(n+1)).put("fields",new JSONObject().put("summary","SYNTHETIC issue").put("status",new JSONObject().put("name","To Do"))));return new JSONObject().put("issues",new JSONObject().put("nodes",issues)).put("nextPageToken","DO-NOT-FOLLOW");});assertEquals(1,calls.size());assertEquals(20,rows.size());assertEquals(20,calls.get(0).getInt("maxResults"));assertEquals(JiraMcp.CLOUD,calls.get(0).getString("cloudId"));assertTrue(calls.get(0).getString("jql").contains("assignee = currentUser()"));assertFalse(calls.get(0).has("nextPageToken"));for(String scope:JiraConnection.SCOPE.split(" "))assertFalse(scope.startsWith("write:")||scope.startsWith("delete:")||scope.startsWith("manage:"));
 }
 @Test public void providerErrorsAndUnknownShapesCannotBecomeEmptySuccess()throws Exception{
  try{JiraMcp.unpack(new JSONObject().put("isError",true).put("content",new JSONArray()));fail("Provider error must fail");}catch(IllegalStateException expected){}
  try{JiraMcp.read((name,args)->new JSONObject().put("unexpected","not an issue list"));fail("Unknown issue shape must fail");}catch(IllegalStateException expected){}
  Object value=JiraMcp.unpack(new JSONObject().put("structuredContent",new JSONObject().put("data",new JSONObject().put("issues",new JSONArray()))));assertTrue(value instanceof JSONObject);assertEquals(0,((JSONObject)value).getJSONArray("issues").length());
 }
}

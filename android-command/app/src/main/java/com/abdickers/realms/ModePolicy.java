package com.abdickers.realms;

import java.time.*;
import java.util.*;

/** Pure policy; no radio reads, network requests, or Android dependencies. */
public final class ModePolicy {
 public record Decision(String realm,String reason) {}
 public record Rules(Set<String> homeNetworks,Set<String> workNetworks,Set<DayOfWeek> workDays,int startMinute,int endMinute,String fallback) {}
 public static Decision choose(String mode,String ssid,ZonedDateTime now,Rules rules) {
  if ("home".equals(mode)||"work".equals(mode)) return new Decision(mode,"Manual override");
  String name=normalizeSsid(ssid);
  boolean home=!name.isEmpty()&&rules.homeNetworks.contains(name),work=!name.isEmpty()&&rules.workNetworks.contains(name);
  if(home&&!work)return new Decision("home","Home Wi-Fi");
  if(work&&!home)return new Decision("work","Work Wi-Fi");
  int minute=now.getHour()*60+now.getMinute();
  // Overnight shifts belong to the weekday on which the shift starts.
  boolean overnight=rules.startMinute>rules.endMinute;
  DayOfWeek day=overnight&&minute<rules.endMinute?now.minusDays(1).getDayOfWeek():now.getDayOfWeek();
  boolean window=rules.startMinute==rules.endMinute?false:overnight?(minute>=rules.startMinute||minute<rules.endMinute):(minute>=rules.startMinute&&minute<rules.endMinute);
  boolean scheduled=rules.workDays.contains(day)&&window;
  return new Decision(scheduled?"work":rules.fallback,scheduled?"Work schedule":"Default outside work hours");
 }
 public static String normalizeSsid(String raw) {
  if(raw==null||raw.equals("<unknown ssid>"))return "";
  if(raw.length()>=2&&raw.startsWith("\"")&&raw.endsWith("\""))return raw.substring(1,raw.length()-1);
  return raw;
 }
 public static Set<String> networks(String lines){Set<String> result=new HashSet<>();for(String line:lines.split("\\R")){String s=normalizeSsid(line.trim());if(!s.isEmpty())result.add(s);}return Set.copyOf(result);}
 private ModePolicy(){}
}

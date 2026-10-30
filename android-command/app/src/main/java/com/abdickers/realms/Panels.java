package com.abdickers.realms;
import java.util.*;
final class Panels {
 static final List<String> ALL=List.of("overview","beacons","systems","calendar","jira","slack","mail","priorities","notes","tools","ai");
 static String title(String key){return switch(key){case "overview"->"Overview";case "beacons"->"Beacons of Production";case "systems"->"Catalyst projects";case "calendar"->"Today’s council";case "jira"->"Jira quests";case "slack"->"The Fellowship · Slack";case "mail"->"The Ravens";case "priorities"->"My priorities";case "notes"->"Field notes";case "tools"->"Tools & Systems";case "ai"->"AI council";default->"Overview";};}
 static boolean local(String key){return Set.of("priorities","notes","tools").contains(key);}
 private Panels(){}
}

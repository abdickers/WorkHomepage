package com.abdickers.realms;
import java.util.*;
final class Panels {
 static final List<String> ALL=List.of("overview","weather","hearth","camera","beacons","systems","watch","calendar","jira","slack","mail","priorities","notes","tools","ai");
 static List<String> forRealm(String realm){return realm.equals("work")?ALL.stream().filter(p->!Set.of("weather","hearth","camera").contains(p)).collect(java.util.stream.Collectors.toList()):List.of("overview","weather","hearth","camera","calendar","mail","priorities","notes","tools","ai","watch");}
 static String title(String key){return switch(key){case "weather"->"Weather";case "hearth"->"The hearth";case "camera"->"Watch the gate";case "overview"->"Overview";case "beacons"->"Beacons of Production";case "systems"->"Catalyst projects";case "watch"->"The Watch · service status";case "calendar"->"Today’s council";case "jira"->"Jira quests";case "slack"->"The Fellowship · Slack";case "mail"->"The Ravens";case "priorities"->"My priorities";case "notes"->"Field notes";case "tools"->"Tools & Systems";case "ai"->"AI council";default->"Overview";};}
 static boolean local(String key){return Set.of("priorities","notes","tools").contains(key);}
 private Panels(){}
}

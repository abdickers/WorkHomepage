package com.abdickers.realms;
import org.junit.Test;
import java.time.*;
import java.util.*;
import static org.junit.Assert.*;

public class ModePolicyTest {
 final ModePolicy.Rules rules=new ModePolicy.Rules(Set.of("Cabin"),Set.of("Office"),Set.of(DayOfWeek.MONDAY,DayOfWeek.FRIDAY),540,1020,"home");
 ZonedDateTime at(String iso){return ZonedDateTime.parse(iso+"[America/Chicago]");}
 @Test public void manualOverrideWinsOverNetworkAndSchedule(){assertEquals("home",ModePolicy.choose("home","Office",at("2026-10-30T12:00-05:00"),rules).realm());assertEquals("work",ModePolicy.choose("work","Cabin",at("2026-10-31T12:00-05:00"),rules).realm());}
 @Test public void knownNetworksWinOverHours(){assertEquals("home",ModePolicy.choose("auto","\"Cabin\"",at("2026-10-30T12:00-05:00"),rules).realm());assertEquals("work",ModePolicy.choose("auto","Office",at("2026-10-31T20:00-05:00"),rules).realm());}
 @Test public void unavailableNetworkFallsBackToSchedule(){assertEquals("work",ModePolicy.choose("auto","<unknown ssid>",at("2026-10-30T09:00-05:00"),rules).realm());assertEquals("home",ModePolicy.choose("auto",null,at("2026-10-30T17:00-05:00"),rules).realm());assertEquals("home",ModePolicy.choose("auto","Cafe",at("2026-10-31T12:00-05:00"),rules).realm());}
 @Test public void overnightBelongsToStartDay(){ModePolicy.Rules night=new ModePolicy.Rules(Set.of(),Set.of(),Set.of(DayOfWeek.FRIDAY),1320,360,"home");assertEquals("work",ModePolicy.choose("auto","",at("2026-10-31T02:00-05:00"),night).realm());assertEquals("home",ModePolicy.choose("auto","",at("2026-10-30T02:00-05:00"),night).realm());assertEquals("home",ModePolicy.choose("auto","",at("2026-10-31T06:00-05:00"),night).realm());}
 @Test public void daylightSavingUsesLocalWallClock(){assertEquals("home",ModePolicy.choose("auto","",at("2026-11-01T01:30-05:00"),rules).realm());assertEquals("home",ModePolicy.choose("auto","",at("2026-11-01T01:30-06:00"),rules).realm());}
 @Test public void ambiguousNetworkFallsBackInsteadOfPickingWrongRealm(){ModePolicy.Rules both=new ModePolicy.Rules(Set.of("Shared"),Set.of("Shared"),Set.of(),540,1020,"home");assertEquals("home",ModePolicy.choose("auto","Shared",at("2026-10-30T12:00-05:00"),both).realm());}
 @Test public void ssidMatchingIsExactAndCaseSensitive(){assertEquals("home",ModePolicy.choose("auto","office",at("2026-10-31T12:00-05:00"),rules).realm());assertEquals(Set.of("Cabin","Office"),ModePolicy.networks("Cabin\nOffice\n\nCabin"));}
}

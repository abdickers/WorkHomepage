export type AgendaEvent = {
  summary?: string;
  title?: string;
  start?: { dateTime?: string; date?: string } | string;
  end?: { dateTime?: string; date?: string } | string;
  htmlLink?: string;
  url?: string;
  display_url?: string;
  status?: string;
};

/** Provider results may be structured or wrapped in MCP text blocks. */
export function parseEvents(result: unknown): AgendaEvent[] {
  function walk(data: unknown, depth = 0): AgendaEvent[] | null {
    if (depth > 8 || data == null) return null;
    if (typeof data === "string") {
      try {
        return walk(JSON.parse(data), depth + 1);
      } catch {
        return null;
      }
    }
    if (Array.isArray(data)) {
      if (
        data.every(
          (x) => x && typeof x === "object" && ("start" in x || "summary" in x),
        )
      )
        return data;
      for (const item of data) {
        const found = walk(item, depth + 1);
        if (found !== null) return found;
      }
    } else if (typeof data === "object") {
      const record = data as Record<string, unknown>;
      for (const key of [
        "events",
        "items",
        "structuredContent",
        "data",
        "result",
        "content",
        "text",
      ]) {
        if (key in record) {
          const found = walk(record[key], depth + 1);
          if (found !== null) return found;
        }
      }
    }
    return null;
  }
  const events = walk(result);
  if (events === null)
    throw new Error(
      "Calendar returned an unfamiliar response. Open Google Calendar to review your day.",
    );
  return events.filter((e) => e.status !== "cancelled");
}

export function eventTime(event: AgendaEvent): string {
  const start =
    typeof event.start === "string" ? event.start : event.start?.dateTime;
  const end = typeof event.end === "string" ? event.end : event.end?.dateTime;
  if (!start || /^\d{4}-\d{2}-\d{2}$/.test(start)) return "All day";
  // The Calendar connector serializes all-day events as timezone-less midnight.
  if (
    /T00:00:00$/.test(start) &&
    end &&
    /T00:00:00$/.test(end) &&
    end.slice(0, 10) > start.slice(0, 10)
  )
    return "All day";
  if (!/(Z|[+-]\d{2}:\d{2})$/.test(start)) {
    const match = start.match(/T(\d{2}):(\d{2})/);
    if (!match) return "Time unavailable";
    return `${Number(match[1]) % 12 || 12}:${match[2]} ${Number(match[1]) < 12 ? "AM" : "PM"}`;
  }
  const date = new Date(start);
  return Number.isNaN(date.getTime())
    ? "Time unavailable"
    : date.toLocaleTimeString("en-US", {
        timeZone: "America/Chicago",
        hour: "numeric",
        minute: "2-digit",
      });
}

/** Compute midnight boundaries separately: DST days have 23 or 25 hours. */
export function chicagoWindow(day: string): { start: string; end: string } {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(day)) throw new Error("Invalid day");
  const noon = new Date(`${day}T12:00:00Z`);
  if (
    !Number.isFinite(noon.getTime()) ||
    noon.toISOString().slice(0, 10) !== day
  )
    throw new Error("Invalid day");
  const nextDay = new Date(noon.getTime() + 86400000)
    .toISOString()
    .slice(0, 10);
  const midnight = (value: string) => {
    const offset = new Intl.DateTimeFormat("en-US", {
      timeZone: "America/Chicago",
      timeZoneName: "shortOffset",
    })
      .formatToParts(new Date(`${value}T06:00:00Z`))
      .find((x) => x.type === "timeZoneName")?.value;
    const match = offset?.match(/^GMT([+-])(\d{1,2})(?::(\d{2}))?$/);
    if (!match) throw new Error("Timezone unavailable");
    return new Date(
      `${value}T00:00:00${match[1]}${match[2].padStart(2, "0")}:${match[3] || "00"}`,
    ).toISOString();
  };
  return { start: midnight(day), end: midnight(nextDay) };
}

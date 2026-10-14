import { test } from "node:test";
import assert from "node:assert/strict";
import {
  parseEvents,
  eventTime,
  chicagoWindow,
} from "../lib/dashboard-data.mts";
test("reads structured events, ignores cancelled entries, preserves text safely", () => {
  const events = parseEvents({
    structuredContent: {
      events: [
        {
          summary: "<script>example</script>",
          start: "2026-10-14T09:15:00-05:00",
        },
        { summary: "Cancelled", status: "cancelled" },
      ],
    },
  });
  assert.equal(events.length, 1);
  assert.equal(events[0].summary, "<script>example</script>");
});
test("recognizes empty success and nested MCP JSON without inventing empty success", () => {
  assert.deepEqual(parseEvents({ structuredContent: { events: [] } }), []);
  assert.equal(
    parseEvents({
      content: [
        {
          type: "text",
          text: JSON.stringify({ events: [{ summary: "Meeting" }] }),
        },
      ],
    }).length,
    1,
  );
  assert.throws(() =>
    parseEvents({ content: [{ type: "text", text: "Unavailable" }] }),
  );
});
test("all-day events and timed events use the correct labels", () => {
  assert.equal(
    eventTime({ start: "2026-10-14T00:00:00", end: "2026-10-15T00:00:00" }),
    "All day",
  );
  assert.equal(eventTime({ start: { date: "2026-10-14" } }), "All day");
  assert.equal(eventTime({ start: "2026-10-14T09:15:00-05:00" }), "9:15 AM");
  assert.equal(eventTime({ start: "2026-10-14T09:15:00" }), "9:15 AM");
});
test("Chicago calendar windows account for DST transitions", () => {
  const duration = (day) => {
    const w = chicagoWindow(day);
    return (Date.parse(w.end) - Date.parse(w.start)) / 3600000;
  };
  assert.equal(duration("2026-03-08"), 23);
  assert.equal(duration("2026-11-01"), 25);
  assert.equal(duration("2026-10-14"), 24);
  assert.throws(() => chicagoWindow("2026-02-31"));
});

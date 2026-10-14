import { test } from "node:test";
import assert from "node:assert/strict";
import { providerData, records, maskLog } from "../lib/provider-data.mts";
test("Celigo MCP text blocks and structured results retain pagination", () => {
  const result = {
    content: [
      {
        type: "text",
        text: JSON.stringify({
          items: [{ name: "Flow", numOpenError: 4 }],
          hasMore: true,
        }),
      },
    ],
  };
  assert.equal(records(result)[0].numOpenError, 4);
  assert.equal(providerData(result).hasMore, true);
  assert.equal(
    records({ structuredContent: { events: [{ timestamp: 1 }] } }, "events")
      .length,
    1,
  );
});
test("unavailable or malformed results cannot become an empty healthy list", () => {
  assert.throws(() =>
    records({ content: [{ type: "text", text: "Permission denied" }] }),
  );
  assert.throws(() =>
    records({ isError: true, structuredContent: { items: [] } }),
  );
  assert.throws(() => records({ other: [] }));
  assert.deepEqual(records({ structuredContent: { items: [] } }), []);
});
test("log display masks auth callback parameters and bearer credentials", () => {
  assert.equal(
    maskLog("/callback?code=abc&state=xyz&x=1"),
    "/callback?code=[redacted]&state=[redacted]&x=1",
  );
  assert.equal(maskLog("Bearer abc.def-123"), "Bearer [redacted]");
  assert.ok(maskLog("a".repeat(2000)).length <= 1600);
});

test("hosted MCP null structuredContent falls back to the text payload", () => {
  assert.equal(
    records({
      structuredContent: null,
      content: [
        { type: "text", text: '{"items":[{"name":"Production flow"}]}' },
      ],
    })[0].name,
    "Production flow",
  );
});

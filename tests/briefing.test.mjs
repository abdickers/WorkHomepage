import { test } from "node:test";
import assert from "node:assert/strict";
import { buildEvidence, briefingRequest } from "../lib/briefing-data.mts";
import { signSnapshot, verifySnapshot } from "../lib/briefing-token.mts";
const ok = (value) => ({
  status: "success",
  result: { structuredContent: value },
});
const at = "2026-10-15T12:00:00Z";
test("briefing allowlist excludes customer data, flow names, logs and credentials", () => {
  const evidence = buildEvidence(
    {
      flows: ok({
        items: [
          {
            name: "Private customer",
            numOpenError: 9,
            secret: "private-key",
            disabled: false,
          },
        ],
        hasMore: true,
      }),
      jobs: ok({
        items: [
          {
            status: "running",
            payload: { customerEmail: "private@example.com" },
          },
        ],
      }),
      logs: ok({
        events: [
          {
            source: { message: "secret request body" },
            $workers: {
              event: {
                path: "/callback?code=private",
                response: { status: 503 },
              },
              outcome: "exception",
            },
          },
        ],
      }),
    },
    at,
  );
  const text = JSON.stringify(briefingRequest(evidence, "test-model"));
  for (const secret of [
    "Private customer",
    "private-key",
    "private@example.com",
    "secret request body",
    "callback?code",
  ])
    assert.ok(!text.includes(secret));
  assert.equal(evidence[0].facts.openErrors, 9);
  assert.equal(evidence[0].facts.hasMore, true);
  assert.equal(evidence[1].facts.running, 1);
  assert.equal(evidence[2].facts.http5xxInSample, 1);
  assert.match(evidence[2].caveat, /not total traffic/);
});
test("missing sources and unknown counts never become healthy zeroes", () => {
  const evidence = buildEvidence(
    {
      flows: ok({ items: [{ numOpenError: null }] }),
      jobs: { status: "tool_error" },
      logs: ok({ bad: [] }),
    },
    at,
  );
  assert.equal(evidence[0].facts.openErrors, null);
  assert.equal(evidence[0].facts.missingErrorCounts, 1);
  assert.equal(evidence[1].state, "unavailable");
  assert.equal(evidence[2].state, "unavailable");
  assert.deepEqual(evidence[2].facts, {});
});
test("signed snapshots reject tampering, expiry, another identity, and another key", async () => {
  const now = Date.now();
  const payload = { userId: "owner", evidence: [], expiresAt: now + 300000 };
  const signed = await signSnapshot(payload, "fixture-secret");
  assert.deepEqual(
    await verifySnapshot(
      signed.body,
      signed.signature,
      "fixture-secret",
      "owner",
      now,
    ),
    payload,
  );
  await assert.rejects(
    verifySnapshot(
      signed.body.replace("owner", "other"),
      signed.signature,
      "fixture-secret",
      "other",
      now,
    ),
  );
  await assert.rejects(
    verifySnapshot(
      signed.body,
      signed.signature,
      "fixture-secret",
      "other",
      now,
    ),
  );
  await assert.rejects(
    verifySnapshot(signed.body, signed.signature, "other-secret", "owner", now),
  );
  await assert.rejects(
    verifySnapshot(
      signed.body,
      signed.signature,
      "fixture-secret",
      "owner",
      now + 300001,
    ),
  );
});

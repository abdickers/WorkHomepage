import { test } from "node:test";
import assert from "node:assert/strict";
import {
  invoiceRecipient,
  invoiceCandidate,
  treasuryTotals,
  slackMessages,
  celigoFlowUrl,
  INVOICE_QUERY,
} from "../lib/daily-data.mts";
const email = {
  id: "abc123",
  subject: "Invoice",
  display_url:
    "https://mail.google.com/mail/u/?authuser=dickersona%4076fireworks.com#all/abc123",
  snippet: "Invoice ID: INV-100 Amount: $25.00 USD Due on: October 9, 2026",
};
const message = (to = "Adam <dickersona@76fireworks.com>") => ({
  payload: {
    headers: [
      { name: "To", value: to },
      { name: "From", value: "Vendor <billing@vendor.com>" },
    ],
    mime_type: "text/plain",
    body: { content: email.snippet },
  },
});
test("invoice intake uses only exact authorized original To recipients", () => {
  assert.equal(
    invoiceRecipient("IT <itservices@76fireworks.com>, other@example.com"),
    true,
  );
  assert.equal(invoiceRecipient("otherdickersona@76fireworks.com"), false);
  assert.equal(invoiceRecipient("iteservices@76fireworks.com"), false);
  assert.equal(invoiceCandidate(email, message("other@example.com")), null);
  assert.match(INVOICE_QUERY, /to:itservices@76fireworks.com/);
});
test("extracts deterministic candidates but keeps financial inclusion pending review", () => {
  const c = invoiceCandidate(email, message());
  assert.equal(c.amountCents, 2500);
  assert.equal(c.currency, "USD");
  assert.equal(c.dueDate, "2026-10-09");
  assert.equal(c.status, "review");
  assert.equal(c.invoiceNumber, "INV-100");
  assert.equal(
    invoiceCandidate({ ...email, id: "def456" }, message()).fingerprint,
    c.fingerprint,
  );
  const second = invoiceCandidate(
    { ...email, snippet: email.snippet.replace("INV-100", "INV-101") },
    message(),
  );
  assert.notEqual(second.fingerprint, c.fingerprint);
  assert.equal(
    invoiceCandidate(
      { ...email, display_url: "javascript:alert(1)" },
      message(),
    ),
    null,
  );
});
test("month budget includes confirmed and paid, excludes pending, excluded, other currencies and other months", () => {
  const row = {
    status: "confirmed",
    currency: "USD",
    amountCents: 10000,
    dueDate: "2026-10-09",
  };
  const totals = treasuryTotals(
    [
      row,
      { ...row, status: "paid" },
      { ...row, status: "review" },
      { ...row, status: "excluded" },
      { ...row, currency: "CAD" },
      { ...row, dueDate: "2026-09-09" },
      { ...row, dueDate: null },
    ],
    "2026-10",
    "2026-10-15",
  );
  assert.deepEqual(totals, {
    committed: 20000,
    remaining: 480000,
    overdue: 20000,
    review: 1,
  });
  assert.equal(
    treasuryTotals([{ ...row, amountCents: 600000 }], "2026-10", "2026-10-15")
      .remaining,
    -100000,
  );
});
test("Slack parser permits only source workspace permalinks and plain text", () => {
  const block =
    "### Result 1 of 1\nChannel: DM\nFrom: Someone\nPermalink: [link](https://76fireworks.slack.com/archives/D123/p123456789)\nText: \n<script>plain text</script>\n---";
  assert.equal(
    slackMessages({ results: block })[0].text,
    "<script>plain text</script> ",
  );
  assert.equal(
    slackMessages({
      results: block.replace("76fireworks.slack.com", "evil.example"),
    }).length,
    0,
  );
  assert.throws(() => slackMessages({}));
});
test("Celigo links validate IDs and use the observed section/builder route", () => {
  assert.equal(
    celigoFlowUrl({
      _id: "a".repeat(24),
      _integrationId: "b".repeat(24),
      _flowGroupingId: "c".repeat(24),
    }),
    `https://integrator.io/integrations/${"b".repeat(24)}/flows/sections/${"c".repeat(24)}/flowBuilder/${"a".repeat(24)}`,
  );
  assert.equal(celigoFlowUrl({ _id: "malicious" }), null);
});

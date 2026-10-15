export const INVOICE_RECIPIENTS = [
  "dickersona@76fireworks.com",
  "iteservices@76fireworks.com",
];
export const INVOICE_QUERY =
  'newer_than:90d -in:spam -in:trash -in:sent {to:dickersona@76fireworks.com to:iteservices@76fireworks.com} {subject:invoice subject:"payment due" subject:"amount due"}';
export function safeSource(value: unknown, host: string) {
  try {
    const u = new URL(String(value));
    return u.protocol === "https:" &&
      u.hostname === host &&
      !u.username &&
      !u.password
      ? u.href
      : null;
  } catch {
    return null;
  }
}
export function chicagoDay(now = new Date()) {
  return now.toLocaleDateString("en-CA", { timeZone: "America/Chicago" });
}
export function celigoFlowUrl(flow: any) {
  const id = (v: unknown) => typeof v === "string" && /^[a-f0-9]{24}$/i.test(v);
  return id(flow._integrationId) && id(flow._flowGroupingId) && id(flow._id)
    ? `https://integrator.io/integrations/${flow._integrationId}/flows/sections/${flow._flowGroupingId}/flowBuilder/${flow._id}`
    : null;
}
export function invoiceRecipient(to: string) {
  const addresses: string[] =
    to.toLowerCase().match(/[a-z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-z0-9.-]+/g) || [];
  return INVOICE_RECIPIENTS.some((a) => addresses.includes(a));
}
export function validDay(value: unknown): value is string {
  if (typeof value !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(value))
    return false;
  const d = new Date(value + "T12:00:00Z");
  return Number.isFinite(+d) && d.toISOString().slice(0, 10) === value;
}
function dateValue(value: string) {
  const n = value.match(/^(\d{1,2})\/(\d{1,2})\/(\d{4})/);
  const iso = n
    ? `${n[3]}-${n[1].padStart(2, "0")}-${n[2].padStart(2, "0")}`
    : (() => {
        const d = new Date(value);
        return Number.isFinite(+d) ? d.toISOString().slice(0, 10) : "";
      })();
  return validDay(iso) ? iso : null;
}
export function mimeText(payload: any, depth = 0): string {
  if (!payload || depth > 8) return "";
  const type = payload.mime_type || payload.mimeType || "";
  let own =
    /^text\/(plain|html)/i.test(type) &&
    typeof payload.body?.content === "string"
      ? payload.body.content.slice(0, 100000)
      : "";
  if (/html/i.test(type))
    own = own
      .replace(/<(script|style)\b[^>]*>[\s\S]*?<\/\1>/gi, " ")
      .replace(/<[^>]*>/g, " ")
      .replace(/&nbsp;/gi, " ")
      .replace(/&amp;/gi, "&")
      .replace(/&#36;/g, "$");
  return [
    own,
    ...(payload.parts || [])
      .slice(0, 30)
      .map((p: any) => mimeText(p, depth + 1)),
  ]
    .join("\n")
    .slice(0, 160000);
}
export function invoiceCandidate(email: any, message: any) {
  const headers = message?.payload?.headers || [];
  const header = (name: string) =>
    String(
      headers.find((h: any) => String(h.name).toLowerCase() === name)?.value ||
        "",
    );
  // The original To header is authoritative. A forwarded body mentioning an address is insufficient.
  const recipient = header("to");
  if (!invoiceRecipient(recipient)) return null;
  const subject = (header("subject") || email.subject || "").slice(0, 400);
  const sender = (header("from") || email.from_ || "").slice(0, 300);
  const text =
    `${subject}\n${email.snippet || ""}\n${mimeText(message.payload)}`.replace(
      /\s+/g,
      " ",
    );
  const invoiceNumber =
    text.match(
      /\bInvoice\s*(?:ID|Number|No\.?|#)\s*[:#]?\s*([a-z0-9][a-z0-9_-]{2,60})/i,
    )?.[1] ||
    text.match(/\binvoice\s*\(#([a-z0-9_-]{3,60})\)/i)?.[1] ||
    null;
  const vendor = (
    sender
      .match(/^[^<]+(?=\s*<)/)?.[0]
      ?.replace(/["']/g, "")
      .trim() ||
    sender.match(/@([a-z0-9.-]+)/i)?.[1] ||
    sender ||
    "Unknown vendor"
  ).slice(0, 160);
  const amount = text.match(
    /(?:amount\s*due|balance\s*due|invoice\s*total|total\s*due|amount)\s*[:\-]?\s*(?:USD\s*)?\$\s*([\d,]+\.\d{2})(?:\s*(USD|CAD|AUD))?/i,
  );
  const receipt = /payment received|paid on|receipt for|invoice.*paid/i.test(
    subject + " " + (email.snippet || ""),
  );
  const fallback = receipt ? text.match(/\$\s*([\d,]+\.\d{2})/) : null;
  const rawAmount = amount?.[1] || fallback?.[1];
  const amountCents = rawAmount
    ? Math.round(Number(rawAmount.replace(/,/g, "")) * 100)
    : null;
  const due = text.match(
    /(?:due\s*(?:on|date)|date\s*due)\s*[:\-]?\s*(\d{1,2}\/\d{1,2}\/\d{4}|\d{4}-\d{2}-\d{2}|[a-z]+\s+\d{1,2},?\s+\d{4})/i,
  )?.[1];
  const receivable =
    /you sent an invoice|invoice was paid by|customer paid|paid by .+!/.test(
      subject.toLowerCase(),
    );
  const fingerprint = invoiceNumber
    ? `${vendor.toLowerCase()}|${invoiceNumber.toLowerCase()}`
    : `message:${email.id}`;
  const sourceUrl = safeSource(email.display_url, "mail.google.com");
  if (!sourceUrl || !/^[a-f0-9]+$/i.test(String(email.id))) return null;
  return {
    fingerprint,
    vendor,
    invoiceNumber,
    amountCents,
    currency: amount?.[2] || (/\bUSD\b/.test(text) ? "USD" : ""),
    dueDate: due ? dateValue(due) : null,
    messageId: email.id,
    sourceUrl,
    recipient: recipient.slice(0, 600),
    subject,
    receivedAt: email.email_ts || "",
    status: "review",
    note: receivable
      ? "Possible customer invoice / incoming payment: exclude unless this is your expense."
      : receipt
        ? "Payment receipt: verify invoice amount and allocation date; may duplicate an earlier bill."
        : amountCents === 0
          ? "Zero balance due does not establish the original invoice amount. Verify before confirming."
          : "Verify vendor, amount, currency, and due date before confirming.",
  };
}
export function treasuryTotals(rows: any[], month: string, today: string) {
  const counted = rows.filter(
    (r) =>
      ["confirmed", "paid"].includes(r.status) &&
      r.currency === "USD" &&
      Number.isInteger(r.amountCents) &&
      r.amountCents >= 0 &&
      validDay(r.dueDate),
  );
  const committed = counted
    .filter((r) => r.dueDate.startsWith(month))
    .reduce((n, r) => n + r.amountCents, 0);
  return {
    committed,
    remaining: 500000 - committed,
    overdue: counted
      .filter((r) => r.status !== "paid" && r.dueDate < today)
      .reduce((n, r) => n + r.amountCents, 0),
    review: rows.filter((r) => r.status === "review").length,
  };
}
export function slackMessages(data: any) {
  if (typeof data.results !== "string")
    throw Error("Slack returned an unfamiliar response.");
  return data.results
    .split(/### Result \d+ of \d+\s*\n/)
    .slice(1, 21)
    .flatMap((block: string) => {
      const url = block.match(
        /Permalink:\s*\[link\]\((https:\/\/[^\s)]+)\)/,
      )?.[1];
      const href = safeSource(url, "76fireworks.slack.com");
      if (!href || !/\/archives\/[A-Z0-9]+\/p\d+$/.test(new URL(href).pathname))
        return [];
      return [
        {
          id: href,
          href,
          title:
            block.match(/^From:\s*(.+)$/m)?.[1]?.slice(0, 160) ||
            "Slack message",
          meta:
            block.match(/^Channel:\s*(.+)$/m)?.[1]?.slice(0, 120) || "Slack",
          text: (block.split(/Text:\s*\n/)[1] || "")
            .replace(/\n---\s*$/, " ")
            .slice(0, 500),
        },
      ];
    });
}

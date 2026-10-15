import { requireWorkGoogle } from "@/lib/google-account";
import { and, eq } from "drizzle-orm";
import { invoices } from "@/db/schema";
import { getDb } from "@/db";
import { dailyOwner, privateHeaders } from "@/lib/daily-server";
import { connectorsForRequest } from "@/lib/connectors";
import { providerData } from "@/lib/provider-data.mjs";
import {
  INVOICE_QUERY,
  invoiceCandidate,
  validDay,
} from "@/lib/daily-data.mjs";
import { chatGPTSignInPath } from "@/app/chatgpt-auth";
const GMAIL = "connector_2128aebfecb84f64a069897515042a44";
export async function POST(request: Request) {
  const auth = await dailyOwner(request);
  if (auth.error) return auth.error;
  const ownerId = auth.user!.userId;
  let body: any;
  try {
    body = await request.json();
  } catch {
    return Response.json(
      { status: "invalid_request", message: "Invalid request." },
      { status: 400, headers: privateHeaders },
    );
  }
  const fail = (message: string, status = 400) =>
    Response.json(
      { status: "invalid_request", message },
      { status, headers: privateHeaders },
    );
  try {
    const db = getDb();
    let scan: any = {};
    if (body.action === "scan") {
      if (
        body.cursor !== undefined &&
        (typeof body.cursor !== "string" || body.cursor.length > 3000)
      )
        return fail("Invalid scan cursor.");
      const accountError = await requireWorkGoogle(GMAIL);
      if (accountError) return Response.json(accountError, {headers:privateHeaders});
      const client = connectorsForRequest();
      const result = await client.invoke(GMAIL, "search_emails", {
        query: INVOICE_QUERY,
        max_results: 10,
        ...(body.cursor ? { next_page_token: body.cursor } : {}),
      });
      const providerFailure = (r: any) =>
        Response.json(
          {
            ...r,
            ...(r.status === "reauthentication_required"
              ? { chatGPTSignInPath: chatGPTSignInPath("/") }
              : {}),
          },
          { headers: privateHeaders },
        );
      if (result.status !== "success") return providerFailure(result);
      const data = providerData(result.result);
      if (!Array.isArray(data.emails)) throw Error();
      const emails = data.emails
        .slice(0, 10)
        .filter((e: any) => /^[a-f0-9]+$/i.test(String(e.id)));
      let candidates: any[] = [];
      if (emails.length) {
        const detail = await client.invoke(GMAIL, "batch_read_email", {
          message_ids: emails.map((e: any) => e.id),
        });
        if (detail.status !== "success") return providerFailure(detail);
        const full = providerData(detail.result);
        if (!Array.isArray(full.responses)) throw Error();
        candidates = emails.flatMap((e: any) => {
          const message = full.responses.find((m: any) => m.id === e.id);
          const candidate = message ? invoiceCandidate(e, message) : null;
          return candidate ? [candidate] : [];
        });
        for (const c of candidates)
          await db
            .insert(invoices)
            .values({ ...c, id: crypto.randomUUID(), ownerId })
            .onConflictDoNothing();
      }
      scan = {
        cursor: data.next_page_token || null,
        examined: emails.length,
        matched: candidates.length,
      };
    } else if (body.action === "update") {
      if (
        typeof body.id !== "string" ||
        !["review", "confirmed", "paid", "excluded"].includes(body.status)
      )
        return fail("Invalid invoice.");
      if (
        typeof body.vendor !== "string" ||
        !body.vendor.trim() ||
        body.vendor.length > 160
      )
        return fail("Enter a vendor.");
      if (
        ![null, undefined].includes(body.amountCents) &&
        (!Number.isInteger(body.amountCents) ||
          body.amountCents < 0 ||
          body.amountCents > 1000000000)
      )
        return fail("Enter a valid invoice amount.");
      if (
        typeof body.currency !== "string" ||
        !/^(?:[A-Z]{3})?$/.test(body.currency)
      )
        return fail("Use a three-letter currency.");
      if (body.dueDate !== null && !validDay(body.dueDate))
        return fail("Enter a valid due / allocation date.");
      if (
        ["confirmed", "paid"].includes(body.status) &&
        (!Number.isInteger(body.amountCents) || !body.currency || !body.dueDate)
      )
        return fail("Verify amount, currency, and date before confirming.");
      await db
        .update(invoices)
        .set({
          vendor: body.vendor.trim(),
          amountCents: body.amountCents ?? null,
          currency: body.currency,
          dueDate: body.dueDate,
          status: body.status,
        })
        .where(and(eq(invoices.ownerId, ownerId), eq(invoices.id, body.id)));
    } else if (body.action !== "list") return fail("Unknown ledger action.");
    const rows = await db
      .select()
      .from(invoices)
      .where(eq(invoices.ownerId, ownerId))
      .orderBy(invoices.dueDate)
      .limit(1000);
    return Response.json(
      {
        status: "success",
        rows,
        ...scan,
        limited: rows.length === 1000,
        fetchedAt: new Date().toISOString(),
      },
      { headers: privateHeaders },
    );
  } catch {
    return Response.json(
      {
        status: "unavailable",
        message:
          "The ledger could not complete this request. Refresh to check its saved state before retrying.",
      },
      { status: 503, headers: privateHeaders },
    );
  }
}

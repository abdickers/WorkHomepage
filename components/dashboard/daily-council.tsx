"use client";
import { PanelFrame } from "@/components/dashboard/panel-frame";
import { connectionEnabled, useConnectionEnabled } from "./connection-preferences";
import { PolishArt } from "@/components/dashboard/polish-art";
import { GondorArt, type GondorArtName } from "@/components/dashboard/gondor-art";
import { accountLink, WORK_EMAIL } from "@/lib/account-links.mjs";
import { useEffect, useRef, useState } from "react";
import { MessageSquare } from "lucide-react";
import { Bird, RefreshCw, ArrowUpRight, Mail, ShieldAlert } from "@/components/dashboard/realm-icon";
import { chicagoDay, treasuryTotals } from "@/lib/daily-data.mjs";
type Entry = {
  id: string;
  href: string;
  title: string;
  meta: string;
  text: string;
};
type Feed = {
  status: string;
  items?: Entry[];
  message?: string;
  more?: boolean;
  chatGPTSignInPath?: string;
  retryAfterMs?: number;
  stale?: boolean;
};
function LoadState({ feed }: { feed: Feed | undefined }) {
  if (!feed)
    return <p className="daily-empty">The ravens have not returned yet.</p>;
  if (feed.status === "success") return null;
  return (
    <p className="daily-error">
      {feed.message || "Source unavailable."}{" "}
      {feed.stale && "Showing a previous snapshot."}{" "}
      {feed.status === "reauthentication_required" &&
        feed.chatGPTSignInPath && (
          <a href={feed.chatGPTSignInPath}>Connect →</a>
        )}
    </p>
  );
}
function FeedLinks({ feed, art }: { feed: Feed | undefined; art: GondorArtName }) {
  return (
    <>
      <LoadState feed={feed} />
      {(feed?.status === "success" || feed?.stale) && (
        <>
          <div className="daily-list">
            {feed.items?.map((item) => (
              <a
                className="daily-entry"
                href={accountLink(item.href, WORK_EMAIL)}
                target="_blank"
                rel="noopener noreferrer"
                key={item.id}
              >
                <span
                  className={`feed-illustration ${item.text === "Highest" || item.text === "High" ? "urgent" : ""}`}
                  aria-hidden="true"
                >
                  <GondorArt name={art} size={32} />
                </span>
                <span>
                  <small>{item.meta}</small>
                  <strong>{item.title}</strong>
                  <p>{item.text}</p>
                </span>
                <ArrowUpRight size={14} />
              </a>
            ))}
          </div>
          {!feed.items?.length && (
            <p className="daily-empty">No matching items in this search.</p>
          )}
          {feed.more && (
            <p className="daily-footnote">
              Latest matching results shown; more may exist.
            </p>
          )}
        </>
      )}
    </>
  );
}
function useDaily(source: "mail" | "slack" | "jira", watch: boolean) {
  const enabled = useConnectionEnabled(source);
  const [data, setData] = useState<any>(null),
    [busy, setBusy] = useState(false),
    [error, setError] = useState("");
  const active = useRef(false),
    retryAt = useRef(0);
  async function refresh() {
    if (!connectionEnabled(source)) return;
    if (active.current) return;
    if (Date.now() < retryAt.current) {
      setError("Please wait a moment before refreshing this source again.");
      return;
    }
    active.current = true;
    setBusy(true);
    setError("");
    const controller = new AbortController(),
      timer = setTimeout(() => controller.abort(), 65000);
    try {
      const response = await fetch(`/api/daily/${source}`, {
        method: "POST",
        signal: controller.signal,
      });
      const d: any = await response.json();
      if (source === "slack" && d.diagnostics) console.info("Slack Site action diagnostics", JSON.stringify(d.diagnostics));
      if (d.status !== "success") {
        retryAt.current = Date.now() + Math.max(60000, d.retryAfterMs || 0);
        setData((previous: any) => (previous?.fetchedAt ? previous : d));
        throw Error(d.message || "Source unavailable.");
      }
      setData((previous: any) => ({
        ...d,
        feeds: Object.fromEntries(
          Object.entries(d.feeds).map(([key, value]: [string, any]) => [
            key,
            value.status !== "success" && previous?.feeds?.[key]?.items
              ? { ...value, items: previous.feeds[key].items, stale: true }
              : value,
          ]),
        ),
      }));
      const waits = Object.values(d.feeds).map((f: any) => f.retryAfterMs || 0);
      retryAt.current = Date.now() + Math.max(10000, ...waits);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unable to refresh.");
      retryAt.current = Math.max(retryAt.current, Date.now() + 60000);
    } finally {
      clearTimeout(timer);
      active.current = false;
      setBusy(false);
    }
  }
  useEffect(() => {
    refresh();
  }, [source, enabled]);
  useEffect(() => {
    if (!watch || !enabled) return;
    const timer = setInterval(() => {
      if (
        document.visibilityState === "visible" &&
        Date.now() >= retryAt.current
      )
        refresh();
    }, 300000);
    return () => clearInterval(timer);
  }, [source, watch, enabled]);
  return { data, busy, error: enabled ? error : "Reads paused in the Royal Registry. Existing results are retained.", refresh, enabled };
}
function FeedFooter({
  data,
  busy,
  error,
  refresh,
  enabled = true,
}: {
  data: any;
  busy: boolean;
  error: string;
  refresh: () => void;
  enabled?: boolean;
}) {
  return (
    <footer className="daily-feed-footer">
      <span>
        {!enabled ? "Paused in Settings" : error
          ? `Refresh failed${data?.fetchedAt ? " · showing previous snapshot" : ""}`
          : data?.fetchedAt
            ? `${Object.values(data.feeds || {}).some((f: any) => f.status !== "success") ? "Last attempt" : "Read"} ${new Date(data.fetchedAt).toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit", timeZone: "America/Chicago" })} CT`
            : "Read only · refresh on demand"}
      </span>
      <button disabled={busy || !enabled} onClick={refresh} aria-label="Refresh feed">
        <RefreshCw size={13} className={busy ? "spinning" : ""} />{" "}
        {busy ? "Reading…" : "Refresh"}
      </button>
    </footer>
  );
}
function Ravens({ watch }: { watch: boolean }) {
  const mail = useDaily("mail", watch),
    slack = useDaily("slack", watch);
  const [tab, setTab] = useState("inbox");
  const current = tab === "inbox" ? mail : slack;
  const feed = current.data?.feeds?.[tab];
  return (
    <section className="panel daily-panel ravens realm-framed realm-gondor">
      <PanelFrame kind="gondor" />
      <div className="panel-heading">
        <div>
          <PolishArt name="raven" className="ravens-heading-art" />
          <h3>The ravens</h3>
        </div>
        <span className="tag">MESSAGES</span>
      </div>
      <div className="daily-tabs">
        {[
          ["inbox", "Gmail", Mail],
          ["dm", "Slack DMs", MessageSquare],
          ["mentions", "Mentions", ShieldAlert],
        ].map(([key, label, Icon]: any) => (
          <button
            key={key}
            aria-pressed={tab === key}
            onClick={() => setTab(key)}
          >
            <Icon size={14} />
            {label}
            {current.data?.feeds?.[key]?.items && (
              <b>{current.data.feeds[key].items.length}</b>
            )}
          </button>
        ))}
      </div>
      <p className="daily-caption">
        {tab === "inbox"
          ? "Latest unread conversations · promotions/social excluded"
          : "Past 7 days · recent messages, read state unavailable"}
      </p>
      {current.error && (
        <LoadState
          feed={
            current.data?.status !== "success"
              ? current.data
              : { status: "error", message: current.error }
          }
        />
      )}
      <FeedLinks feed={feed} art="notes" />
      <FeedFooter {...current} />
    </section>
  );
}
function Quests({ watch }: { watch: boolean }) {
  const source = useDaily("jira", watch);
  const [tab, setTab] = useState("assigned");
  return (
    <section className="panel daily-panel quests realm-framed realm-gondor">
      <PanelFrame kind="gondor" />
      <div className="panel-heading">
        <div>
          <GondorArt name="quest" />
          <h3>The quest board</h3>
        </div>
        <a
          href="https://76fireworks.atlassian.net/jira/your-work"
          target="_blank"
          rel="noopener noreferrer"
          aria-label="Open Jira"
        >
          <ArrowUpRight size={16} />
        </a>
      </div>
      <div className="daily-tabs">
        <button
          aria-pressed={tab === "assigned"}
          onClick={() => setTab("assigned")}
        >
          My quests
        </button>
        <button
          aria-pressed={tab === "unclaimed"}
          onClick={() => setTab("unclaimed")}
        >
          Unclaimed · urgent
        </button>
      </div>
      <p className="daily-caption">
        {tab === "assigned"
          ? "Assigned to you · unfinished · highest priority first"
          : "Unassigned High / Highest · To Do · all accessible projects"}
      </p>
      {source.error && (
        <LoadState
          feed={
            source.data?.status !== "success"
              ? source.data
              : { status: "error", message: source.error }
          }
        />
      )}
      <FeedLinks feed={source.data?.feeds?.[tab]} art="quest" />
      <FeedFooter {...source} />
    </section>
  );
}
const money = (cents: number) =>
  new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    maximumFractionDigits: 2,
  }).format(cents / 100);
function InvoiceEditor({
  row,
  busy,
  save,
}: {
  row: any;
  busy: boolean;
  save: (value: any) => Promise<void>;
}) {
  const [vendor, setVendor] = useState(row.vendor),
    [amount, setAmount] = useState(
      row.amountCents == null ? "" : String(row.amountCents / 100),
    ),
    [currency, setCurrency] = useState(row.currency),
    [date, setDate] = useState(row.dueDate || "");
  const value = (status: string) => ({
    id: row.id,
    vendor,
    amountCents: amount === "" ? null : Math.round(Number(amount) * 100),
    currency: currency.toUpperCase(),
    dueDate: date || null,
    status,
  });
  return (
    <details className="invoice-row">
      <summary>
        <span className={`invoice-coin ${row.status}`} aria-hidden="true">
          <GondorArt name="treasury" size={32} />
        </span>
        <span>
          <strong>{row.vendor}</strong>
          <small>
            {row.invoiceNumber || "Invoice number unavailable"} ·{" "}
            {row.dueDate || "Date needs review"}
          </small>
        </span>
        <span>
          {row.amountCents == null
            ? "?"
            : row.currency === "USD"
              ? money(row.amountCents)
              : `${(row.amountCents / 100).toFixed(2)} ${row.currency || "currency?"}`}
          <small>{row.status}</small>
        </span>
      </summary>
      <div className="invoice-editor">
        <a href={accountLink(row.sourceUrl, WORK_EMAIL)} target="_blank" rel="noopener noreferrer">
          Open original invoice in Gmail <ArrowUpRight size={13} />
        </a>
        <p>{row.subject}</p>
        <small>To: {row.recipient}</small>
        <p>{row.note}</p>
        <label>
          Vendor
          <input
            value={vendor}
            onChange={(e) => setVendor(e.target.value)}
            maxLength={160}
          />
        </label>
        <div className="invoice-fields">
          <label>
            Invoice amount
            <input
              type="number"
              min="0"
              step="0.01"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
            />
          </label>
          <label>
            Currency
            <input
              value={currency}
              onChange={(e) => setCurrency(e.target.value)}
              placeholder="USD"
              maxLength={3}
            />
          </label>
          <label>
            Due / allocation date
            <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
            />
          </label>
        </div>
        <p className="daily-footnote">
          For receipts with no due date, choose the month’s allocation date.
          Confirm only expenses charged to your IT budget.
        </p>
        <div className="invoice-actions">
          <button disabled={busy} onClick={() => save(value("confirmed"))}>
            Confirm expense
          </button>
          <button disabled={busy} onClick={() => save(value("paid"))}>
            Mark paid
          </button>
          <button disabled={busy} onClick={() => save(value("excluded"))}>
            Exclude
          </button>
          <button disabled={busy} onClick={() => save(value("review"))}>
            Save for review
          </button>
        </div>
      </div>
    </details>
  );
}
function Treasury() {
  const mailEnabled = useConnectionEnabled("mail");
  const [rows, setRows] = useState<any[]>([]),
    [loaded, setLoaded] = useState(false),
    [busy, setBusy] = useState(false),
    [error, setError] = useState<any>(null),
    [month, setMonth] = useState(chicagoDay().slice(0, 7)),
    [filter, setFilter] = useState("month"),
    [cursor, setCursor] = useState<string | null>(null),
    [scan, setScan] = useState("");
  const active = useRef(false),
    retryAt = useRef(0);
  async function action(payload: any) {
    if (payload.action === "scan" && !connectionEnabled("mail")) return;
    if (
      active.current ||
      (payload.action === "scan" && Date.now() < retryAt.current)
    )
      return;
    active.current = true;
    setBusy(true);
    setError(null);
    const controller = new AbortController(),
      timer = setTimeout(() => controller.abort(), 90000);
    try {
      const r = await fetch("/api/treasury", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
        signal: controller.signal,
      });
      const d: any = await r.json();
      if (d.status !== "success") {
        setError(d);
        retryAt.current = Date.now() + Math.max(60000, d.retryAfterMs || 0);
        return;
      }
      setRows(d.rows);
      setLoaded(true);
      if (payload.action === "scan") {
        setCursor(d.cursor);
        setScan(
          `${d.examined} messages examined · ${d.matched} eligible candidates found${d.cursor ? " · more results available" : " · end of search"}. Existing invoices are kept once.`,
        );
        retryAt.current = Date.now() + 10000;
        setFilter("review");
      }
    } catch {
      setError({
        message:
          "Request interrupted. Refresh the ledger to check saved state before retrying.",
      });
    } finally {
      clearTimeout(timer);
      active.current = false;
      setBusy(false);
    }
  }
  useEffect(() => {
    action({ action: "list" });
  }, []);
  const totals = treasuryTotals(rows, month, chicagoDay()),
    used = Math.min(100, (totals.committed / 500000) * 100);
  const shown = rows.filter((r) =>
    filter === "review"
      ? r.status === "review"
      : filter === "all"
        ? true
        : filter === "overdue"
          ? r.status === "confirmed" &&
            r.currency === "USD" &&
            r.dueDate &&
            r.dueDate < chicagoDay()
          : r.dueDate?.startsWith(month) && r.status !== "excluded",
  );
  return (
    <section className="panel daily-panel treasury realm-framed realm-gondor">
      <PanelFrame kind="gondor" />
      <div className="panel-heading">
        <div>
          <GondorArt name="treasury" />
          <h3>The treasury of Gondor</h3>
        </div>
        <input
          type="month"
          aria-label="Budget month"
          value={month}
          onChange={(e) => {
            if (/^\d{4}-(?:0[1-9]|1[0-2])$/.test(e.target.value))
              setMonth(e.target.value);
          }}
          required
        />
      </div>
      <div className="treasury-overview">
        <div
          className={`treasury-ring ${totals.remaining < 0 ? "over-budget" : ""}`}
          style={{ "--budget-used": `${used}%` } as React.CSSProperties}
        >
          <GondorArt name="treasury" size={104} className="treasury-coffer-art" />
          <strong>{loaded ? money(totals.committed) : "—"}</strong>
          <small>of $5,000 / month</small>
        </div>
        <div className="treasury-metrics">
          <div>
            <strong>{loaded ? money(totals.remaining) : "—"}</strong>
            <small>
              {totals.remaining < 0
                ? "Over budget (negative)"
                : "Available to commit"}
            </small>
          </div>
          <button onClick={() => setFilter("overdue")}>
            <strong>{loaded ? money(totals.overdue) : "—"}</strong>
            <small>Overdue · all months</small>
          </button>
          <button onClick={() => setFilter("review")}>
            <strong>{loaded ? totals.review : "—"}</strong>
            <small>Awaiting your review</small>
          </button>
        </div>
      </div>
      <p className="daily-caption">
        Confirmed USD invoices allocated by due date, including paid invoices.
        Pending candidates do not count. This ledger does not verify bank
        payments.
      </p>
      <div className="treasury-scan">
        <button disabled={busy || !mailEnabled} onClick={() => action({ action: "scan" })} title={!mailEnabled ? "Gmail is paused in the Royal Registry" : undefined}>
          <Bird size={14} />
          {busy ? "Working…" : "Find invoices"}
        </button>
        {cursor && (
          <button
            disabled={busy || !mailEnabled}
            onClick={() => action({ action: "scan", cursor })}
          >
            Scan next 10
          </button>
        )}
        <button
          disabled={busy}
          onClick={() => action({ action: "list" })}
          aria-label="Refresh invoice ledger"
        >
          <RefreshCw size={14} />
        </button>
      </div>
      {!mailEnabled && <p className="connection-paused">Invoice discovery is paused in Settings. Your saved ledger remains available.</p>}
      <p className="daily-footnote">
        Past 90 days · To: dickersona@76fireworks.com or
        itservices@76fireworks.com · 10 messages per scan. Amounts in
        attachments may need manual review.
      </p>
      {scan && (
        <p className="daily-caption" role="status">
          {scan}
        </p>
      )}
      {error && <LoadState feed={error} />}
      <div className="daily-tabs">
        {[
          ["month", "This month"],
          ["review", `Review (${totals.review})`],
          ["overdue", "Overdue"],
          ["all", "All records"],
        ].map(([key, label]) => (
          <button
            key={key}
            aria-pressed={filter === key}
            onClick={() => setFilter(key)}
          >
            {label}
          </button>
        ))}
      </div>
      <div className="invoice-list">
        {shown.map((row) => (
          <InvoiceEditor
            key={`${row.id}-${row.status}-${row.amountCents}-${row.dueDate}-${row.vendor}`}
            row={row}
            busy={busy}
            save={(value) => action({ action: "update", ...value })}
          />
        ))}
        {loaded && !shown.length && (
          <p className="daily-empty">
            {filter === "review"
              ? "No invoices awaiting review."
              : "No saved invoices in this view. Find invoices to begin."}
          </p>
        )}
      </div>
      <footer className="daily-feed-footer">
        Based on saved invoices only · private, persistent ledger · no email
        sent to Claude
      </footer>
    </section>
  );
}
export function DailyCouncil() {
  const [watch, setWatch] = useState(true);
  return (
    <>
      <label className="daily-watch">
        <input
          type="checkbox"
          checked={watch}
          onChange={(e) => setWatch(e.target.checked)}
        />{" "}
        Watch ravens & quests · every 5 minutes while visible
      </label>
      <div className="daily-council">
        <Ravens watch={watch} />
        <Quests watch={watch} />
        <Treasury />
      </div>
    </>
  );
}

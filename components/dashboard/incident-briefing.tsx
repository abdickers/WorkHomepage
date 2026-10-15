"use client";
import { useState, useEffect, useRef } from "react";
import { ArrowUpRight, RefreshCw, Sparkles, ShieldCheck } from "lucide-react";
import type { Evidence } from "@/lib/briefing-data.mjs";

type Snapshot = {
  evidence: Evidence[];
  observedAt: string;
  expiresAt: number;
  snapshot: { body: string; signature: string } | null;
  configured: boolean;
  chatGPTSignInPath?: string;
};
type Briefing = {
  text: string;
  model: string;
  observedAt: string;
  generatedAt: string;
  evidence: Evidence[];
  truncated: boolean;
  usage: { inputTokens?: number; outputTokens?: number };
};
const time = (s: string) =>
  new Date(s).toLocaleTimeString("en-US", {
    timeZone: "America/Chicago",
    hour: "numeric",
    minute: "2-digit",
  });
export function IncidentBriefing() {
  const [snapshot, setSnapshot] = useState<Snapshot | null>(null);
  const [briefing, setBriefing] = useState<Briefing | null>(null);
  const [busy, setBusy] = useState<"prepare" | "generate" | null>(null);
  const [error, setError] = useState("");
  const [authPath, setAuthPath] = useState("");
  const [now, setNow] = useState(Date.now());
  const [retryAt, setRetryAt] = useState(0);
  const inflight = useRef(false);
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  async function action(action: "prepare" | "generate") {
    if (inflight.current) return;
    inflight.current = true;
    setBusy(action);
    setError("");
    setAuthPath("");
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 60000);
    try {
      const response = await fetch("/api/briefing", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          action,
          ...(action === "generate" ? { snapshot: snapshot?.snapshot } : {}),
        }),
        signal: controller.signal,
      });
      const data = (await response.json()) as Partial<Snapshot & Briefing> & {
        message?: string;
        retryAfterMs?: number;
      };
      if (!response.ok) {
        if (data.chatGPTSignInPath?.startsWith("/signin-with-chatgpt?"))
          setAuthPath(data.chatGPTSignInPath);
        if (data.retryAfterMs) setRetryAt(Date.now() + data.retryAfterMs);
        throw new Error(data.message || "Briefing unavailable.");
      }
      if (action === "prepare") {
        if (
          !Array.isArray(data.evidence) ||
          typeof data.expiresAt !== "number" ||
          typeof data.observedAt !== "string"
        )
          throw new Error("Invalid evidence response.");
        setSnapshot(data as Snapshot);
        setNow(Date.now());
      } else {
        if (typeof data.text !== "string" || !Array.isArray(data.evidence))
          throw new Error("Invalid briefing response.");
        setBriefing(data as Briefing);
        setRetryAt(Date.now() + 60000);
      }
    } catch (e) {
      setError(
        e instanceof Error && e.name !== "AbortError"
          ? e.message
          : "Request timed out. No automatic retry was made.",
      );
    } finally {
      clearTimeout(timeout);
      setBusy(null);
      inflight.current = false;
    }
  }
  const expired = !!snapshot && now >= snapshot.expiresAt;
  const confirmed =
    snapshot?.evidence.filter((e) => e.state === "confirmed").length ?? 0;
  return (
    <div className="incident-briefing">
      <div
        className={"council-orb-widget " + (busy ? "orb-awake" : "")}
        aria-hidden="true"
      >
        <div className="council-orbit" />
        <div className="council-orb">
          <span>✧</span>
        </div>
        <div className="council-base" />
      </div>
      <div className="briefing-actions">
        <button
          className="outline-button"
          disabled={!!busy}
          onClick={() => action("prepare")}
        >
          <RefreshCw size={14} className={busy === "prepare" ? "spin" : ""} />
          {busy === "prepare"
            ? "Collecting…"
            : snapshot
              ? "Refresh evidence"
              : "Collect evidence"}
        </button>
        <button
          className="gold-button"
          disabled={
            !!busy ||
            !snapshot?.configured ||
            !snapshot.snapshot ||
            !confirmed ||
            expired ||
            now < retryAt
          }
          onClick={() => action("generate")}
        >
          <Sparkles size={14} />
          {busy === "generate"
            ? "Consulting Claude…"
            : now < retryAt
              ? `Wait ${Math.ceil((retryAt - now) / 1000)}s`
              : "Generate briefing"}
        </button>
      </div>
      {snapshot ? (
        <>
          <p className="briefing-snapshot">
            {confirmed}/{snapshot.evidence.length} feeds confirmed · collected{" "}
            {time(snapshot.observedAt)} CT
            {expired
              ? " · expired—refresh to generate"
              : " · valid for 5 minutes"}
          </p>
          <div className="briefing-evidence">
            {snapshot.evidence.map((e) => (
              <details key={e.id}>
                <summary>
                  <span
                    className={
                      e.state === "confirmed" ? "confirmed-text" : "error-text"
                    }
                  >
                    {e.state === "confirmed" ? "●" : "○"}
                  </span>{" "}
                  [{e.id}] {e.source}
                  <span>
                    {e.state === "confirmed" ? "Read confirmed" : "Unavailable"}
                  </span>
                </summary>
                <p>{e.scope}</p>
                <dl>
                  {Object.entries(e.facts).map(([k, v]) => (
                    <div key={k}>
                      <dt>{k.replace(/([A-Z])/g, " $1")}</dt>
                      <dd>{v === null ? "Unknown" : String(v)}</dd>
                    </div>
                  ))}
                </dl>
                <p>{e.caveat}</p>
                <a href={e.href} target="_blank" rel="noreferrer">
                  Open source <ArrowUpRight size={12} />
                </a>
              </details>
            ))}
          </div>
          {!snapshot.configured && (
            <p className="connection-message warning">
              Evidence is ready. Claude needs its server-side secret before
              briefings can run.
            </p>
          )}
          {snapshot.chatGPTSignInPath && (
            <p className="connection-message">
              <a href={snapshot.chatGPTSignInPath} target="_top">
                Reconnect operational feeds
              </a>
            </p>
          )}
        </>
      ) : (
        <div className="briefing-empty">
          <ShieldCheck size={24} />
          <p>Collect → review → consult</p>
        </div>
      )}
      {error && (
        <p className="connection-message warning" role="alert">
          {error}
        </p>
      )}
      {authPath && (
        <p className="connection-message">
          <a href={authPath} target="_top">
            Sign in to the Council
          </a>
        </p>
      )}
      {briefing && (
        <div className="briefing-result" aria-live="polite">
          <div className="briefing-result-heading">
            <strong>The Council’s assessment</strong>
            <span>{time(briefing.generatedAt)} CT</span>
          </div>
          <p className="briefing-snapshot">
            AI interpretation · evidence from {time(briefing.observedAt)} CT
            {Date.now() - Date.parse(briefing.observedAt) > 300000
              ? " · older snapshot"
              : ""}
          </p>
          <div className="briefing-text">{briefing.text}</div>
          {briefing.truncated && (
            <p className="connection-message warning">
              The response reached its output limit and may be incomplete.
            </p>
          )}
          <nav
            className="briefing-source-links"
            aria-label="Briefing evidence sources"
          >
            {briefing.evidence.map((e) => (
              <a key={e.id} href={e.href} target="_blank" rel="noreferrer">
                [{e.id}] {e.source} <ArrowUpRight size={10} />
              </a>
            ))}
          </nav>
          <small>
            {briefing.model} · {briefing.usage.inputTokens ?? "—"} input /{" "}
            {briefing.usage.outputTokens ?? "—"} output tokens
          </small>
        </div>
      )}
      <details className="realm-explainer">
        <summary>Privacy & coverage</summary>
        <p className="briefing-privacy">
          Only the displayed counts, timestamps, scope, and caveats go to
          Anthropic when you click Generate. No raw logs, customer records, or
          flow names. Briefings stay in this page until reload.
        </p>
        <p className="briefing-coverage">
          Current coverage: Celigo Prod + this dashboard’s Worker. Netlify,
          GitHub, Jira, Vercel, and other Cloudflare resources are not included
          yet.
        </p>
      </details>
    </div>
  );
}

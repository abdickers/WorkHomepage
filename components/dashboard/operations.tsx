"use client";
import { useEffect, useRef, useState } from "react";
import {
  Activity,
  RefreshCw,
  ArrowUpRight,
  RadioTower,
  AlertTriangle,
  Check,
  Clock3,
} from "lucide-react";
import { BrandLogo } from "./brand-logo";
import { maskLog, providerData, records } from "@/lib/provider-data.mjs";
type Feed = {
  state: "idle" | "loading" | "success" | "error" | "auth";
  data: any;
  message: string;
  fetchedAt?: string;
  retryAt: number;
  authPath?: string;
};
const blank = (): Feed => ({
  state: "idle",
  data: null,
  message: "",
  retryAt: 0,
});
function time(value: unknown) {
  if (typeof value !== "string" && typeof value !== "number") return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime())
    ? "—"
    : d.toLocaleTimeString("en-US", {
        timeZone: "America/Chicago",
        hour: "numeric",
        minute: "2-digit",
        second: "2-digit",
      });
}
function flowTime(value: unknown) {
  if (typeof value !== "string") return "—";
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return "—";
  const today = new Date().toLocaleDateString("en-CA", {
    timeZone: "America/Chicago",
  });
  const day = d.toLocaleDateString("en-CA", { timeZone: "America/Chicago" });
  return day === today
    ? time(value)
    : d.toLocaleString("en-US", {
        timeZone: "America/Chicago",
        month: "short",
        day: "numeric",
        hour: "numeric",
        minute: "2-digit",
      });
}
function rows(result: any, key = "items") {
  if (!result || result.status !== "success") return null;
  try {
    return records(result.result, key);
  } catch {
    return null;
  }
}
function failure(result: any) {
  return result && result.status !== "success"
    ? result.message || "Source unavailable."
    : null;
}
export function Operations() {
  const [tab, setTab] = useState("celigo");
  const [diagnostics,setDiagnostics]=useState<string>("");
  async function inspectConnection(){try{const r=await fetch("/api/operations/context");const d=await r.json() as any;setDiagnostics(JSON.stringify(d.status==="success"?d.connectors.map((c:any)=>({id:c.connectorId,policy:c.policy,tools:c.tools?.map((t:any)=>({actionName:t.actionName,description:t.description,inputSchema:t.inputSchema}))??null})):d,null,2));}catch{setDiagnostics("Connection details unavailable.");}}
  const [auto, setAuto] = useState(true);
  const [feeds, setFeeds] = useState<Record<string, Feed>>({
    celigo: blank(),
    cloudflare: blank(),
  });
  const current = useRef(feeds);
  const inflight = useRef(new Set<string>());
  const [now, setNow] = useState(Date.now());
  useEffect(() => {
    current.current = feeds;
  }, [feeds]);
  async function refresh(source: string, manual = false) {
    if (source === "vercel" || inflight.current.has(source)) return;
    const prev = current.current[source];
    if (Date.now() < prev.retryAt || (!manual && prev.state === "auth")) return;
    inflight.current.add(source);
    setFeeds((f) => ({
      ...f,
      [source]: { ...f[source], state: "loading", message: "" },
    }));
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 65000);
    try {
      const response = await fetch(`/api/operations/${source}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: "{}",
        signal: controller.signal,
      });
      const data = (await response.json()) as any;
      if (data.status !== "success")
        throw Object.assign(
          new Error(data.message || "This source is unavailable."),
          {
            status: data.status,
            retryAfterMs: data.retryAfterMs,
            authPath: data.chatGPTSignInPath,
          },
        );
      if (
        source === "celigo" &&
        data.sources?.flows?.status !== "success" &&
        data.sources?.jobs?.status !== "success"
      ) {
        const e = data.sources?.flows || data.sources?.jobs;
        throw Object.assign(
          new Error(e?.message || "Celigo did not confirm a result."),
          {
            status: e?.status,
            retryAfterMs: e?.retryAfterMs,
            authPath: data.chatGPTSignInPath,
          },
        );
      }
      setFeeds((f) => ({
        ...f,
        [source]: {
          state: "success",
          data,
          message: "",
          fetchedAt: new Date().toISOString(),
          retryAt: 0,
        },
      }));
    } catch (error: any) {
      setFeeds((f) => ({
        ...f,
        [source]: {
          ...f[source],
          state:
            error.status === "reauthentication_required" ? "auth" : "error",
          message:
            error.name === "AbortError"
              ? "Request timed out. The last result is retained."
              : error.message,
          retryAt: Date.now() + Math.max(60000, error.retryAfterMs || 0),
          authPath: error.authPath,
        },
      }));
    } finally {
      clearTimeout(timeout);
      inflight.current.delete(source);
    }
  }
  useEffect(() => {
    refresh("celigo");
    refresh("cloudflare");
    const t = setInterval(() => setNow(Date.now()), 10000);
    return () => clearInterval(t);
  }, []);
  useEffect(() => {
    if (!auto) return;
    const timer = setInterval(() => {
      if (document.visibilityState === "visible") {
        refresh("celigo");
        refresh("cloudflare");
      }
    }, 60000);
    return () => clearInterval(timer);
  }, [auto]);
  const feed = feeds[tab];
  const flows = rows(feeds.celigo.data?.sources?.flows),
    jobs = rows(feeds.celigo.data?.sources?.jobs);
  const totalErrors = flows?.reduce(
    (n, f) => n + (Number(f.numOpenError) || 0),
    0,
  );
  const jobByFlow = new Map<string, any>();
  jobs?.forEach((j) => {
    const id = j._flowId || j.flow?._id;
    const old = jobByFlow.get(id);
    if (!old || j.status === "running") jobByFlow.set(id, j);
  });
  let logs: any[] | null = null;
  try {
    if (feeds.cloudflare.data?.status === "success")
      logs = records(feeds.cloudflare.data.result, "events");
  } catch {}
  const stale =
    feed?.fetchedAt &&
    (feed.state === "error" || now - Date.parse(feed.fetchedAt) > 130000);
  const sources = feeds.celigo.data?.sources;
  return (
    <section className="panel operations systems">
      <div className="panel-heading">
        <div>
          <RadioTower size={19} />
          <h3>The Beacons of Production</h3>
        </div>
        <label className="auto-refresh">
          <input
            type="checkbox"
            checked={auto}
            onChange={(e) => setAuto(e.target.checked)}
          />
          Auto · 60s
        </label>
      </div>
      <div
        className="operations-tabs"
        role="group"
        aria-label="Operations provider"
      >
        {[
          ["celigo", "Celigo"],
          ["vercel", "Vercel"],
          ["cloudflare", "Cloudflare"],
        ].map(([id, name]) => (
          <button
            key={id}
            className={tab === id ? "selected" : ""}
            onClick={() => setTab(id)}
          >
            <BrandLogo name={name} />
            {name}
            {id === "celigo" &&
              totalErrors !== undefined &&
              totalErrors > 0 && (
                <span className="error-count">{totalErrors}</span>
              )}
          </button>
        ))}
        <button
          className="icon-btn refresh-operation"
          aria-label="Refresh operations"
          disabled={
            tab === "vercel" ||
            feed?.state === "loading" ||
            Date.now() < (feed?.retryAt || 0)
          }
          onClick={() => refresh(tab, true)}
        >
          <RefreshCw
            size={16}
            className={feed?.state === "loading" ? "spin" : ""}
          />
        </button>
      </div>
      {tab === "celigo" && (
        <>
          <div className="operations-metrics">
            <div>
              <span className="metric-dot running" />
              Running
              <strong>
                {jobs ? jobs.filter((j) => j.status === "running").length : "—"}
              </strong>
            </div>
            <div>
              <span className="metric-dot queued" />
              Queued
              <strong>
                {jobs ? jobs.filter((j) => j.status === "queued").length : "—"}
              </strong>
            </div>
            <div>
              <span className="metric-dot errors" />
              Open errors<strong>{totalErrors ?? "—"}</strong>
            </div>
            <div>
              Flows returned<strong>{flows?.length ?? "—"}</strong>
            </div>
          </div>
          {(failure(sources?.flows) || failure(sources?.jobs)) && (
            <p className="connection-message warning">
              Partial result:{" "}
              {failure(sources?.flows) || failure(sources?.jobs)}
            </p>
          )}
          {flows ? (
            <div className="operations-table-scroll">
              <table className="operations-table">
                <thead>
                  <tr>
                    <th>Production flow</th>
                    <th>State</th>
                    <th>Last triggered · CT</th>
                    <th>Open errors</th>
                  </tr>
                </thead>
                <tbody>
                  {[...flows]
                    .sort(
                      (a, b) =>
                        (Number(b.numOpenError) || 0) -
                        (Number(a.numOpenError) || 0),
                    )
                    .map((f) => {
                      const job = jobByFlow.get(f._id);
                      const status =
                        job?.status ||
                        (f.disabled
                          ? "Paused"
                          : f.numOpenError > 0
                            ? "Needs attention"
                            : "Idle");
                      return (
                        <tr key={f._id}>
                          <td>
                            <a
                              href="https://integrator.io/"
                              target="_blank"
                              rel="noreferrer"
                            >
                              {f.name || f._id}
                              <ArrowUpRight size={11} />
                            </a>
                            {job && (
                              <small>
                                {Number(job.numSuccess || 0).toLocaleString()}{" "}
                                records succeeded in current run
                              </small>
                            )}
                          </td>
                          <td>
                            <span
                              className={
                                "flow-state " +
                                (job?.status === "running"
                                  ? "running"
                                  : f.numOpenError > 0
                                    ? "attention"
                                    : "")
                              }
                            >
                              {status}
                            </span>
                          </td>
                          <td>{flowTime(f.lastExecutedAt)}</td>
                          <td
                            className={f.numOpenError > 0 ? "error-text" : ""}
                          >
                            {Number(f.numOpenError || 0).toLocaleString()}
                          </td>
                        </tr>
                      );
                    })}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="operations-empty">
              <Activity size={25} />
              <p>
                {feed?.state === "loading"
                  ? "Reading production flows…"
                  : "Production flow data is unavailable."}
              </p>
            </div>
          )}
          {sources?.flows?.status === "success" &&
            (() => {
              try {
                return providerData(sources.flows.result).hasMore;
              } catch {
                return false;
              }
            })() && (
              <p className="connection-message">
                Showing the first 50 flows. Counts cover this page only.
              </p>
            )}
          <p className="operations-scope">
            Production account · current jobs and open errors. Idle means no
            active job returned; it does not confirm the last sync succeeded.
          </p>
        </>
      )}
      {tab === "vercel" && (
        <div className="operations-empty">
          <BrandLogo name="Vercel" />
          <h4>Choose the watchtower.</h4>
          <p>
            The connected account returned no projects or teams. Project
            selection is deferred; runtime and deployment logs are not being
            monitored.
          </p>
          <a
            className="outline-button"
            href="https://vercel.com/dashboard"
            target="_blank"
            rel="noreferrer"
          >
            Open Vercel
            <ArrowUpRight size={13} />
          </a>
        </div>
      )}
      {tab === "cloudflare" && (
        <>
          <div className="cloudflare-scope">
            <BrandLogo name="Cloudflare" />
            <div>
              <strong>This dashboard’s Worker</strong>
              <small>Last hour · up to 30 request and error events</small>
            </div>
            <span className="tag">SCOPED</span>
          </div>
          {logs ? (
            <div className="operations-table-scroll">
              <table className="operations-table log-table">
                <thead>
                  <tr>
                    <th>Time · CT</th>
                    <th>Request / event</th>
                    <th>Result</th>
                  </tr>
                </thead>
                <tbody>
                  {logs.length ? (
                    logs.map((log, i) => {
                      const worker = log.$workers;
                      const path = worker?.event?.path;
                      const message =
                        path ||
                        maskLog(
                          log.source?.message ||
                            log.$metadata?.message ||
                            "Worker event",
                        );
                      const status = worker?.event?.response?.status;
                      return (
                        <tr key={log.$metadata?.id || i}>
                          <td>{time(log.timestamp)}</td>
                          <td>
                            {maskLog(message)}
                            <small>{log.$metadata?.requestId || ""}</small>
                          </td>
                          <td
                            className={
                              Number(status) >= 500 ? "error-text" : ""
                            }
                          >
                            {status || worker?.outcome || "Event"}
                          </td>
                        </tr>
                      );
                    })
                  ) : (
                    <tr>
                      <td colSpan={3}>No events returned in this window.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="operations-empty">
              <Activity size={24} />
              <p>
                {feed?.state === "loading"
                  ? "Reading Worker logs…"
                  : "Worker logs have not loaded."}
              </p>
            </div>
          )}
          <p className="operations-scope">
            Cloudflare logs for this hosted Site only. Other Workers, zones, and
            account analytics are deferred.
          </p>
        </>
      )}
      {feed?.message && (
        <div className="feed-error" role="status">
          <AlertTriangle size={15} />
          <span>
            {feed.message}
            {feed.data ? " Last successful data is shown as stale." : ""}
          </span>
          {feed.state === "auth" &&
            feed.authPath?.startsWith("/signin-with-chatgpt?") && (
              <a href={feed.authPath} target="_top">
                Connect
              </a>
            )}
        </div>
      )}
      <button className="text-action" onClick={inspectConnection}>Connection details</button>
      {diagnostics&&<pre className="connection-details">{diagnostics}</pre>}
      <div className="panel-foot">
        <span className={"local-dot " + (stale ? "stale-dot" : "")} />
        <span>
          {tab === "vercel"
            ? "Not configured"
            : feed?.state === "loading"
              ? "Refreshing…"
              : stale
                ? "Stale data"
                : feed?.state === "success"
                  ? "Live read confirmed"
                  : feed?.state === "auth"
                    ? "Sign-in needed"
                    : "Awaiting a confirmed read"}
        </span>
        <span>
          {feed?.fetchedAt
            ? `Updated ${time(feed.fetchedAt)} CT`
            : "No successful refresh yet"}
        </span>
      </div>
    </section>
  );
}

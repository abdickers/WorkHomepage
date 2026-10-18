"use client";
import { useState } from "react";
import { celigoFlowUrl } from "@/lib/daily-data.mjs";
import { StudyArt } from "./study-art";
import { GondorArt } from "./gondor-art";
import { GuardianIcon } from "./guardian-icon";

const litBeaconArt: Record<string, string> = {
  error: "beacon-error-orange",
  queued: "beacon-queued-blue",
  active: "beacon-alert-gold",
};

/** Supplied beacon artwork. Counts remain observations, never a health score. */
export function Beacon({
  value,
  label,
  tone,
}: {
  value: number | null;
  label: string;
  tone: string;
}) {
  const lit = value !== null && value > 0;
  const artwork = lit && tone !== "roster"
    ? (litBeaconArt[tone] ?? "beacon-alert-gold")
    : "beacon-unlit";
  return (
    <div
      className={`realm-beacon ${tone} ${lit ? "lit" : "unlit"}`}
      aria-label={`${label}: ${value ?? "unknown"}`}
    >
      <img
        className="beacon-art"
        src={`/art/beacons/${artwork}.webp`}
        alt=""
        aria-hidden="true"
        width="320"
        height="480"
        decoding="async"
      />
      <strong>{value === null ? "—" : value.toLocaleString()}</strong>
      <span>{label}</span>
    </div>
  );
}

type RealmFlow = {
  _id: string;
  _integrationId?: string;
  _flowGroupingId?: string;
  name?: string;
  disabled?: boolean;
  numOpenError?: number;
  lastExecutedAt?: string;
};
export function FlowConstellation({
  flows,
  jobs,
  stale,
}: {
  flows: RealmFlow[] | null;
  jobs: any[] | null;
  stale: boolean;
}) {
  const [selected, setSelected] = useState<string | null>(null);
  const activeIds = new Set(
    jobs
      ?.filter((j) => j.status === "running")
      .map((j) => j._flowId || j.flow?._id),
  );
  const state = (f: RealmFlow) =>
    Number(f.numOpenError) > 0
      ? "error"
      : activeIds.has(f._id)
        ? "active"
        : f.disabled
          ? "paused"
          : jobs &&
              typeof f.numOpenError === "number" &&
              Number.isFinite(f.numOpenError) &&
              f.numOpenError >= 0
            ? "idle"
            : "unknown";
  const chosen = flows?.find((f) => f._id === selected);
  const troubled =
    flows
      ?.filter((f) => Number(f.numOpenError) > 0)
      .sort((a, b) => Number(b.numOpenError) - Number(a.numOpenError)) ?? [];
  const total = troubled.reduce((s, f) => s + Number(f.numOpenError), 0);
  const incomplete = flows?.some(
    (f) =>
      typeof f.numOpenError !== "number" ||
      !Number.isFinite(f.numOpenError) ||
      f.numOpenError < 0,
  );
  return (
    <div className={`realm-flow-board ${stale ? "realm-stale" : ""}`}>
      <div className="constellation-widget">
        <div className="realm-widget-label">
          The flow constellation{stale && <b>STALE</b>}
        </div>
        <div className="constellation-chart">
          <svg viewBox="0 0 300 270" aria-hidden="true">
            <circle cx="150" cy="135" r="108" className="rune-ring" />
            <circle cx="150" cy="135" r="92" className="rune-ring inner" />
            <path
              d="M150 12V35M150 235V258M27 135H48M252 135H273"
              className="compass-ticks"
            />
            {flows?.slice(0, 50).map((f, i) => {
              const a = (i / flows.length) * Math.PI * 2 - Math.PI / 2;
              return (
                <path
                  key={f._id}
                  d={`M150 135L${150 + 108 * Math.cos(a)} ${135 + 108 * Math.sin(a)}`}
                  className={`constellation-spoke ${state(f)}`}
                />
              );
            })}
          </svg>
          <div className="constellation-center">
            <StudyArt name="elven-compass" className="constellation-compass-art" />
            <div className="constellation-total">
              <strong>{flows?.length ?? "—"}</strong>
              <span>flows returned</span>
            </div>
          </div>
          {flows?.slice(0, 50).map((f, i) => {
            const a = (i / flows.length) * Math.PI * 2 - Math.PI / 2;
            return (
              <button
                key={f._id}
                style={{
                  left: `${50 + 36 * Math.cos(a)}%`,
                  top: `${50 + 40 * Math.sin(a)}%`,
                }}
                className={`flow-star ${state(f)} ${selected === f._id ? "chosen" : ""}`}
                title={`${f.name || f._id} · ${state(f)} · ${f.numOpenError ?? "unknown"} open errors`}
                aria-label={`${f.name || f._id}: ${state(f)}, ${f.numOpenError ?? "unknown"} open errors`}
                aria-pressed={selected === f._id}
                onClick={() => setSelected(f._id)}
              >
                <span />
              </button>
            );
          })}
        </div>
        <div className="realm-legend">
          <span className="error">Errors</span>
          <span className="active">Running</span>
          <span className="paused">Paused</span>
          <span className="idle">Idle</span>
          {flows?.some((f) => state(f) === "unknown") && (
            <span className="idle">Unknown</span>
          )}
        </div>
        <p className="constellation-selection">
          {chosen
            ? `${chosen.name || chosen._id} · ${chosen.numOpenError ?? "?"} open errors`
            : flows
              ? "Select a star to inspect its flow"
              : "Awaiting a confirmed read"}
        </p>
        {chosen && celigoFlowUrl(chosen) && (
          <a
            className="realm-flow-link"
            href={celigoFlowUrl(chosen)!}
            target="_blank"
            rel="noopener noreferrer"
          >
            Open this flow in Celigo ↗
          </a>
        )}
      </div>
      <div className="error-ledger">
        <div className="realm-widget-label">Where the shadows gather</div>
        <div className="error-sword" aria-hidden="true">
          <StudyArt name="ranger-sword" />
        </div>
        {flows ? (
          troubled.length ? (
            <div className="error-distribution">
              {troubled.slice(0, 3).map((f) => (
                <div key={f._id}>
                  <div className="error-bar-label">
                    {celigoFlowUrl(f) ? (
                      <a
                        href={celigoFlowUrl(f)!}
                        target="_blank"
                        rel="noopener noreferrer"
                        title="Open this flow in Celigo"
                      >
                        {f.name || f._id} ↗
                      </a>
                    ) : (
                      <span title={f.name}>{f.name || f._id}</span>
                    )}
                    <strong>{Number(f.numOpenError).toLocaleString()}</strong>
                  </div>
                  <div className="error-bar-track">
                    <span
                      style={{
                        width: `${(Number(f.numOpenError) / total) * 100}%`,
                      }}
                    />
                  </div>
                </div>
              ))}
              {troubled.length > 3 && (
                <small>+ {troubled.length - 3} other flows with errors</small>
              )}
            </div>
          ) : (
            <div className="realm-no-errors">
              <span>◇</span>
              <strong>
                {incomplete
                  ? "Some error counts unavailable"
                  : "0 open errors returned"}
              </strong>
            </div>
          )
        ) : (
          <div className="realm-no-errors">
            <span>?</span>
            <strong>Error data unavailable</strong>
          </div>
        )}
        <small>Open-error backlog · not new failures per hour</small>
      </div>
    </div>
  );
}

export function WorkerRunes({ logs, stale = false }: { logs: any[]; stale?: boolean }) {
  const groups = [
    {
      label: "2xx",
      tone: "active",
      value: logs.filter((l) => {
        const s = Number(l.$workers?.event?.response?.status);
        return s >= 200 && s < 300;
      }).length,
    },
    {
      label: "4xx",
      tone: "paused",
      value: logs.filter((l) => {
        const s = Number(l.$workers?.event?.response?.status);
        return s >= 400 && s < 500;
      }).length,
    },
    {
      label: "5xx",
      tone: "error",
      value: logs.filter((l) => {
        const s = Number(l.$workers?.event?.response?.status);
        return s >= 500 && s < 600;
      }).length,
    },
    {
      label: "Exceptions",
      tone: "error",
      value: logs.filter((l) => l.$workers?.outcome === "exception").length,
    },
  ];
  return (
    <div className="worker-runes">
      <div className="rune-counters">
        {groups.map((g) => (
          <div key={g.label} className={g.tone}>
            <GuardianIcon alert={g.tone !== "active" && g.value > 0} unavailable={stale} />
            <strong>{g.value}</strong>
            <small>{g.label}</small>
          </div>
        ))}
      </div>
      <div className="realm-widget-label">
        The watch · {logs.length} sampled events
      </div>
      <div className="event-rune-strip" aria-label="Sampled request outcomes">
        {[...logs]
          .sort(
            (a, b) =>
              (new Date(a.timestamp).getTime() || 0) -
              (new Date(b.timestamp).getTime() || 0),
          )
          .map((l, i) => {
            const s = Number(l.$workers?.event?.response?.status);
            return (
              <span
                key={i}
                className={
                  s >= 500 || l.$workers?.outcome === "exception"
                    ? "error"
                    : s >= 400
                      ? "paused"
                      : s >= 200 && s < 400
                        ? "active"
                        : "idle"
                }
                title={`${l.timestamp ? new Date(l.timestamp).toLocaleTimeString("en-US", { timeZone: "America/Chicago" }) : "Time unavailable"} CT · ${s || l.$workers?.outcome || "Event"}`}
              >
                ◆
              </span>
            );
          })}
      </div>
      <small>
        Oldest → newest returned · sample, not total traffic or error rate
      </small>
    </div>
  );
}

export function QuestSeal({ done, total }: { done: number; total: number }) {
  const progress = total ? done / total : 0;
  return (
    <div className="quest-seal-widget">
      <GondorArt name="shield" size={160} className="quest-medallion-art" />
      <div>
        <span className="realm-widget-label">The day’s quest</span>
        <strong>{total - done} remaining</strong>
        <div className="quest-progress-status">
          <svg viewBox="0 0 140 140" aria-hidden="true">
            <circle cx="70" cy="70" r="50" className="quest-track" />
            <circle cx="70" cy="70" r="50" className="quest-progress"
              strokeDasharray={`${progress * 314.16} 314.16`}
              transform="rotate(-90 70 70)" />
          </svg>
          <span>{done} / {total} complete</span>
        </div>
        <small>
          {total ? `${Math.round(progress * 100)}% complete` : "A fresh page"}
        </small>
      </div>
    </div>
  );
}

export function DayDial({
  now,
  events,
  loaded,
}: {
  now: Date | null;
  events: number;
  loaded: boolean;
}) {
  const hour = now
    ? Number(
        new Intl.DateTimeFormat("en-US", {
          timeZone: "America/Chicago",
          hour: "numeric",
          hourCycle: "h23",
        }).format(now),
      )
    : 0;
  const minute = now
    ? Number(
        new Intl.DateTimeFormat("en-US", {
          timeZone: "America/Chicago",
          minute: "numeric",
        }).format(now),
      )
    : 0;
  return (
    <div className="day-dial-widget">
      <GondorArt name="calendar" size={160} className="agenda-sundial-art" />
      <div>
        <span className="realm-widget-label">The sun’s passage</span>
        <time className="agenda-live-time" dateTime={now?.toISOString()}>
          {now ? `${String(hour).padStart(2, "0")}:${String(minute).padStart(2, "0")} CT` : "Clock loading"}
        </time>
        <strong>
          {loaded ? events : "—"}
          <small>{loaded ? "events today" : "calendar not loaded"}</small>
        </strong>
      </div>
    </div>
  );
}

"use client";
import { PanelFrame } from "@/components/dashboard/panel-frame";
import { StudyArt } from "@/components/dashboard/study-art";
import { PolishArt } from "@/components/dashboard/polish-art";
import { useEffect, useRef, useState } from "react";
import { Link2, RotateCcw, Download, Upload, Sun, Moon, Command, Mountain } from "lucide-react";
import { LayoutDashboard, ListChecks, NotebookPen, RadioTower, Settings, Search, ArrowUpRight, Plus, ArrowRight, Clock3, CalendarDays, ChevronRight, Check, X, Flame, Play, Pause, RefreshCw, Shield } from "@/components/dashboard/realm-icon";
import { BrandLogo } from "@/components/dashboard/brand-logo";
import { Operations } from "@/components/dashboard/operations";
import { DailyCouncil } from "@/components/dashboard/daily-council";
import { Palantir } from "@/components/dashboard/palantir";
import { QuestSeal, DayDial } from "@/components/dashboard/realm-widgets";
type Task = {
  id: string;
  text: string;
  done: boolean;
  priority: "normal" | "high";
  kind: "task" | "waiting";
};
type Note = { id: string; text: string; created: string };
type Link = { name: string; url: string; abbr: string; color: string };
import { parseEvents, eventTime } from "@/lib/dashboard-data.mjs";
import type { AgendaEvent as Agenda } from "@/lib/dashboard-data.mjs";
import { accountLink, WORK_EMAIL, PERSONAL_URL, WORK_URL } from "@/lib/account-links.mjs";
const initialLinks: Link[] = [
  {
    name: "NetSuite",
    url: "https://system.netsuite.com/",
    abbr: "N",
    color: "#88a4ac",
  },
  {
    name: "Celigo",
    url: "https://integrator.io/",
    abbr: "c",
    color: "#eaaf6b",
  },
  {
    name: "Gmail",
    url: "https://mail.google.com/",
    abbr: "M",
    color: "#e1a793",
  },
  {
    name: "Calendar",
    url: "https://calendar.google.com/",
    abbr: "31",
    color: "#8ea8d6",
  },
  {
    name: "GitHub",
    url: "https://github.com/abdickers",
    abbr: "GH",
    color: "#d8d6cb",
  },
  { name: "Slack", url: "https://slack.com/", abbr: "#", color: "#c5a3ce" },
  {
    name: "Drive",
    url: "https://drive.google.com/",
    abbr: "△",
    color: "#a0c9a7",
  },
  {
    name: "ChatGPT",
    url: "https://chatgpt.com/",
    abbr: "✳",
    color: "#97bcad",
  },
];
initialLinks.push(
  {
    name: "BigCommerce",
    url: "https://login.bigcommerce.com/",
    abbr: "BC",
    color: "#a5b2de",
  },
  {
    name: "Vercel",
    url: "https://vercel.com/dashboard",
    abbr: "V",
    color: "#e5e5e5",
  },
  {
    name: "Cloudflare",
    url: "https://dash.cloudflare.com/",
    abbr: "CF",
    color: "#eea453",
  },
  {
    name: "Jira",
    url: "https://start.atlassian.com/",
    abbr: "J",
    color: "#76a8ee",
  },
  {
    name: "Confluence",
    url: "https://start.atlassian.com/",
    abbr: "C",
    color: "#81a9e4",
  },
  { name: "Zoom", url: "https://zoom.us/", abbr: "Z", color: "#729ff1" },
  { name: "Claude", url: "https://claude.ai/", abbr: "C", color: "#d99e7e" },
  {
    name: "Gemini",
    url: "https://gemini.google.com/",
    abbr: "G",
    color: "#94b5ed",
  },
);
const fellowshipNames = [
  "NetSuite",
  "GitHub",
  "Celigo",
  "BigCommerce",
  "Dotdigital",
  "Jira",
  "Confluence",
  "Avalara",
  "Cybersource",
  "Netlify",
  "Cloudflare",
];
initialLinks.push(
  {
    name: "Dotdigital",
    url: "https://dotdigital.com/",
    abbr: "D",
    color: "#e092be",
  },
  {
    name: "Avalara",
    url: "https://www.avalara.com/",
    abbr: "A",
    color: "#ef9a60",
  },
  {
    name: "Cybersource",
    url: "https://www.cybersource.com/",
    abbr: "C",
    color: "#79add0",
  },
  {
    name: "Netlify",
    url: "https://app.netlify.com/",
    abbr: "N",
    color: "#75c7bc",
  },
);
const routines: Task[] = [
  {
    id: "routine-1",
    text: "Check overnight integrations",
    done: false,
    priority: "high",
    kind: "task",
  },
  {
    id: "routine-2",
    text: "Review requests and blockers",
    done: false,
    priority: "normal",
    kind: "task",
  },
  {
    id: "routine-3",
    text: "Protect a block for deep work",
    done: false,
    priority: "normal",
    kind: "task",
  },
];
function read<T>(key: string, fallback: T): T {
  try {
    const value = localStorage.getItem(key);
    return value ? JSON.parse(value) : fallback;
  } catch {
    return fallback;
  }
}
function chicagoDay() {
  return new Intl.DateTimeFormat("en-CA", {
    timeZone: "America/Chicago",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).format(new Date());
}
export default function Dashboard() {
  const [ready, setReady] = useState(false);
  const [tasks, setTasks] = useState<Task[]>([]);
  const [notes, setNotes] = useState<Note[]>([]);
  const [links, setLinks] = useState(initialLinks);
  const [view, setView] = useState("overview");
  const [filter, setFilter] = useState("open");
  const [capture, setCapture] = useState("");
  const [kind, setKind] = useState("task");
  const [priority, setPriority] = useState(false);
  const [query, setQuery] = useState("");
  const [now, setNow] = useState<Date | null>(null);
  const [light, setLight] = useState(false);
  const [settings, setSettings] = useState(false);
  const [notice, setNotice] = useState("");
  const [seconds, setSeconds] = useState(25 * 60);
  const [running, setRunning] = useState(false);
  const [agenda, setAgenda] = useState<Agenda[]>([]);
  const [agendaState, setAgendaState] = useState("idle");
  const [agendaMessage, setAgendaMessage] = useState("");
  const [refreshed, setRefreshed] = useState("");
  const [retryAt, setRetryAt] = useState(0);
  const [authPath, setAuthPath] = useState("");
  const captureRef = useRef<HTMLTextAreaElement>(null);
  const searchRef = useRef<HTMLInputElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  useEffect(() => {
    const t = read<Task[]>("command-tasks-v1", routines),
      n = read<Note[]>("command-notes-v1", []),
      l = read<Link[]>("command-links-v1", initialLinks);
    setTasks(Array.isArray(t) ? t : routines);
    setNotes(Array.isArray(n) ? n : []);
    setLinks(
      Array.isArray(l)
        ? [
            ...l,
            ...initialLinks.filter(
              (item) => !l.some((saved) => saved.name === item.name),
            ),
          ]
        : initialLinks,
    );
    setLight(read("command-light", false));
    setNow(new Date());
    setReady(true);
    const timer = setInterval(() => setNow(new Date()), 30000);
    const shortcut = (e: KeyboardEvent) => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        searchRef.current?.focus();
      }
      if (e.key === "Escape") setSettings(false);
    };
    window.addEventListener("keydown", shortcut);
    return () => {
      clearInterval(timer);
      window.removeEventListener("keydown", shortcut);
    };
  }, []);
  useEffect(() => {
    if (!ready) return;
    try {
      localStorage.setItem("command-tasks-v1", JSON.stringify(tasks));
      localStorage.setItem("command-notes-v1", JSON.stringify(notes));
      localStorage.setItem("command-links-v1", JSON.stringify(links));
      localStorage.setItem("command-light", JSON.stringify(light));
    } catch {
      setNotice(
        "Browser storage is unavailable. Export your data before leaving.",
      );
    }
  }, [tasks, notes, links, light, ready]);
  useEffect(() => {
    if (!running) return;
    const deadline = Date.now() + seconds * 1000;
    const timer = setInterval(() => {
      const left = Math.max(0, Math.ceil((deadline - Date.now()) / 1000));
      setSeconds(left);
      if (left === 0) {
        setRunning(false);
        setNotice("Focus session complete. Time to stretch your legs.");
      }
    }, 250);
    return () => clearInterval(timer);
  }, [running]);
  useEffect(() => {
    if (!settings) return;
    const previous = document.activeElement as HTMLElement | null;
    const trap = (event: KeyboardEvent) => {
      if (event.key !== "Tab") return;
      const nodes = document.querySelectorAll<HTMLElement>(
        ".settings-modal button,.settings-modal input:not([hidden]),.settings-modal a",
      );
      const first = nodes[0],
        last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last?.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first?.focus();
      }
    };
    document.addEventListener("keydown", trap);
    return () => {
      document.removeEventListener("keydown", trap);
      previous?.focus();
    };
  }, [settings]);
  useEffect(() => {
    if (!notice) return;
    const t = setTimeout(() => setNotice(""), 7000);
    return () => clearTimeout(t);
  }, [notice]);
  const open = tasks.filter((t) => !t.done && t.kind === "task"),
    waiting = tasks.filter((t) => !t.done && t.kind === "waiting"),
    done = tasks.filter((t) => t.done);
  const visible = tasks
    .filter((t) =>
      filter === "done"
        ? t.done
        : filter === "waiting"
          ? !t.done && t.kind === "waiting"
          : !t.done && t.kind === "task",
    )
    .filter((t) => t.text.toLowerCase().includes(query.toLowerCase()))
    .sort(
      (a, b) => Number(b.priority === "high") - Number(a.priority === "high"),
    );
  const today = now?.toLocaleDateString("en-US", {
    timeZone: "America/Chicago",
    weekday: "long",
    month: "long",
    day: "numeric",
  });
  const hour = now
    ? Number(
        new Intl.DateTimeFormat("en-US", {
          timeZone: "America/Chicago",
          hour: "numeric",
          hourCycle: "h23",
        }).format(now),
      )
    : 12;
  function addCapture(e: React.FormEvent) {
    e.preventDefault();
    if (!capture.trim()) return;
    if (kind === "note")
      setNotes((n) => [
        {
          id: crypto.randomUUID(),
          text: capture.trim(),
          created: new Date().toISOString(),
        },
        ...n,
      ]);
    else
      setTasks((t) => [
        ...t,
        {
          id: crypto.randomUUID(),
          text: capture.trim(),
          done: false,
          priority: priority ? "high" : "normal",
          kind: kind === "waiting" ? "waiting" : "task",
        },
      ]);
    setCapture("");
    setNotice(kind === "note" ? "Note captured." : "Added to your list.");
  }
  function exportData() {
    const blob = new Blob(
      [JSON.stringify({ version: 1, tasks, notes, links }, null, 2)],
      { type: "application/json" },
    );
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `command-center-${chicagoDay()}.json`;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 1000);
  }
  function safeUrl(s: string) {
    try {
      return ["https:", "http:"].includes(new URL(s).protocol);
    } catch {
      return false;
    }
  }
  async function importData(file?: File) {
    if (!file) return;
    try {
      if (file.size > 2e6) throw Error();
      const data = JSON.parse(await file.text());
      if (
        data.version !== 1 ||
        !Array.isArray(data.tasks) ||
        !Array.isArray(data.notes) ||
        !Array.isArray(data.links) ||
        data.tasks.length > 2000 ||
        data.notes.length > 2000 ||
        data.links.length > 100
      )
        throw Error();
      if (
        !data.tasks.every(
          (t: any) =>
            typeof t.id === "string" &&
            typeof t.text === "string" &&
            t.text.length <= 2000 &&
            typeof t.done === "boolean" &&
            ["normal", "high"].includes(t.priority) &&
            ["task", "waiting"].includes(t.kind),
        ) ||
        !data.notes.every(
          (n: any) =>
            typeof n.id === "string" &&
            typeof n.text === "string" &&
            n.text.length <= 2000 &&
            typeof n.created === "string",
        ) ||
        !data.links.every(
          (l: any) =>
            typeof l.name === "string" &&
            typeof l.abbr === "string" &&
            typeof l.color === "string" &&
            /^#[a-f\d]{6}$/i.test(l.color) &&
            safeUrl(l.url),
        )
      )
        throw Error();
      setTasks(data.tasks);
      setNotes(data.notes);
      setLinks(data.links);
      setNotice("Backup restored.");
    } catch {
      setNotice("That file is not a valid Command Center backup.");
    }
    if (fileRef.current) fileRef.current.value = "";
  }
  async function refreshAgenda() {
    if (Date.now() < retryAt) return;
    setAgendaState("loading");
    setAgendaMessage("");
    try {
      const day = chicagoDay();
      const chicago = new Date(
        new Date().toLocaleString("en-US", { timeZone: "America/Chicago" }),
      );
      const utc = new Date(
        new Date().toLocaleString("en-US", { timeZone: "UTC" }),
      );
      const hours = Math.round((chicago.getTime() - utc.getTime()) / 3600000);
      const offset = `${hours < 0 ? "-" : "+"}${String(Math.abs(hours)).padStart(2, "0")}:00`;
      const res = await fetch("/api/agenda", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ day, offset }),
      });
      const data = (await res.json()) as {
        status: string;
        message?: string;
        retryAfterMs?: number;
        chatGPTSignInPath?: string;
        result?: unknown;
      };
      if (data.status !== "success") {
        if (
          data.status === "reauthentication_required" &&
          typeof data.chatGPTSignInPath === "string" &&
          data.chatGPTSignInPath.startsWith("/signin-with-chatgpt?")
        )
          setAuthPath(data.chatGPTSignInPath);
        if (data.retryAfterMs) setRetryAt(Date.now() + data.retryAfterMs);
        throw Object.assign(
          new Error(data.message || "Calendar is unavailable."),
          { status: data.status },
        );
      }
      setAgenda(parseEvents(data.result));
      setAgendaState("success");
      setRefreshed(
        new Date().toLocaleTimeString("en-US", {
          timeZone: "America/Chicago",
          hour: "numeric",
          minute: "2-digit",
        }),
      );
    } catch (e: any) {
      setAgendaState(
        e.status === "reauthentication_required" ? "auth" : "error",
      );
      setAgendaMessage(
        e.status === "binding_unavailable"
          ? "Live calendar is available on the hosted dashboard. Open Calendar for now."
          : e.message || "Unable to reach Calendar.",
      );
    }
  }
  const section = (name: string) => view === "overview" || view === name;
  return (
    <div className={light ? "app light" : "app"} data-command-realm="work" style={{"--realm-art": "url(/art/middle-earth-council-display-3840.jpg)"} as React.CSSProperties}>
      <aside className="sidebar">
        <a className="brand" href="#" onClick={() => setView("overview")}>
          <span className="ring-mark">A</span>
          <span>
            THE COMMAND
            <br />
            <strong>CENTER</strong>
          </span>
        </a>
        <nav className="realm-switch" aria-label="Command center"><a href={PERSONAL_URL}>The Shire</a><a href={WORK_URL} aria-current="page">Work</a></nav>
        <div className="side-label">YOUR DAILY COUNCIL</div>
        <nav aria-label="Main navigation">
          {[
            ["overview", "Overview", LayoutDashboard],
            ["priorities", "My priorities", ListChecks],
            ["notes", "Field notes", NotebookPen],
            ["systems", "The beacons", RadioTower],
          ].map(([id, label, Icon]: any) => (
            <button
              key={id}
              className={view === id ? "nav-item active" : "nav-item"}
              onClick={() => setView(id)}
            >
              <Icon size={18} />
              {label}
              {id === "priorities" && (
                <span className="nav-count">{open.length}</span>
              )}
            </button>
          ))}
        </nav>
        <div className="side-divider" />
        <div className="side-label">THE FELLOWSHIP</div>
        <nav className="fellowship-nav" aria-label="The Fellowship">
          {fellowshipNames.map((name) => {
            const link =
              links.find((l) => l.name === name) ||
              initialLinks.find((l) => l.name === name)!;
            return (
              <a
                className="side-link"
                href={accountLink(link.url, WORK_EMAIL, link.name)}
                key={name}
                target="_blank"
                rel="noreferrer"
                aria-label={`Open ${name}`}
              >
                <BrandLogo name={name} className="sidebar-logo" />
                <span>{name}</span>
                <ArrowUpRight size={13} />
              </a>
            );
          })}
        </nav>
        <div className="sidebar-bottom">
          <div className="side-quote">
            <Mountain size={25} />
            <p>
              One thing at a time.
              <br />
              Even Mordor had a queue.
            </p>
          </div>
          <button className="nav-item" onClick={() => setSettings(true)}>
            <Settings size={18} />
            Settings & backup
          </button>
          <div className="profile">
            <span className="avatar">AD</span>
            <div>
              <strong>Adam Dickerson</strong>
              <small className="realm-account">{WORK_EMAIL}</small>
            </div>
            <span className="presence" />
          </div>
        </div>
      </aside>
      <main>
        <header className="topbar">
          <div className="breadcrumb">
            Your workspace <ChevronRight size={13} />
            <span>
              {
                (
                  {
                    overview: "Overview",
                    priorities: "My priorities",
                    notes: "Field notes",
                    systems: "The beacons",
                  } as any
                )[view]
              }
            </span>
          </div>
          <div className="top-actions">
            <span className="top-date">{today}</span>
            <button
              className="icon-btn"
              aria-label={light ? "Use dark theme" : "Use light theme"}
              onClick={() => setLight((v) => !v)}
            >
              {light ? <Moon size={18} /> : <Sun size={18} />}
            </button>
            <span className="avatar small">AD</span>
          </div>
        </header>
        <div className="content">
          <section className="hero">
            <div className="hero-shade" />
            <div className="hero-copy">
              <div className="eyebrow">
                <span /> THE COUNCIL IS IN SESSION
              </div>
              <h1>
                Adam’s Middle-earth
                <br />
                <em>Command Center</em>
              </h1>
              <p>
                Good{" "}
                {hour < 12 ? "morning" : hour < 17 ? "afternoon" : "evening"},
                Adam. All that is yours to do, see, and know.
              </p>
              <button
                className="gold-button"
                onClick={() => {
                  setView("priorities");
                  setTimeout(() => captureRef.current?.focus(), 80);
                }}
              >
                Set the day in motion <ArrowRight size={16} />
              </button>
            </div>
            <div className="hero-stamp">
              <span className="stamp-ring">✧</span>
              <span>
                YOUR CORNER
                <br />
                OF MIDDLE-EARTH
              </span>
            </div>
            <div className="hero-bottom">
              <span>
                <Shield size={13} /> Work command center
              </span>
              <a
                className="landscape-link"
                href="/art/middle-earth-council-display-3840.jpg"
                target="_blank"
                rel="noreferrer"
              >
                View full scene <ArrowUpRight size={12} />
              </a>
              <span>
                {now?.toLocaleTimeString("en-US", {
                  timeZone: "America/Chicago",
                  hour: "numeric",
                  minute: "2-digit",
                }) || "—"}{" "}
                <span className="muted">· Central time</span>
              </span>
            </div>
          </section>
          <div className="summary-strip">
            <div>
              <span className="stat-icon">
                <ListChecks size={20} />
              </span>
              <div>
                <strong>
                  {open.length}
                  <small>open priorities</small>
                </strong>
                <span>
                  {open.filter((t) => t.priority === "high").length} marked
                  important
                </span>
              </div>
            </div>
            <div>
              <span className="stat-icon">
                <Clock3 size={20} />
              </span>
              <div>
                <strong>
                  {waiting.length}
                  <small>waiting on others</small>
                </strong>
                <span>Keep the loose ends in sight</span>
              </div>
            </div>
            <div>
              <span className="stat-icon">
                <Check size={20} />
              </span>
              <div>
                <strong>
                  {done.length}
                  <small>completed</small>
                </strong>
                <span>A little progress counts</span>
              </div>
            </div>
            <button
              className="summary-action"
              onClick={() => {
                setView("overview");
                setTimeout(() => captureRef.current?.focus(), 80);
              }}
            >
              <Plus size={17} />
              Quick capture
            </button>
          </div>
          <div className="dashboard-toolbar">
            <div>
              <h2>
                {view === "overview"
                  ? "The day, at a glance"
                  : view === "priorities"
                    ? "Give the important things room"
                    : view === "notes"
                      ? "A place for the loose threads"
                      : "Keep an eye on the beacons"}
              </h2>
              <p>
                {view === "overview"
                  ? "Less noise. More getting things done."
                  : view === "systems"
                    ? "Connection states, without the guesswork."
                    : "Saved on this browser. Export a backup anytime."}
              </p>
            </div>
            <div className="search-box">
              <Search size={16} />
              <input
                ref={searchRef}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Find a task, note, or tool…"
                aria-label="Search dashboard"
              />
              <kbd>⌘ K</kbd>
              {query && (
                <button
                  className="icon-btn"
                  aria-label="Clear search"
                  onClick={() => setQuery("")}
                >
                  <X size={14} />
                </button>
              )}
            </div>
          </div>
          {view === "overview" && <DailyCouncil />}
          <div className={`dashboard-grid dashboard-view-${view}`}>
            <div className="dashboard-stack dashboard-day">
            {section("priorities") && (
              <section className="panel priorities realm-framed realm-elven">
                <PanelFrame kind="elven" />
                <div className="panel-heading">
                  <div>
                    <span className="section-icon">
                      <ListChecks size={17} />
                    </span>
                    <h3>My priorities</h3>
                  </div>
                  <span className="tag">PERSONAL</span>
                </div>
                <QuestSeal done={done.length} total={tasks.length} />
                <div
                  className="task-tabs"
                  role="group"
                  aria-label="Task filter"
                >
                  {[
                    ["open", "To do", open.length],
                    ["waiting", "Waiting", waiting.length],
                    ["done", "Done", done.length],
                  ].map(([id, label, count]) => (
                    <button
                      key={id}
                      className={filter === id ? "selected" : ""}
                      aria-pressed={filter === id}
                      onClick={() => setFilter(String(id))}
                    >
                      {label}
                      <span>{count}</span>
                    </button>
                  ))}
                </div>
                <div className="task-list">
                  {visible.length ? (
                    visible.map((t) => (
                      <div
                        className={"task " + (t.done ? "task-done" : "")}
                        key={t.id}
                      >
                        <button
                          className="check-button"
                          aria-label={
                            t.done ? `Reopen ${t.text}` : `Complete ${t.text}`
                          }
                          onClick={() =>
                            setTasks((ts) =>
                              ts.map((x) =>
                                x.id === t.id ? { ...x, done: !x.done } : x,
                              ),
                            )
                          }
                        >
                          {t.done && <Check size={13} />}
                        </button>
                        <span>{t.text}</span>
                        <button
                          className={
                            "priority-toggle " +
                            (t.priority === "high" ? "important" : "")
                          }
                          aria-label={`Toggle importance for ${t.text}`}
                          title="Toggle importance"
                          onClick={() =>
                            setTasks((ts) =>
                              ts.map((x) =>
                                x.id === t.id
                                  ? {
                                      ...x,
                                      priority:
                                        x.priority === "high"
                                          ? "normal"
                                          : "high",
                                    }
                                  : x,
                              ),
                            )
                          }
                        >
                          <Flame size={14} />
                        </button>
                        <button
                          className="remove"
                          aria-label={`Delete ${t.text}`}
                          onClick={() =>
                            setTasks((ts) => ts.filter((x) => x.id !== t.id))
                          }
                        >
                          <X size={13} />
                        </button>
                      </div>
                    ))
                  ) : (
                    <div className="empty compact">
                      <Check size={23} />
                      <p>
                        {query
                          ? "No matching tasks."
                          : filter === "done"
                            ? "Your completed work will land here."
                            : filter === "waiting"
                              ? "Nothing waiting in the wings."
                              : "Clear skies. Add your next priority below."}
                      </p>
                    </div>
                  )}
                </div>
                <button
                  className="text-action"
                  onClick={() => {
                    setKind(filter === "waiting" ? "waiting" : "task");
                    captureRef.current?.focus();
                  }}
                >
                  <Plus size={15} />
                  Add a {filter === "waiting" ? "waiting item" : "priority"}
                </button>
                <div className="panel-foot">
                  <span className="local-dot" /> Browser storage{" "}
                  <span>
                    {done.length} / {tasks.length} complete
                  </span>
                </div>
              </section>
            )}
            {(view === "overview" || view === "systems") && (
              <section className="panel agenda realm-framed realm-elven">
                <PanelFrame kind="elven" />
                <div className="panel-heading">
                  <div>
                    <CalendarDays size={17} />
                    <h3>Today’s agenda</h3>
                  </div>
                  <a
                    href={accountLink("https://calendar.google.com/calendar/r/day", WORK_EMAIL)}
                    target="_blank"
                    rel="noreferrer"
                    aria-label="Open Google Calendar"
                  >
                    <ArrowUpRight size={17} />
                  </a>
                </div>
                <div className="agenda-date">
                  <strong>
                    {now?.toLocaleDateString("en-US", {
                      timeZone: "America/Chicago",
                      weekday: "long",
                    }) || "Today"}
                  </strong>
                  <span>
                    {now?.toLocaleDateString("en-US", {
                      timeZone: "America/Chicago",
                      month: "short",
                      day: "numeric",
                    })}{" "}
                    · Central
                  </span>
                </div>
                <DayDial
                  now={now}
                  events={agenda.length}
                  loaded={agendaState === "success"}
                />
                {agenda.length > 0 ? (
                  <div className="events">
                    {agenda.map((e, i) => (
                      <a
                        key={i}
                        className="event"
                        href={
                          accountLink(safeUrl(e.htmlLink || e.url || e.display_url || "")
                            ? e.htmlLink || e.url || e.display_url
                            : "https://calendar.google.com/calendar/r", WORK_EMAIL)
                        }
                        target="_blank"
                        rel="noreferrer"
                      >
                        <time>{eventTime(e)}</time>
                        <span>
                          <strong>
                            {e.summary || e.title || "Untitled event"}
                          </strong>
                          <small>Google Calendar</small>
                        </span>
                        <ArrowUpRight size={13} />
                      </a>
                    ))}
                  </div>
                ) : (
                  <div className="empty agenda-empty">
                    <CalendarDays size={27} />
                    <strong>
                      {agendaState === "success"
                        ? "Room to breathe."
                        : "Bring your day into view."}
                    </strong>
                    <p>
                      {agendaState === "success"
                        ? "No events returned for today."
                        : "Load today’s events from your primary Google Calendar."}
                    </p>
                  </div>
                )}
                {agendaMessage && (
                  <p className="connection-message" role="status">
                    {agendaMessage}
                    {refreshed && " Showing the last successful refresh."}
                  </p>
                )}
                {agendaState === "auth" && authPath && (
                  <a
                    className="outline-button full"
                    href={authPath}
                    target="_top"
                  >
                    Sign in to connect Calendar
                    <ArrowUpRight size={14} />
                  </a>
                )}
                <button
                  className="outline-button full"
                  onClick={refreshAgenda}
                  disabled={agendaState === "loading" || Date.now() < retryAt}
                >
                  <RefreshCw
                    size={14}
                    className={agendaState === "loading" ? "spin" : ""}
                  />
                  {agendaState === "loading"
                    ? "Checking your calendar…"
                    : refreshed
                      ? "Refresh agenda"
                      : "Load my calendar"}
                </button>
                <div className="panel-foot">
                  <span>
                    {refreshed
                      ? `Updated ${refreshed}`
                      : "Read-only · loads on request"}
                  </span>
                  {agendaState === "error" && <span>Unavailable</span>}
                </div>
              </section>
            )}
            </div>
            {(view === "overview" || view === "systems") && <Operations />}
            <div className="dashboard-stack dashboard-support">
            {view === "overview" && <Palantir />}
            {(section("notes") || view === "priorities") && (
              <section className="panel capture realm-framed realm-oak">
                <PanelFrame kind="oak" />
                <div className="panel-heading">
                  <div>
                    <NotebookPen size={17} />
                    <h3>Quick capture</h3>
                  </div>
                  <StudyArt name="quill-inkwell" className="capture-quill-art" />
                </div>
                <form onSubmit={addCapture}>
                  <label className="capture-label" htmlFor="quick-capture">Your field note</label>
                  <textarea
                    id="quick-capture"
                    ref={captureRef}
                    value={capture}
                    onChange={(e) => setCapture(e.target.value)}
                    maxLength={2000}
                    placeholder="A task, a thought, something you don’t want to forget…"
                    aria-label="Capture a task or note"
                    required
                  />
                  <div className="capture-options">
                    <label className="capture-type-label">
                    <span>Capture type</span>
                    <select
                      value={kind}
                      onChange={(e) => setKind(e.target.value)}
                      aria-label="Capture type"
                    >
                      <option value="task">New priority</option>
                      <option value="waiting">Waiting on someone</option>
                      <option value="note">Field note</option>
                    </select>
                    </label>
                    {kind !== "note" && (
                      <button
                        type="button"
                        className={
                          "flag-button " + (priority ? "important" : "")
                        }
                        aria-pressed={priority}
                        onClick={() => setPriority((v) => !v)}
                      >
                        <Flame size={15} />
                        Important
                      </button>
                    )}
                    <button
                      className="gold-button"
                      type="submit"
                      disabled={!capture.trim()}
                    >
                      <Plus size={15} />
                      Capture
                    </button>
                  </div>
                </form>
                <div className="notes-header">
                  <span>RECENT FIELD NOTES</span>
                  <span>{notes.length}</span>
                </div>
                <div className="notes-list">
                  {notes
                    .filter((n) =>
                      n.text.toLowerCase().includes(query.toLowerCase()),
                    )
                    .slice(0, view === "notes" ? 100 : 3)
                    .map((n) => (
                      <article className="note" key={n.id}>
                        <div>
                          <p>{n.text}</p>
                          <small>
                            {new Date(n.created).toLocaleDateString("en-US", {
                              month: "short",
                              day: "numeric",
                            })}
                          </small>
                        </div>
                        <button
                          className="remove"
                          aria-label="Delete note"
                          onClick={() =>
                            setNotes((ns) => ns.filter((x) => x.id !== n.id))
                          }
                        >
                          <X size={13} />
                        </button>
                      </article>
                    ))}
                  {!notes.length && (
                    <p className="no-notes">
                      Good ideas rarely arrive on schedule. Catch them here.
                    </p>
                  )}
                </div>
              </section>
            )}
            </div>
          </div>
          {view === "overview" && (
            <>
              <section className="tools-section realm-framed realm-dwarven">
                <PanelFrame kind="dwarven" />
                <div className="tools-title">
                  <h3>The Fellowship & daily tools</h3>
                  <button
                    className="text-action"
                    onClick={() => setSettings(true)}
                  >
                    Customize
                    <Settings size={13} />
                  </button>
                </div>
                <div className="tool-groups">
                  {[
                    { title: "The Fellowship", names: fellowshipNames },
                    {
                      title: "Daily tools",
                      names: [
                        "Gmail",
                        "Calendar",
                        "Slack",
                        "Drive",
                        "Zoom",
                        "Vercel",
                      ],
                    },
                    {
                      title: "AI council",
                      names: ["ChatGPT", "Claude", "Gemini"],
                    },
                  ].map((group) => (
                    <div className="tool-group" key={group.title}>
                      <h4>{group.title}</h4>
                      <div className="tool-grid">
                        {group.names
                          .map((name) => links.find((l) => l.name === name))
                          .filter(
                            (l): l is Link =>
                              !!l &&
                              l.name
                                .toLowerCase()
                                .includes(query.toLowerCase()),
                          )
                          .map((l) => (
                            <a
                              className="tool"
                              key={l.name}
                              href={accountLink(l.url, WORK_EMAIL, l.name)}
                              target="_blank"
                              rel="noreferrer"
                            >
                              <span className="tool-logo">
                                <BrandLogo name={l.name} />
                              </span>
                              <span>{l.name}</span>
                              <ArrowUpRight size={13} />
                            </a>
                          ))}
                      </div>
                    </div>
                  ))}
                </div>
              </section>
              <section className="focus-band realm-framed realm-oak">
                <PanelFrame kind="oak" />
                <div className="focus-intro">
                  <span className="focus-icon">
                    <PolishArt name="hourglass" className="focus-hourglass-art" />
                  </span>
                  <div>
                    <h3>A little room to focus.</h3>
                    <p>One task. Twenty-five minutes. The rest can wait.</p>
                  </div>
                </div>
                <div className="timer-controls">
                  <span className="timer">
                    {String(Math.floor(seconds / 60)).padStart(2, "0")}
                    <span>:</span>
                    {String(seconds % 60).padStart(2, "0")}
                  </span>
                  <button
                    className="outline-button"
                    onClick={() => {
                      if (seconds === 0) setSeconds(1500);
                      setRunning((v) => !v);
                    }}
                  >
                    {running ? <Pause size={14} /> : <Play size={14} />}{" "}
                    {running ? "Pause" : "Start focus"}
                  </button>
                  <button
                    className="icon-btn"
                    aria-label="Reset focus timer"
                    onClick={() => {
                      setRunning(false);
                      setSeconds(1500);
                    }}
                  >
                    <RotateCcw size={16} />
                  </button>
                </div>
              </section>
            </>
          )}
          <footer>
            <span>
              <span className="footer-ring">◌</span> Not all those who wander
              are lost.
            </span>
            <span>
              Built for your day.{" "}
              <span className="muted">One step at a time.</span>
            </span>
          </footer>
        </div>
      </main>
      {notice && (
        <div className="toast" role="status">
          <Check size={17} />
          {notice}
          <button
            className="icon-btn"
            aria-label="Dismiss notification"
            onClick={() => setNotice("")}
          >
            <X size={15} />
          </button>
        </div>
      )}
      {settings && (
        <div className="modal-backdrop" onClick={() => setSettings(false)}>
          <section
            className="settings-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="settings-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="panel-heading">
              <div>
                <Settings size={18} />
                <h3 id="settings-title">Make it yours</h3>
              </div>
              <button
                autoFocus
                className="icon-btn"
                aria-label="Close settings"
                onClick={() => setSettings(false)}
              >
                <X size={18} />
              </button>
            </div>
            <p className="settings-description">
              Tasks, field notes, and shortcuts are saved in this browser.
              Backups let you move them to another device.
            </p>
            <div className="backup-actions">
              <button className="outline-button" onClick={exportData}>
                <Download size={15} />
                Export backup
              </button>
              <button
                className="outline-button"
                onClick={() => fileRef.current?.click()}
              >
                <Upload size={15} />
                Restore backup
              </button>
              <input
                ref={fileRef}
                type="file"
                accept="application/json"
                hidden
                onChange={(e) => importData(e.target.files?.[0])}
              />
            </div>
            <p className="settings-description">
              Restoring a backup replaces this browser’s current lists and
              shortcuts.
            </p>
            <h4>App shortcuts</h4>
            <div className="link-editor">
              {links.map((l, i) => (
                <label key={l.name}>
                  {l.name}
                  <input
                    type="url"
                    defaultValue={l.url}
                    aria-label={`${l.name} URL`}
                    onBlur={(e) => {
                      const value = e.target.value.trim();
                      if (safeUrl(value))
                        setLinks((ls) =>
                          ls.map((x, j) =>
                            j === i ? { ...x, url: value } : x,
                          ),
                        );
                      else {
                        e.target.value = l.url;
                        setNotice("Use a full https:// or http:// address.");
                      }
                    }}
                  />
                </label>
              ))}
            </div>
            <div className="settings-bottom">
              <span>
                <Shield size={14} /> No credentials stored in this page.
              </span>
              <button
                className="gold-button"
                onClick={() => setSettings(false)}
              >
                Done
                <Check size={14} />
              </button>
            </div>
          </section>
        </div>
      )}
    </div>
  );
}

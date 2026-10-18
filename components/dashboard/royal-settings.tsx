"use client";
import { useEffect, useRef, useState } from "react";
import { ArrowUpRight, Check, RefreshCw, Settings, Shield, X } from "./realm-icon";
import { Download, Upload, Link2, Sun, Moon, KeyRound } from "lucide-react";
import { GondorCrest } from "./gondor-crest";
import { BrandLogo } from "./brand-logo";
import { CONNECTIONS, connectionReadOutcome, type ConnectionId } from "@/lib/connection-registry.mjs";
import { connectionEnabled, setConnectionEnabled, useConnectionEnabled } from "./connection-preferences";

type Probe = ReturnType<typeof connectionReadOutcome> & { checkedAt?: string; retryAt?: number; busy?: boolean };
type Access = { id: ConnectionId; access: string };
const managementGuide = "https://help.openai.com/en/articles/20001494-connecting-and-managing-app-accounts-in-chatgpt";

function ConnectionCard({ source, access, probe, check, notify }: {
  source: typeof CONNECTIONS[number]; access?: string; probe?: Probe;
  check: () => void; notify: (message: string) => void;
}) {
  const enabled = useConnectionEnabled(source.id);
  const text = !enabled ? "Paused here" : probe?.busy ? "Checking…" : probe?.state === "confirmed" ? "Read confirmed" : probe?.state === "partial" ? "Partial access" : probe?.state === "auth" ? "Sign-in needed" : probe ? "Not confirmed" : access === "available" ? "Available · not checked" : access === "configured" ? "Configured · on demand" : access === "not_configured" ? "Not configured" : access === "disabled" ? "Disabled by policy" : "Not checked";
  return <article className="registry-connection">
    <div className="registry-connection-heading"><span className="registry-logo"><BrandLogo name={source.logo} /></span><div><h4>{source.name}</h4><span className={`registry-status ${!enabled ? "paused" : probe?.state || "unknown"}`}><i />{text}</span></div></div>
    <p>{source.purpose}</p><small>{source.scope}</small>
    <label className="registry-toggle"><span>Use in dashboard</span><input type="checkbox" aria-label={`Use ${source.name} in dashboard`} checked={enabled} onChange={(e) => { try { setConnectionEnabled(source.id, e.target.checked); } catch { notify("Browser storage is unavailable; the setting was not saved."); } }} /></label>
    <div className="registry-card-actions">
      {source.id !== "council" && <button className="outline-button" onClick={check} disabled={!enabled || !!probe?.busy || Date.now() < (probe?.retryAt || 0)}><RefreshCw size={14} className={probe?.busy ? "spin" : ""} />{probe?.busy ? "Checking…" : "Check connection"}</button>}
      <a className="registry-text-link" href={source.url} target="_blank" rel="noopener noreferrer">Open app <ArrowUpRight size={14} /></a>
    </div>
    {probe && <p className="registry-probe" role="status">{!enabled ? "Future dashboard reads are paused. Existing results remain saved." : probe.message}{probe.checkedAt && !probe.busy && <small>{new Date(probe.checkedAt).toLocaleTimeString("en-US", {hour:"numeric",minute:"2-digit",timeZone:"America/Chicago"})} CT{probe.retryAt && Date.now() < probe.retryAt ? " · check cooling down" : ""}</small>}</p>}
    {enabled && probe?.state === "auth" && probe.authPath && <a href={probe.authPath} target="_top" className="outline-button">Reconnect securely <ArrowUpRight size={14}/></a>}
  </article>;
}

export function RoyalSettings({ close, links, updateLink, light, setLight, exportData, importData, notify }: {
  close: () => void; links: { name: string; url: string }[]; updateLink: (index: number, value: string) => boolean;
  light: boolean; setLight: (light: boolean) => void; exportData: () => void; importData: (file?: File) => Promise<void>; notify: (message: string) => void;
}) {
  const [section, setSection] = useState("connections");
  const [access, setAccess] = useState<Access[]>([]);
  const [accessBusy, setAccessBusy] = useState(false);
  const [accessMessage, setAccessMessage] = useState("");
  const [authPath, setAuthPath] = useState("");
  const [probes, setProbes] = useState<Partial<Record<ConnectionId, Probe>>>({});
  const [, setTick] = useState(0);
  const dialog = useRef<HTMLElement>(null), file = useRef<HTMLInputElement>(null);
  const active = useRef(new Set<ConnectionId>()), controllers = useRef(new Set<AbortController>()), accessActive = useRef(false);
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const overflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    dialog.current?.querySelector<HTMLElement>('button[aria-label="Close settings"]')?.focus();
    const trap = (event: KeyboardEvent) => {
      if (event.key === "Escape") close();
      if (event.key !== "Tab") return;
      const nodes = [...(dialog.current?.querySelectorAll<HTMLElement>('button:not(:disabled),a[href],input:not([hidden]),select,textarea') || [])].filter((node) => node.getClientRects().length && node.tabIndex >= 0);
      const first = nodes[0], last = nodes[nodes.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    };
    document.addEventListener("keydown", trap);
    const timer = setInterval(() => setTick((n) => n + 1), 1000);
    return () => { document.body.style.overflow = overflow; document.removeEventListener("keydown", trap); clearInterval(timer); controllers.current.forEach((c) => c.abort()); previous?.focus(); };
  }, []);
  async function refreshAccess() {
    if (accessActive.current) return;
    accessActive.current = true; setAccessBusy(true); setAuthPath("");
    const controller = new AbortController(); controllers.current.add(controller);
    const timer = setTimeout(() => controller.abort(), 30000);
    try {
      const response = await fetch("/api/connections", { method: "POST", signal: controller.signal });
      const data = await response.json() as {status?: string; connections?: Access[]; message?: string; chatGPTSignInPath?: string};
      if (Array.isArray(data.connections)) setAccess(data.connections);
      setAccessMessage(data.status === "success" ? "Site availability refreshed. Check a read below to confirm provider access." : data.message || "Site availability could not be confirmed. Existing settings are still available.");
      if (data.status === "reauthentication_required" && data.chatGPTSignInPath?.startsWith("/signin-with-chatgpt?")) setAuthPath(data.chatGPTSignInPath);
    } catch { if (!controller.signal.aborted) setAccessMessage("Availability could not be loaded. Try again when the connection returns."); }
    finally { clearTimeout(timer); controllers.current.delete(controller); accessActive.current = false; setAccessBusy(false); }
  }
  useEffect(() => { refreshAccess(); }, []);
  async function check(id: ConnectionId) {
    if (!connectionEnabled(id) || active.current.has(id) || id === "council" || Date.now() < (probes[id]?.retryAt || 0)) return;
    active.current.add(id);
    setProbes((old) => ({ ...old, [id]: {state:"unavailable",message:"Checking a read from your connected account…",busy:true} }));
    const controller = new AbortController(); controllers.current.add(controller);
    const timeout = setTimeout(() => controller.abort(), 65000);
    try {
      const endpoint = ["mail","slack","jira"].includes(id) ? `/api/daily/${id}` : id === "calendar" ? "/api/agenda" : `/api/operations/${id}`;
      let body: object = {};
      if (id === "calendar") {
        const day = new Intl.DateTimeFormat("en-CA",{timeZone:"America/Chicago",year:"numeric",month:"2-digit",day:"2-digit"}).format(new Date());
        const offsetPart = new Intl.DateTimeFormat("en-US",{timeZone:"America/Chicago",timeZoneName:"longOffset"}).formatToParts(new Date()).find((p) => p.type === "timeZoneName")?.value;
        body = {day,offset:offsetPart?.replace("GMT","") || "+00:00"};
      }
      const response = await fetch(endpoint, {method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(body),signal:controller.signal});
      const outcome = connectionReadOutcome(await response.json());
      setProbes((old) => ({...old,[id]:{...outcome,checkedAt:new Date().toISOString(),retryAt:Date.now()+Math.max(outcome.state === "confirmed" ? 10000 : 60000,outcome.retryAfterMs || 0)}}));
    } catch { if (!controller.signal.aborted) setProbes((old) => ({...old,[id]:{state:"unavailable",message:"The read did not finish. Access is not confirmed.",retryAt:Date.now()+60000}})); }
    finally { clearTimeout(timeout); controllers.current.delete(controller); active.current.delete(id); if (controller.signal.aborted) setProbes((old) => ({...old,[id]:{state:"unavailable",message:"The check timed out. No automatic retry was made.",retryAt:Date.now()+60000}})); }
  }
  const sections = [{id:"connections",name:"Connections",Icon:KeyRound},{id:"shortcuts",name:"Shortcuts",Icon:Link2},{id:"appearance",name:"Appearance",Icon:Sun},{id:"backup",name:"Backup",Icon:Shield}];
  return <div className="modal-backdrop royal-backdrop" onClick={close}>
    <section ref={dialog} className="settings-modal royal-registry" role="dialog" aria-modal="true" aria-labelledby="settings-title" onClick={(e) => e.stopPropagation()}>
      <header className="registry-header"><GondorCrest /><div><span className="registry-eyebrow">THE KEYS TO YOUR KINGDOM</span><h3 id="settings-title">The Royal Registry</h3><p>Connections, preferences & the tools of your trade.</p></div><button className="icon-btn" aria-label="Close settings" onClick={close}><X size={20}/></button></header>
      <div className="registry-layout"><nav className="registry-nav" aria-label="Settings sections">{sections.map(({id,name,Icon})=><button key={id} aria-pressed={section===id} onClick={()=>setSection(id)}><Icon size={18}/><span>{name}</span></button>)}<div className="registry-account"><Shield size={18}/><strong>Adam’s private realm</strong><span>dickersona@76fireworks.com</span></div></nav>
      <div className="registry-content" key={section}>
        {section === "connections" && <><div className="registry-section-heading"><div><span className="registry-eyebrow">KEEP THE GATES IN ORDER</span><h4>Your connections</h4><p>Choose what the dashboard can read on this browser.</p></div><button className="outline-button" onClick={refreshAccess} disabled={accessBusy}><RefreshCw size={14} className={accessBusy?"spin":""}/>{accessBusy?"Refreshing…":"Refresh access"}</button></div>
          <details className="registry-guidance"><summary><KeyRound size={18}/><strong>Manage account authorization</strong></summary><div><p>Pause dashboard reads here. To change an account, grant access, or disconnect it everywhere, use ChatGPT’s Settings → Plugins.</p><a href="https://chatgpt.com/plugins" target="_blank" rel="noopener noreferrer">Manage apps in ChatGPT <ArrowUpRight size={13}/></a><a href={managementGuide} target="_blank" rel="noopener noreferrer">Connection guide <ArrowUpRight size={13}/></a></div></details>
          {accessMessage && <p className="registry-access-message" role="status">{accessMessage}</p>}{authPath && <a className="outline-button" href={authPath} target="_top">Sign in with ChatGPT <ArrowUpRight size={14}/></a>}
          <div className="registry-connections">{CONNECTIONS.map((source)=><ConnectionCard key={source.id} source={source} access={access.find((item)=>item.id===source.id)?.access} probe={probes[source.id]} check={()=>check(source.id)} notify={notify}/>)}</div>
          <p className="registry-footnote">Availability describes Site access. Only a confirmed read verifies a provider response. Vercel reads are scoped to the spirit-of-76-fireworks team.</p></>}
        {section === "shortcuts" && <><div className="registry-section-heading"><div><span className="registry-eyebrow">YOUR ROADS BEYOND THE CITY</span><h4>App shortcuts</h4><p>Set the exact destination for each tool. Changes save when you leave a field.</p></div></div><div className="registry-shortcuts">{links.map((link,index)=><label key={link.name}><span><BrandLogo name={link.name}/>{link.name}</span><input type="url" defaultValue={link.url} aria-label={`${link.name} URL`} onBlur={(e)=>{if(!updateLink(index,e.target.value.trim()))e.target.value=link.url;}}/></label>)}</div><p className="registry-footnote">A shortcut opens a website. Editing its address does not connect or authorize an account.</p></>}
        {section === "appearance" && <><div className="registry-section-heading"><div><span className="registry-eyebrow">THE LIGHT OVER GONDOR</span><h4>Choose your atmosphere</h4><p>The same silver city, in the light that suits your day.</p></div></div><div className="registry-appearances"><button className={!light?"chosen":""} aria-pressed={!light} onClick={()=>setLight(false)}><span className="atmosphere-preview dusk"><GondorCrest/></span><span><Moon size={18}/><strong>Citadel at dusk</strong></span><small>Charcoal, moonlit stone & royal silver</small>{!light&&<Check size={18}/>}</button><button className={light?"chosen":""} aria-pressed={light} onClick={()=>setLight(true)}><span className="atmosphere-preview dawn"><GondorCrest/></span><span><Sun size={18}/><strong>The White City</strong></span><small>Pearl limestone, slate & clear daylight</small>{light&&<Check size={18}/>}</button></div><p className="registry-footnote">Your choice is saved on this browser. The Royal Registry keeps its silver-and-charcoal finish in both modes.</p></>}
        {section === "backup" && <><div className="registry-section-heading"><div><span className="registry-eyebrow">THE ARCHIVES OF THE CITADEL</span><h4>Keep your work safe</h4><p>Move your personal lists and preferences between browsers.</p></div></div><div className="registry-backup-card"><Shield size={38}/><h4>A copy for your archives</h4><p>Back up tasks, field notes, app shortcuts, theme, and dashboard read preferences. Credentials and live provider data are never included.</p><div className="backup-actions"><button className="outline-button" onClick={exportData}><Download size={16}/>Export backup</button><button className="outline-button" onClick={()=>file.current?.click()}><Upload size={16}/>Restore backup</button><input ref={file} type="file" accept="application/json" hidden onChange={async(e)=>{await importData(e.target.files?.[0]);e.target.value="";}}/></div><p className="registry-footnote">Restoring replaces this browser’s current lists and shortcuts. Existing version 1 backups are supported. Saved invoice records remain in the private ledger.</p></div></>}
      </div></div>
      <footer className="registry-footer"><span><Shield size={15}/>Private by design · no credentials stored here</span><button className="gold-button" onClick={close}>Done<Check size={15}/></button></footer>
    </section>
  </div>;
}

"use client";
import { useState } from "react";
import { GondorArt } from "./gondor-art";
import { ArrowUpRight } from "./realm-icon";
import { providerData, records } from "@/lib/provider-data.mjs";
import { WATCHED_PROJECTS, VERCEL_TEAM_URL, type VercelDeployment, type VercelProfile, type WatchedProject } from "@/lib/vercel-feed.mjs";
import type { ConnectorResult } from "@/lib/connector-contract.mjs";

function list(result?: ConnectorResult): VercelDeployment[] | null {
  if (result?.status !== "success") return null;
  try { return records(result.result) as VercelDeployment[]; } catch { return null; }
}
function profile(result?: ConnectorResult): VercelProfile | null {
  if (result?.status !== "success") return null;
  try { return providerData(result.result) as VercelProfile; } catch { return null; }
}
function timestamp(value: number | null | undefined) {
  return value ? new Date(value).toLocaleString("en-US", { timeZone: "America/Chicago", month: "short", day: "numeric", year: "numeric", hour: "numeric", minute: "2-digit" }) + " CT" : "Not reported";
}
function State({ value }: { value?: string }) {
  return <span className={`citadel-state state-${(value || "unknown").toLowerCase()}`}>{value?.replaceAll("_", " ") || "UNAVAILABLE"}</span>;
}
function Deployment({ title, data, outcome, loading }: { title: string; data: VercelDeployment[] | null; outcome?: ConnectorResult; loading: boolean }) {
  const latest = data?.[0];
  return <div className="citadel-deployment">
    <span className="citadel-kicker">{title}</span>
    <State value={latest?.state || (data ? "NO DEPLOYMENTS" : undefined)} />
    <p>{latest ? timestamp(latest.createdAt) : loading ? "Reading Vercel…" : data ? "No deployments returned for this environment." : outcome?.status !== "success" ? outcome?.message || "Awaiting a confirmed read." : "Awaiting a confirmed read."}</p>
    {latest && <><small>{latest.target} · {latest.id}</small><a href={latest.href} target="_blank" rel="noopener noreferrer">Inspect deployment <ArrowUpRight size={13}/></a></>}
  </div>;
}
export function ProjectCenter({ watched, loading }: { watched?: WatchedProject[]; loading: boolean }) {
  const [choice, setSelected] = useState<string>("");
  const selected = choice || watched?.find((w) => w.profile.status === "success")?.key || WATCHED_PROJECTS[0].key;
  const watch = WATCHED_PROJECTS.find((w) => w.key === selected)!;
  const current = watched?.find((w) => w.key === selected);
  const details = profile(current?.profile), history = list(current?.deployments), production = list(current?.production);
  return <section className="citadel-center" aria-label="Gondor project information center">
    <div className="citadel-title"><GondorArt name="shield" size={64}/><div><span className="citadel-kicker">THE CITADEL WATCH</span><h4>Project information center</h4><p>{watched ? `${watched.filter((w) => w.profile.status === "success").length} of 3 projects visible to this connection` : "Three projects. One watch over the realm."}</p></div></div>
    <div className="citadel-projects" role="group" aria-label="Watched projects">
      {WATCHED_PROJECTS.map((w, i) => {
        const entry = watched?.find((item) => item.key === w.key), p = profile(entry?.profile), recent = list(entry?.deployments);
        const failures = recent?.filter((d) => ["ERROR", "BLOCKED"].includes(d.state)).length;
        return <button key={w.key} type="button" aria-pressed={selected === w.key} onClick={() => setSelected(w.key)}>
          <GondorArt name={i === 0 ? "overview" : i === 1 ? "shield" : "beacon"} size={64}/>
          <span className="citadel-project-copy"><strong>{w.label}</strong><small>{p?.name && p.name !== w.label ? `Vercel: ${p.name}` : p ? "Project connected" : loading ? "Reading project…" : "Access not confirmed"}</small></span>
          <State value={recent?.[0]?.state || (recent ? "NO DEPLOYMENTS" : undefined)}/>
          <span className="citadel-card-note">{recent ? `${recent.length} recent · ${failures} build ${failures === 1 ? "error" : "errors"}` : "Select to inspect details"}</span>
        </button>;
      })}
    </div>
    <div className="citadel-dossier" role="region" aria-label={`${watch.label} project details`} aria-busy={loading}>
      <div className="citadel-dossier-title"><div><span className="citadel-kicker">PROJECT DOSSIER</span><h4>{details?.name || watch.label}</h4></div>
        <a className="outline-button" href={details?.href || `${VERCEL_TEAM_URL}/${watch.names[0]}`} target="_blank" rel="noopener noreferrer">Open project<ArrowUpRight size={14}/></a>
      </div>
      {!details && <p className="citadel-notice" role="status">{loading ? "Reading project information from Vercel…" : current?.profile.status !== "success" ? current?.profile.message || "No project data has been confirmed yet." : "No project data has been confirmed yet."}</p>}
      <div className="citadel-deployment-grid">
        <Deployment title="LATEST PRODUCTION DEPLOYMENT" data={production} outcome={current?.production} loading={loading}/>
        <Deployment title="LATEST ACTIVITY · ALL ENVIRONMENTS" data={history} outcome={current?.deployments} loading={loading}/>
      </div>
      {details && <dl className="citadel-metadata"><div><dt>Framework</dt><dd>{details.framework}</dd></div><div><dt>Node version</dt><dd>{details.nodeVersion}</dd></div><div><dt>Project updated</dt><dd>{timestamp(details.updatedAt)}</dd></div></dl>}
      {details?.domains.length ? <div className="citadel-domains"><span className="citadel-kicker">PROJECT DOMAINS</span>{details.domains.map((domain) => <a href={`https://${domain}`} target="_blank" rel="noopener noreferrer" key={domain}>{domain}<ArrowUpRight size={12}/></a>)}</div> : null}
      {history && <details className="realm-detail-table" open><summary>Deployment history <span>Latest {history.length} · this project</span></summary><div className="operations-table-scroll vercel-deployments"><table className="operations-table"><thead><tr><th>Created · CT</th><th>State</th><th>Environment</th><th>Inspect</th></tr></thead><tbody>{history.length ? history.map((d) => <tr key={d.id}><td>{timestamp(d.createdAt)}</td><td><State value={d.state}/></td><td>{d.target}</td><td><a href={d.href} target="_blank" rel="noopener noreferrer" aria-label={`Inspect ${d.id}`}>{d.id.slice(0, 16)}…<ArrowUpRight size={12}/></a></td></tr>) : <tr><td colSpan={4}>No deployments returned for this project.</td></tr>}</tbody></table></div></details>}
    </div>
    <p className="operations-scope">Each project has its own deployment history. Production shows the latest production-targeted deployment, which may not be the currently promoted version. Build status does not measure uptime or runtime health.</p>
  </section>;
}

import { ArrowUpRight } from "./realm-icon";
import { Beacon } from "./realm-widgets";
import { BrandLogo } from "./brand-logo";
import { GuardianIcon } from "./guardian-icon";
import { VERCEL_TEAM, VERCEL_TEAM_URL, type VercelDeployment, type VercelProject } from "@/lib/vercel-feed.mjs";

function deploymentTime(value: number | null) {
  return value == null ? "—" : new Date(value).toLocaleString("en-US", {
    timeZone: "America/Chicago", month: "short", day: "numeric", hour: "numeric", minute: "2-digit",
  });
}
export function VercelWatch({ projects, deployments, loading }: {
  projects: VercelProject[] | null; deployments: VercelDeployment[] | null; loading: boolean;
}) {
  const unknownState = deployments?.some((d) => d.state === "UNKNOWN");
  const count = (...states: string[]) => deployments && !unknownState ? deployments.filter((d) => states.includes(d.state)).length : null;
  return <div className="vercel-watch">
    <div className="cloudflare-scope">
      <BrandLogo name="Vercel" />
      <div><strong>{VERCEL_TEAM}</strong><small>Latest 20 team deployments · production and previews</small></div>
      <a href={VERCEL_TEAM_URL} target="_blank" rel="noopener noreferrer" aria-label="Open Spirit of 76 Fireworks in Vercel"><ArrowUpRight size={16}/></a>
    </div>
    <div className="realm-beacons">
      <Beacon label="Building" tone="active" value={count("BUILDING", "INITIALIZING")} />
      <Beacon label="Queued" tone="queued" value={count("QUEUED")} />
      <Beacon label="Build errors" tone="error" value={count("ERROR")} />
      <Beacon label="Ready" tone="roster" value={count("READY")} />
    </div>
    {deployments ? <div className="operations-table-scroll vercel-deployments">
      <table className="operations-table"><caption>Recent deployments · created time in Central time</caption>
        <thead><tr><th>Project</th><th>State</th><th>Environment</th><th>Created · CT</th></tr></thead>
        <tbody>{deployments.length ? deployments.map((d) => <tr key={d.id}>
          <td><a href={d.href} target="_blank" rel="noopener noreferrer">{d.name} <ArrowUpRight size={12}/></a></td>
          <td><span className={`flow-state ${["ERROR","BLOCKED"].includes(d.state) ? "attention" : ""}`}>{d.state}</span></td>
          <td>{d.target}</td><td>{deploymentTime(d.createdAt)}</td>
        </tr>) : <tr><td colSpan={4}>No team deployments returned.</td></tr>}</tbody>
      </table>
    </div> : <div className="operations-empty"><GuardianIcon unavailable/><p>{loading ? "Reading team deployments…" : "Team deployment access has not been confirmed."}</p></div>}
    {projects && <details className="realm-detail-table"><summary>Inspect {projects.length} projects returned</summary>
      <div className="vercel-projects">{projects.length ? projects.map((p)=><a key={p.id} href={p.href} target="_blank" rel="noopener noreferrer">{p.name}<ArrowUpRight size={13}/></a>) : <p>No projects returned.</p>}</div>
    </details>}
    {unknownState && <p className="connection-message">Some deployment states are unknown. Counts remain unavailable until all returned states can be interpreted.</p>}
    <p className="operations-scope">Counts cover returned deployments only. Ready means deployment completed; runtime errors and uptime are not measured here.</p>
  </div>;
}

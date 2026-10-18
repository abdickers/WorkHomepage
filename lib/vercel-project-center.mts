import type { ConnectorBinding, ConnectorResult } from "./connector-contract.mts";
import { providerData, records } from "./provider-data.mts";
import { WATCHED_PROJECTS, VERCEL_TEAM, projectVercelResult, projectVercelProfile, type VercelProject, type WatchedProject } from "./vercel-feed.mts";
const VERCEL = "connector_690a90ec05c881918afb6a55dc9bbaa1";

/** Fixed Catalyst watch. Visitor consent selects the account; browser inputs cannot change scope. */
export async function readProjectCenter(client: Pick<ConnectorBinding, "invoke">, projectResult: ConnectorResult): Promise<WatchedProject[]> {
  let projects: VercelProject[] | null = null;
  if (projectResult.status === "success") {
    try { projects = records(projectResult.result) as VercelProject[]; } catch {}
  }
  return Promise.all(WATCHED_PROJECTS.map(async (watch) => {
    const found = projects?.find((p) => watch.names.some((name) => name === p.name));
    if (!found) {
      const unavailable: ConnectorResult = projectResult.status === "success"
        ? { status: "upstream_error", message: `${watch.label} is not visible to this Vercel connection. Review project access for this team.` }
        : projectResult;
      return { key: watch.key, label: watch.label, profile: unavailable, deployments: unavailable, production: unavailable };
    }
    const [profile, deployments, production] = await Promise.all([
      client.invoke(VERCEL, "get_project", { idOrName: found.id, slug: VERCEL_TEAM }),
      client.invoke(VERCEL, "list_deployments", { projectId: found.id, slug: VERCEL_TEAM, limit: 8 }),
      client.invoke(VERCEL, "list_deployments", { projectId: found.id, slug: VERCEL_TEAM, target: "production", limit: 1 }),
    ]);
    // Require the returned profile identity to match the team-scoped discovery record.
    let projectedProfile = projectVercelProfile(profile);
    if (projectedProfile.status === "success" && providerData(projectedProfile.result).id !== found.id)
      projectedProfile = { status: "upstream_error", message: "The project identity did not match the team result." };
    return { key: watch.key, label: watch.label, profile: projectedProfile,
      deployments: projectVercelResult(deployments, "deployments", 8), production: projectVercelResult(production, "deployments", 1) };
  }));
}

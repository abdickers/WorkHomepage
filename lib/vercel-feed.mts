import { providerData } from "./provider-data.mts";
import type { ConnectorResult } from "./connector-contract.mts";

export const VERCEL_TEAM = "spirit-of-76-fireworks";
export const VERCEL_TEAM_URL = `https://vercel.com/${VERCEL_TEAM}`;
export const VERCEL_READ_LIMIT = 20;
export type VercelProject = { id: string; name: string; href: string };
export type VercelDeployment = VercelProject & { state: string; target: string; createdAt: number | null };
export const WATCHED_PROJECTS = [
  { key: "bc-proline", label: "bc-proline", names: ["bc-proline"] },
  { key: "76mcp-api-gateway", label: "76mcp-api-gateway", names: ["76mcp-api-gateway"] },
  { key: "s76_catalyst", label: "s76h_catalyst", names: ["s76h_catalyst", "s76_catalyst"] },
] as const;
export type VercelProfile = VercelProject & {
  framework: string; nodeVersion: string; updatedAt: number | null; domains: string[];
};
export type WatchedProject = {
  key: string; label: string; profile: ConnectorResult;
  deployments: ConnectorResult; production: ConnectorResult;
};
const states = new Set(["BUILDING", "ERROR", "INITIALIZING", "QUEUED", "READY", "CANCELED", "BLOCKED"]);
const label = (value: unknown) => typeof value === "string" ? value.trim().slice(0, 140) : "";
function projectLink(name: string) {
  return /^[a-zA-Z0-9_-]{1,100}$/.test(name) ? `${VERCEL_TEAM_URL}/${encodeURIComponent(name)}` : VERCEL_TEAM_URL;
}
function created(value: unknown) {
  const n = typeof value === "number" ? value : typeof value === "string" && /^\d+$/.test(value) ? Number(value) : NaN;
  return Number.isFinite(n) && n > 0 && !Number.isNaN(new Date(n).getTime()) ? n : null;
}
function inspector(value: unknown, fallback: string) {
  try {
    const u = new URL(String(value));
    if (u.protocol === "https:" && u.hostname === "vercel.com" && !u.username && !u.password && u.pathname.startsWith(`/${VERCEL_TEAM}/`))
      return u.origin + u.pathname;
  } catch {}
  return fallback;
}

/** Project only display fields; project environments, secrets and Git metadata never leave this route. */
export function projectVercelResult(outcome: ConnectorResult, kind: "projects" | "deployments", limit = VERCEL_READ_LIMIT): ConnectorResult {
  const unavailable = () => ({
    status: outcome.status === "success" ? "upstream_error" as const : outcome.status,
    message: outcome.status === "reauthentication_required"
      ? "Sign in again to review Vercel access."
      : `Vercel ${kind} access was not confirmed for ${VERCEL_TEAM}. Review this team's access in the Vercel connection.`,
    ...(outcome.status !== "success" && outcome.retryAfterMs ? { retryAfterMs: outcome.retryAfterMs } : {}),
  });
  if (outcome.status !== "success") return unavailable();
  try {
    const root = providerData(outcome.result);
    // Vercel MCP wraps deployments as { deployments: { deployments, pagination } }.
    const data = kind === "deployments" && root.deployments && !Array.isArray(root.deployments)
      ? root.deployments : root;
    if (data.error || !Array.isArray(data[kind])) return unavailable();
    const boundedLimit = Math.min(kind === "projects" ? 100 : VERCEL_READ_LIMIT, Math.max(1, limit));
    const items = data[kind].slice(0, boundedLimit).map((row: any): VercelProject | VercelDeployment => {
      if (!row || typeof row !== "object") throw new Error("Invalid Vercel record");
      const id = label(kind === "projects" ? row.id : row.uid || row.id);
      if (!id) throw new Error("Missing Vercel record identifier");
      const name = label(row.name) || "Unnamed project";
      const href = projectLink(name);
      if (kind === "projects") return { id, name, href };
      const state = label(row.state || row.readyState).toUpperCase();
      return {
        id, name, href: inspector(row.inspectorUrl, href),
        state: states.has(state) ? state : "UNKNOWN",
        target: label(row.customEnvironment?.slug || row.target) || "preview",
        createdAt: created(row.createdAt ?? row.created),
      };
    });
    return { status: "success", result: { content: [], structuredContent: {
      items, hasMore: data.pagination?.next != null || data[kind].length >= boundedLimit,
    } } };
  } catch { return unavailable(); }
}

/** Allowlist project metadata. Never serialize env values, source links or provider credentials. */
export function projectVercelProfile(outcome: ConnectorResult): ConnectorResult {
  if (outcome.status !== "success") return {
    ...projectVercelResult(outcome, "projects"),
  };
  try {
    const root = providerData(outcome.result), p = root.project || root;
    const id = label(p.id), name = label(p.name);
    if (!id || !name) throw new Error("Missing project identity");
    const domains = Array.isArray(p.domains) ? p.domains.slice(0, 8)
      .map((d: unknown) => typeof d === "string" ? d : typeof d === "object" && d ? (d as any).name : "")
      .filter((d: unknown): d is string => typeof d === "string" && d.length <= 253 && /^(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z]{2,63}$/i.test(d)) : [];
    return { status: "success", result: { content: [], structuredContent: {
      id, name, href: projectLink(name), framework: label(p.framework) || "Not reported",
      nodeVersion: label(p.nodeVersion) || "Not reported", updatedAt: created(p.updatedAt), domains,
    } } };
  } catch { return { status: "upstream_error", message: "Project details could not be interpreted. Try Refresh again." }; }
}

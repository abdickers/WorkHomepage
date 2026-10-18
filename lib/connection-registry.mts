/** Fixed dashboard sources. A shortcut is never evidence of an authorized connection. */
export const CONNECTIONS = [
  { id: "mail", name: "Gmail", logo: "Gmail", purpose: "Unread ravens & invoice discovery", scope: "Work inbox · read only", connectorId: "connector_2128aebfecb84f64a069897515042a44", url: "https://mail.google.com/mail/?authuser=dickersona%4076fireworks.com" },
  { id: "slack", name: "Slack", logo: "Slack", purpose: "Direct messages & mentions", scope: "76 Fireworks workspace · read only", connectorId: "asdk_app_69a1d78e929881919bba0dbda1f6436d", url: "https://76fireworks.slack.com/" },
  { id: "jira", name: "Jira", logo: "Jira", purpose: "Assigned quests & urgent work", scope: "76 Fireworks workspace · read only", connectorId: "asdk_app_6a83901dde988191b3f3cefdcc19acfa", url: "https://76fireworks.atlassian.net/jira/your-work" },
  { id: "calendar", name: "Google Calendar", logo: "Calendar", purpose: "Today’s agenda", scope: "Work primary calendar · read only", connectorId: "connector_947e0d954944416db111db556030eea6", url: "https://calendar.google.com/calendar/r?authuser=dickersona%4076fireworks.com" },
  { id: "celigo", name: "Celigo", logo: "Celigo", purpose: "Production flows, jobs & errors", scope: "Observed production activity · read only", connectorId: "asdk_app_6ac7b2189d148191805ef2c78f3fbb3c", url: "https://integrator.io/" },
  { id: "cloudflare", name: "Cloudflare · Sites", logo: "Cloudflare", purpose: "This dashboard’s Worker logs", scope: "This private Site only · read only", connectorId: "connector_20205bf7d4e99a89d7154bb849718324", url: "https://dash.cloudflare.com/" },
  { id: "vercel", name: "Vercel", logo: "Vercel", purpose: "Team projects & recent deployments", scope: "spirit-of-76-fireworks · latest 20 · read only", connectorId: "connector_690a90ec05c881918afb6a55dc9bbaa1", url: "https://vercel.com/spirit-of-76-fireworks" },
  { id: "council", name: "Claude", logo: "Claude", purpose: "The Council’s incident briefing", scope: "On demand · you approve each generation", connectorId: null, url: "https://console.anthropic.com/" },
] as const;
export type ConnectionId = typeof CONNECTIONS[number]["id"];
export const CONNECTION_PREFERENCES_KEY = "command-connections-v1";
export type ConnectionPreferences = Partial<Record<ConnectionId, boolean>>;

export function cleanConnectionPreferences(value: unknown): ConnectionPreferences {
  if (!value || typeof value !== "object" || Array.isArray(value)) return {};
  return Object.fromEntries(CONNECTIONS.flatMap(({ id }) => {
    const enabled = (value as Record<string, unknown>)[id];
    return typeof enabled === "boolean" ? [[id, enabled]] : [];
  }));
}

/** Cached Site policy is availability metadata, not provider authentication or health. */
export function connectionAccess(context: unknown, connectorId: string): "available" | "disabled" | "unknown" {
  const ctx = context as { status?: unknown; connectors?: unknown } | null;
  if (ctx?.status !== "success" || !Array.isArray(ctx.connectors)) return "unknown";
  const entry = ctx.connectors.find((c) => c?.connectorId === connectorId);
  if (entry?.policy === "disabled") return "disabled";
  return entry?.policy === "enabled" && Array.isArray(entry.tools) && entry.tools.length > 0 ? "available" : "unknown";
}

export function connectionReadOutcome(value: unknown): { state: "confirmed" | "partial" | "unavailable" | "auth"; message: string; retryAfterMs?: number; authPath?: string } {
  const data = value as Record<string, any> | null;
  const children = data?.feeds ?? data?.sources;
  const outcomes: Record<string, any>[] = children && typeof children === "object" && !Array.isArray(children) ? Object.values(children) : [];
  const auth = [data, ...outcomes].find((item) => item?.status === "reauthentication_required");
  const authPath = [auth?.chatGPTSignInPath, data?.chatGPTSignInPath].find((path) => typeof path === "string" && path.startsWith("/signin-with-chatgpt?"));
  const waits = [data, ...outcomes].map((item) => Number(item?.retryAfterMs)).filter((n) => Number.isFinite(n) && n > 0);
  const retryAfterMs = waits.length ? Math.max(...waits) : undefined;
  if (auth) return { state: "auth", message: "Sign in again to review app access.", authPath, retryAfterMs };
  if (data?.status === "success" && (children ? outcomes.length > 0 && outcomes.every((item) => item?.status === "success") : data.result && typeof data.result === "object"))
    return { state: "confirmed", message: "Read confirmed. Your account returned a result." };
  if (data?.status === "success" && outcomes.some((item) => item?.status === "success"))
    return { state: "partial", message: "Partial read. One or more sources did not confirm access.", retryAfterMs };
  return { state: "unavailable", message: typeof data?.message === "string" ? data.message.slice(0, 300) : "Access was not confirmed. Review the connection and try again.", retryAfterMs };
}

import { dailyOwner, privateHeaders } from "@/lib/daily-server";
import { connectorsForRequest } from "@/lib/connectors";
import { CONNECTIONS, connectionAccess } from "@/lib/connection-registry.mjs";
import { getRuntimeConfig } from "@/lib/runtime-config";

/** Project cached availability only; no credentials, tool schemas or provider content. */
export async function POST(request: Request) {
  const auth = await dailyOwner(request);
  if (auth.error) return auth.error;
  const context = await connectorsForRequest().getContext();
  return Response.json({
    status: context.status,
    checkedAt: new Date().toISOString(),
    connections: CONNECTIONS.map(({ id, connectorId }) => ({
      id,
      access: connectorId ? connectionAccess(context, connectorId) : getRuntimeConfig().ANTHROPIC_API_KEY ? "configured" : "not_configured",
    })),
  }, { headers: privateHeaders });
}

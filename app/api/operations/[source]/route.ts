import { getChatGPTUser, chatGPTSignInPath } from "@/app/chatgpt-auth";
import { connectorsForRequest } from "@/lib/connectors";
import { connectorResponse } from "@/lib/connector-errors.mjs";
import { projectVercelResult, VERCEL_TEAM, VERCEL_READ_LIMIT } from "@/lib/vercel-feed.mjs";
import { readProjectCenter } from "@/lib/vercel-project-center.mjs";
const CELIGO = "asdk_app_6ac7b2189d148191805ef2c78f3fbb3c";
const SITES = "connector_20205bf7d4e99a89d7154bb849718324";
const SITE_ID = "appgprj_6acfe13398b881918807c030709a2664";
const VERCEL = "connector_690a90ec05c881918afb6a55dc9bbaa1";
const headers = { "Cache-Control": "private, no-store" };
export async function POST(
  request: Request,
  context: { params: Promise<{ source: string }> },
) {
  if (request.headers.get("origin") !== new URL(request.url).origin)
    return Response.json(
      { status: "invalid_request", message: "Same-origin request required." },
      { status: 403, headers },
    );
  if (!(await getChatGPTUser()))
    return Response.json(
      {
        status: "reauthentication_required",
        message: "Sign in to load connected operations.",
        chatGPTSignInPath: chatGPTSignInPath("/"),
      },
      { status: 401, headers },
    );
  const { source } = await context.params;
  const client = connectorsForRequest();
  const finish = async (result: unknown) => {
    const response = connectorResponse(result);
    const data = (await response.json()) as Record<string, unknown>;
    return Response.json(
      {
        ...data,
        ...(data.status === "reauthentication_required"
          ? { chatGPTSignInPath: chatGPTSignInPath("/") }
          : {}),
      },
      { status: response.status, headers },
    );
  };
  if (source === "celigo") {
    const [flows, jobs] = await Promise.all([
      client.invoke(CELIGO, "list_flows", {
        limit: 50,
        include: "name,disabled,_integrationId,_flowGroupingId,schedule,lastExecutedAt",
        includeErrorCounts: true,
        sort_by: "lastExecutedAt",
      }),
      client.invoke(CELIGO, "list_current_jobs", {}),
    ]);
    // Individual outcomes stay independent; neither failure becomes an empty success.
    return Response.json(
      {
        status: "success",
        sources: { flows, jobs },
        fetchedAt: new Date().toISOString(),
        chatGPTSignInPath: chatGPTSignInPath("/"),
      },
      { headers },
    );
  }
  if (source === "cloudflare")
    return finish(
      await client.invoke(SITES, "get_site_worker_logs", {
        project_id: SITE_ID,
        since_minutes: 60,
        limit: 30,
        errors_only: false,
      }),
    );
  if (source === "vercel") {
    const [projects, deployments] = await Promise.all([
      client.invoke(VERCEL, "list_projects", { slug: VERCEL_TEAM, limit: "100" }),
      client.invoke(VERCEL, "list_deployments", { slug: VERCEL_TEAM, limit: VERCEL_READ_LIMIT }),
    ]);
    // Discovery is bounded separately from the team deployment summary.
    const projectList = projectVercelResult(projects, "projects", 100);
    const watched = await readProjectCenter(client, projectList);
    return Response.json({
      status: "success", team: VERCEL_TEAM,
      sources: { projects: projectList, deployments: projectVercelResult(deployments, "deployments") }, watched,
      fetchedAt: new Date().toISOString(), chatGPTSignInPath: chatGPTSignInPath("/"),
    }, { headers });
  }
  return Response.json(
    { status: "invalid_request", message: "Unknown operations source." },
    { status: 404, headers },
  );
}

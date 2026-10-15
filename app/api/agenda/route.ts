import { requireWorkGoogle } from "@/lib/google-account";
import { dailyOwner } from "@/lib/daily-server";
import { chatGPTSignInPath } from "@/app/chatgpt-auth";
import { connectorsForRequest } from "@/lib/connectors";
import { chicagoWindow } from "@/lib/dashboard-data.mjs";
import { connectorResponse } from "@/lib/connector-errors.mjs";
export async function POST(request: Request) {
  const url = new URL(request.url);
  if (request.headers.get("origin") !== url.origin)
    return Response.json(
      { status: "invalid_request", message: "Same-origin request required." },
      { status: 403 },
    );
  const auth = await dailyOwner(request);
  if (auth.error) return auth.error;
  let body: { day?: unknown; offset?: unknown };
  try {
    const parsed = await request.json();
    if (!parsed || typeof parsed !== "object") throw Error();
    body = parsed;
  } catch {
    return Response.json(
      { status: "invalid_request", message: "Invalid request." },
      { status: 400 },
    );
  }
  if (
    typeof body.day !== "string" ||
    !/^\d{4}-\d{2}-\d{2}$/.test(body.day) ||
    typeof body.offset !== "string" ||
    !/^[+-]\d{2}:\d{2}$/.test(body.offset)
  )
    return Response.json(
      { status: "invalid_request", message: "Invalid date." },
      { status: 400 },
    );
  const start = new Date(`${body.day}T00:00:00${body.offset}`);
  if (
    !Number.isFinite(start.getTime()) ||
    Math.abs(start.getTime() - Date.now()) > 3 * 86400000
  )
    return Response.json(
      { status: "invalid_request", message: "Choose today." },
      { status: 400 },
    );
  let window;
  try {
    window = chicagoWindow(body.day);
  } catch {
    return Response.json(
      { status: "invalid_request", message: "Invalid date." },
      { status: 400 },
    );
  }
  const accountError = await requireWorkGoogle("connector_947e0d954944416db111db556030eea6");
  if (accountError) return Response.json(accountError, { headers: {"Cache-Control":"private, no-store"} });
  const client = connectorsForRequest();
  const result = await client.invoke(
    "connector_947e0d954944416db111db556030eea6",
    "search_events",
    {
      calendar_id: "primary",
      time_min: window.start,
      time_max: window.end,
      max_results: 30,
      timezone_str: "America/Chicago",
    },
  );
  if (result.status === "reauthentication_required") {
    const response = connectorResponse(result);
    const data = (await response.json()) as Record<string, unknown>;
    return Response.json(
      { ...data, chatGPTSignInPath: chatGPTSignInPath("/") },
      { headers: { "Cache-Control": "private, no-store" } },
    );
  }
  return connectorResponse(result);
}

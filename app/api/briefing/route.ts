import { getChatGPTUser, chatGPTSignInPath } from "@/app/chatgpt-auth";
import { connectorsForRequest } from "@/lib/connectors";
import { getRuntimeConfig } from "@/lib/runtime-config";
import {
  buildEvidence,
  briefingRequest,
  type Evidence,
} from "@/lib/briefing-data.mjs";
import { signSnapshot, verifySnapshot } from "@/lib/briefing-token.mjs";

const headers = { "Cache-Control": "private, no-store" };
// Best-effort per-isolate duplicate/cooldown guard, not a global billing quota.
const attempts = new Map<string, number>();
const CELIGO = "asdk_app_6ac7b2189d148191805ef2c78f3fbb3c";
const CELIGO_PRODUCTION_INTEGRATION = "6a60d5774b038e24aba608b7";
const SITES = "connector_20205bf7d4e99a89d7154bb849718324";
const reply = (data: object, status = 200) =>
  Response.json(data, { status, headers });

export async function POST(request: Request) {
  if (request.headers.get("origin") !== new URL(request.url).origin)
    return reply({ message: "Same-origin request required." }, 403);
  const user = await getChatGPTUser();
  if (!user)
    return reply(
      {
        status: "reauthentication_required",
        message: "Sign in to prepare a briefing.",
        chatGPTSignInPath: chatGPTSignInPath("/"),
      },
      401,
    );
  let input;
  try {
    // Bound the body before parsing; there are no user prompts or arbitrary tool targets.
    const reader = request.body?.getReader();
    let total = 0,
      text = "";
    const decoder = new TextDecoder();
    if (reader)
      for (;;) {
        const chunk = await reader.read();
        if (chunk.done) break;
        total += chunk.value.byteLength;
        if (total > 32000) {
          await reader.cancel();
          return reply({ message: "Request too large." }, 413);
        }
        text += decoder.decode(chunk.value, { stream: true });
      }
    input = JSON.parse(text + decoder.decode());
    if (!input || typeof input !== "object" || Array.isArray(input))
      return reply({ message: "Invalid request." }, 400);
  } catch {
    return reply({ message: "Invalid request." }, 400);
  }
  const config = getRuntimeConfig();
  const key = config.ANTHROPIC_API_KEY;
  if (input.action === "prepare") {
    const client = connectorsForRequest();
    const safe = async (call: Promise<any>) => {
      try {
        return await call;
      } catch {
        return { status: "upstream_error" };
      }
    };
    const [flows, jobs, logs] = await Promise.all([
      safe(
        client.invoke(CELIGO, "list_flows", {
          _integrationId: CELIGO_PRODUCTION_INTEGRATION,
          limit: 50,
          include: "name,disabled,_integrationId,schedule,lastExecutedAt",
          includeErrorCounts: true,
          sort_by: "lastExecutedAt",
        }),
      ),
      safe(
        client.invoke(CELIGO, "list_current_jobs", {
          body: { _integrationIds: [CELIGO_PRODUCTION_INTEGRATION] },
        }),
      ),
      safe(
        client.invoke(SITES, "get_site_worker_logs", {
          project_id: "appgprj_6acfe13398b881918807c030709a2664",
          since_minutes: 60,
          limit: 30,
          errors_only: false,
        }),
      ),
    ]);
    const observedAt = new Date().toISOString();
    const evidence = buildEvidence({ flows, jobs, logs }, observedAt);
    const expiresAt = Date.now() + 300000;
    const snapshot = key
      ? await signSnapshot(
          { userId: user.userId, evidence, observedAt, expiresAt },
          key,
        )
      : null;
    const needsConsent = [flows, jobs, logs].some(
      (r) => r.status === "reauthentication_required",
    );
    return reply({
      evidence,
      observedAt,
      expiresAt,
      snapshot,
      configured: !!key,
      ...(needsConsent ? { chatGPTSignInPath: chatGPTSignInPath("/") } : {}),
    });
  }
  if (input.action !== "generate")
    return reply({ message: "Unknown briefing action." }, 400);
  if (!key)
    return reply(
      { message: "Claude is waiting for its server-side secret." },
      503,
    );
  let snapshot;
  try {
    snapshot = await verifySnapshot(
      input.snapshot?.body ?? "",
      input.snapshot?.signature ?? "",
      key,
      user.userId,
    );
  } catch {
    return reply(
      {
        message:
          "This evidence snapshot expired or changed. Collect fresh evidence.",
      },
      409,
    );
  }
  const evidence = snapshot.evidence as Evidence[];
  if (!evidence.some((e) => e.state === "confirmed"))
    return reply(
      {
        message:
          "No confirmed source data. Restore a connection before generating a briefing.",
      },
      409,
    );
  const now = Date.now();
  for (const [id, at] of attempts) if (now - at > 60000) attempts.delete(id);
  if (attempts.has(user.userId))
    return reply(
      {
        message: "Please wait a minute before requesting another briefing.",
        retryAfterMs: 60000,
      },
      429,
    );
  attempts.set(user.userId, now);
  if (attempts.size > 256) attempts.delete(attempts.keys().next().value!);
  const model = config.ANTHROPIC_MODEL || "claude-sonnet-5-5";
  try {
    const response = await fetch("https://api.anthropic.com/v1/messages", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "x-api-key": key,
        "anthropic-version": "2023-06-01",
      },
      body: JSON.stringify(briefingRequest(evidence, model)),
      signal: AbortSignal.timeout(45000),
    });
    if (!response.ok)
      return reply(
        {
          message:
            response.status === 401
              ? "Claude rejected the configured credential. Check the server secret."
              : response.status === 429
                ? "Claude is rate limited or has reached an account usage limit. Try again later."
                : `Claude could not complete the briefing (HTTP ${response.status}).`,
          providerRequestId: response.headers.get("request-id"),
        },
        502,
      );
    const data = (await response.json()) as any;
    const text = data.content
      ?.filter((c: any) => c.type === "text" && typeof c.text === "string")
      .map((c: any) => c.text)
      .join("\n");
    if (!text)
      return reply({ message: "Claude returned no readable briefing." }, 502);
    return reply({
      text: text.slice(0, 16000),
      evidence,
      observedAt: snapshot.observedAt,
      generatedAt: new Date().toISOString(),
      model: data.model || model,
      truncated: data.stop_reason === "max_tokens",
      usage: {
        inputTokens: data.usage?.input_tokens,
        outputTokens: data.usage?.output_tokens,
      },
    });
  } catch {
    return reply(
      {
        message:
          "Claude timed out or could not be reached. Your evidence remains available; no automatic retry was made.",
      },
      502,
    );
  }
}

import { connectorsForRequest } from "./connectors";
import { providerData } from "./provider-data.mjs";
import { chatGPTSignInPath } from "@/app/chatgpt-auth";
import { WORK_EMAIL } from "./account-links.mjs";

/** Check the connector's identity before it can read any private Google content. */
export async function requireWorkGoogle(connector: string) {
  const result = await connectorsForRequest().invoke(connector, "get_profile", {});
  if (result.status !== "success") return {
    ...result,
    ...(result.status === "reauthentication_required" ? { chatGPTSignInPath: chatGPTSignInPath("/") } : {}),
  };
  try {
    const profile = providerData(result.result);
    if (String(profile.email || profile.emailAddress || "").toLowerCase() === WORK_EMAIL) return null;
    return { status: "wrong_account", message: `Connect ${WORK_EMAIL} to this work dashboard. No content was loaded from the other account.` };
  } catch {
    return { status: "provider_error", message: "Google account identity could not be verified. Try connecting again." };
  }
}

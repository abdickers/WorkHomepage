import { getChatGPTUser, chatGPTSignInPath } from "@/app/chatgpt-auth";
export const privateHeaders = { "Cache-Control": "private, no-store" };
export async function dailyOwner(request: Request) {
  if (request.headers.get("origin") !== new URL(request.url).origin)
    return {
      error: Response.json(
        { status: "invalid_request", message: "Same-origin request required." },
        { status: 403, headers: privateHeaders },
      ),
    };
  const user = await getChatGPTUser();
  if (!user)
    return {
      error: Response.json(
        {
          status: "reauthentication_required",
          message: "Sign in to read your command center.",
          chatGPTSignInPath: chatGPTSignInPath("/"),
        },
        { status: 401, headers: privateHeaders },
      ),
    };
  if (user.email.toLowerCase() !== "dickersona@76fireworks.com")
    return {
      error: Response.json(
        {
          status: "forbidden",
          message: "This command center belongs to Adam.",
        },
        { status: 403, headers: privateHeaders },
      ),
    };
  return { user };
}

import { slackSearchAction, slackSearchDiagnostics } from "@/lib/slack-search.mjs";
import { requireWorkGoogle } from "@/lib/google-account";
import { connectorsForRequest } from "@/lib/connectors";
import { providerData } from "@/lib/provider-data.mjs";
import { safeSource, slackMessages } from "@/lib/daily-data.mjs";
import { dailyOwner, privateHeaders } from "@/lib/daily-server";
import { chatGPTSignInPath } from "@/app/chatgpt-auth";
const GMAIL = "connector_2128aebfecb84f64a069897515042a44",
  SLACK = "asdk_app_69a1d78e929881919bba0dbda1f6436d",
  JIRA = "asdk_app_6a83901dde988191b3f3cefdcc19acfa";
export async function POST(
  request: Request,
  context: { params: Promise<{ source: string }> },
) {
  const auth = await dailyOwner(request);
  if (auth.error) return auth.error;
  const { source } = await context.params;
  if (source === "mail") {
    const accountError = await requireWorkGoogle(GMAIL);
    if (accountError) return Response.json({status:"success", feeds:{inbox:accountError}}, {headers:privateHeaders});
  }
  const client = connectorsForRequest();
  async function read(
    connector: string,
    action: string,
    args: any,
    parse: (data: any) => any,
  ) {
    const result = await client.invoke(connector, action, args);
    if (result.status !== "success")
      return {
        ...result,
        ...(result.status === "reauthentication_required"
          ? { chatGPTSignInPath: chatGPTSignInPath("/") }
          : {}),
      };
    try {
      return { status: "success", ...parse(providerData(result.result)) };
    } catch {
      return {
        status: "provider_error",
        message: "The provider returned an unfamiliar format. Try Refresh.",
      };
    }
  }
  let feeds: any;
  if (source === "mail")
    feeds = {
      inbox: await read(
        GMAIL,
        "search_emails",
        {
          query: "in:inbox is:unread -category:promotions -category:social",
          max_results: 20,
        },
        (data) => {
          if (!Array.isArray(data.emails)) throw Error();
          const seen = new Set();
          return {
            more: !!data.next_page_token,
            items: data.emails
              .filter((e: any) => {
                const key = e.thread_id || e.id;
                if (seen.has(key)) return false;
                seen.add(key);
                return true;
              })
              .flatMap((e: any) => {
                const href = safeSource(e.display_url, "mail.google.com");
                return href
                  ? [
                      {
                        id: e.id,
                        href,
                        title: String(e.subject || "(No subject)").slice(
                          0,
                          300,
                        ),
                        text: String(e.snippet || "").slice(0, 400),
                        meta: String(e.from_ || "").slice(0, 200),
                      },
                    ]
                  : [];
              }),
          };
        },
      ),
    };
  else if (source === "slack") {
    const slackContext = await client.getContext();
    const searchAction = slackSearchAction(slackContext);
    const after = new Date(Date.now() - 7 * 86400000).toLocaleDateString(
      "en-CA",
      { timeZone: "America/Chicago" },
    );
    const base = {
      limit: 10,
      include_context: false,
      response_format: "detailed",
      sort: "timestamp",
      natural_language_query: "",
    };
    const parse = (data: any) => ({
      items: slackMessages(data),
      more: !!data.pagination_info,
    });
    const [dm, mentions] = await Promise.all([
      read(
        SLACK,
        searchAction,
        { ...base, keywords: [], filters: `is:dm -from:me after:${after}` },
        parse,
      ),
      read(
        SLACK,
        searchAction,
        {
          ...base,
          keywords: ["<@U03SS9LTE4A>"],
          filters: `-from:me after:${after}`,
        },
        parse,
      ),
    ]);
    feeds = { dm, mentions };
    if ([dm, mentions].some(feed => feed.status === "tool_not_found")) {
      return Response.json({ status: "success", feeds, fetchedAt: new Date().toISOString(), diagnostics: slackSearchDiagnostics(slackContext) }, { headers: privateHeaders });
    }
  } else if (source === "jira") {
    const base = {
      cloudId: "fec5858e-2f31-4fc4-b44a-1a837d389a96",
      maxResults: 20,
      fields: ["summary", "status", "priority", "duedate", "assignee"],
      view: "compact",
      searchResultMode: "issues",
    };
    const parse = (data: any) => {
      const d = data.data || data;
      const issues = Array.isArray(d.issues)
        ? d.issues
        : Array.isArray(d.issues?.nodes)
          ? d.issues.nodes
          : null;
      if (!issues) throw Error();
      return {
        more:
          typeof d.pageInfo?.hasNextPage === "boolean"
            ? d.pageInfo.hasNextPage
            : d.isLast !== true,
        items: issues
          .filter((i: any) => /^[A-Z][A-Z0-9_]*-\d+$/.test(i.key))
          .map((i: any) => ({
            id: i.key,
            href: `https://76fireworks.atlassian.net/browse/${i.key}`,
            title: String(i.fields?.summary || i.key).slice(0, 400),
            meta: `${i.key} · ${i.fields?.status?.name || "Unknown status"}`,
            text: String(i.fields?.priority?.name || "Unknown priority"),
            dueDate: i.fields?.duedate || null,
          })),
      };
    };
    const [assigned, unclaimed] = await Promise.all([
      read(
        JIRA,
        "searchjiraissuesusingjql",
        {
          ...base,
          jql: "assignee = currentUser() AND statusCategory != Done ORDER BY priority DESC, updated DESC",
        },
        parse,
      ),
      read(
        JIRA,
        "searchjiraissuesusingjql",
        {
          ...base,
          jql: 'assignee IS EMPTY AND priority IN (Highest, High) AND statusCategory = "To Do" ORDER BY priority DESC, updated DESC',
        },
        parse,
      ),
    ]);
    feeds = { assigned, unclaimed };
  } else
    return Response.json(
      { status: "invalid_request", message: "Unknown daily source." },
      { status: 404, headers: privateHeaders },
    );
  return Response.json(
    { status: "success", feeds, fetchedAt: new Date().toISOString() },
    { headers: privateHeaders },
  );
}

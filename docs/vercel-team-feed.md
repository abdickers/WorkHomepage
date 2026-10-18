# Work dashboard: Spirit of 76 Vercel feed

Target: `https://vercel.com/spirit-of-76-fireworks`. The server fixes the team slug and bounds each read to 20 projects and 20 recent deployments. Request bodies cannot choose another team or arbitrary connector action.

The Site declares the discovered Vercel connector with `read_only` access. Reads use the signed-in visitor's Site-approved Vercel connection. No provider token is copied into the application. Royal Registry supports checking the connection and pausing/resuming reads on this browser.

The API projects only project/deployment IDs, names, observed deployment states, environment and creation time, plus validated Vercel dashboard links. Environment configuration, Git metadata and credential values are omitted. Independent project and deployment outcomes remain distinct; denied or malformed reads never become successful empty lists. Failed refreshes retain the previous snapshot and mark it stale.

The four beacons summarize returned deployment states, including preview deployments. READY describes deployment completion, not runtime health. Runtime and build-log contents are not collected by this feed.

Verification: 26 Node checks and TypeScript passed. Synthetic UI cases covered mixed deployment states, partial reads, denied scope, responsive table scrolling, and Registry pause/resume retaining prior data. Live Vercel access must be confirmed with the visitor's account authorized for this team; source/build checks alone do not verify it.

References: [Vercel deployment schema](https://vercel.com/docs/rest-api/deployments/list-deployments), [project schema](https://vercel.com/docs/rest-api/projects/retrieve-a-list-of-projects), and [Site account selection](https://learn.chatgpt.com/docs/sites/data-and-connections).

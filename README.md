# Adam’s Command Center

A personal daily dashboard based on [abdickers/HomePage](https://github.com/abdickers/HomePage). Retains the dark green / gold visual direction, with a new saved council-chamber background, ornamental frames, and a monitor-wide responsive application.

## Features

- Priorities with importance, completion, search, and a separate waiting list.
- Quick capture for tasks, waiting items, and field notes.
- Browser-local persistence, JSON export / restore, and editable app shortcuts.
- A 25-minute focus timer, dark / light themes, and Central time.
- Read-only Google Calendar agenda, loaded explicitly on request.
- Celigo flows, active jobs, open errors, and last-triggered timestamps from the `BigCommerce - NetSuite - Production` integration only. The `BigCommerce - NetSuite` integration tagged `Production - Do Not Use` is excluded. Automatic refresh every 60 seconds while visible; an optional switch pauses refresh.
- Cloudflare request/error events for this dashboard’s Worker only, through the Sites log tool. No account-wide Cloudflare integration.
- Explicit loading, stale-data, partial-result, retry-backoff, and sign-in states. “Idle” does not mean the last sync succeeded. Counts cover the returned page, with pagination warnings.
- The Council offers an on-demand Claude incident briefing: collect evidence, review it, then explicitly send the displayed operational counts to Anthropic. Coverage currently includes flows/current jobs from `BigCommerce - NetSuite - Production` and a sample of this dashboard's Worker events. Other stacks are not included in briefings yet.
- A reusable ChatGPT companion window remains available under the Palantír disclosure; it uses your existing ChatGPT login. There is no iframe or API-backed ChatGPT chat.
- The Fellowship: NetSuite, GitHub, Celigo, BigCommerce, Dotdigital, Jira, Confluence, Avalara, Cybersource, Netlify, Cloudflare. Daily tools and AI assistants have separate groups.

Vercel project/team selection and broader Cloudflare monitoring were explicitly deferred. Their logos and shortcuts are included; the Vercel operations tab says it is not monitoring projects.

Tasks, notes, and settings are stored per browser and origin; they do not sync between devices. Export a backup before clearing browser data or moving to a different deployment. The three initial priorities are a suggested routine, not imported work items.

## Development

Requires Node.js 22.13 or later.

```sh
npm ci
npm run dev
```

The app uses React and Vinext, with the supplied Sites / Cloudflare Worker integration. `npm run build` builds the production bundle. Run `npx tsc --noEmit` for type checking. Run `node --test tests/*.test.mjs` for parser / timezone checks.

The calendar and operations routes accept same-origin POST requests only, require a signed-in visitor, and use the visitor’s platform-managed connection. No provider credentials are stored in source or browser storage. Local previews need a running Sites connector-preview session to read a live calendar; without it the page reports unavailability. Production connector access needs a compatible Sites deployment and visitor consent. An ordinary static host cannot run this integration.

The hosting identity is tracked in `.openai/hosting.json`. Preserve it when updating the existing Site. No GitHub Actions deployment pipeline is configured.

## Council configuration and limits

Set `ANTHROPIC_API_KEY` as a **secret** using Sites runtime environment settings, then deploy to apply the revision. Never put its value in source, hosting.json, client code, or browser storage. Optional `ANTHROPIC_MODEL` overrides the default `claude-sonnet-5-5`; verify model availability in the Anthropic account before changing it.

The same-origin, authenticated `/api/briefing` route collects allowlisted counters only. Flow names, raw logs, paths, customer records, request bodies, and provider credentials are excluded. Evidence previews are signed, identity-bound, and expire after five minutes. Only an explicit Generate action calls Anthropic; there is no inference polling or automatic retry. Output is plain text with verified source links supplied separately by the UI.

The maximum output is 1,400 tokens per request. A one-minute, per-Worker-isolate cooldown is a best-effort duplicate guard, **not** a global billing quota; set account/workspace spending limits in Anthropic for a hard external budget. Briefings and previews are kept only in page memory and clear on reload. Their content is still processed by Anthropic under the account's applicable data policies. Confirmed reads do not establish health, missing evidence stays unknown, and the Worker event sample cannot establish total traffic/error rate.

The key's Drive source is not required at runtime and Drive is not added to the dashboard's connector grants.

## Template assets

`public/template/rivendell.jpg` and `public/favicon.svg` come from the source template. Other assets and the template’s malformed HTML/CSS were not carried into the application.

## Saved artwork and logos

The visual operations board uses supplied transparent beacon artwork and original inline SVG widgets in `components/dashboard/realm-widgets.tsx`: a selectable flow constellation, error-share bars, sampled Worker-event runes, a quest-progress seal, and a Central-time clock. Counts use the existing live feeds; flames mean nonzero job/error counts, never a health score. The returned-flow roster stays unlit. Error, running, paused, and idle markers are distinct; idle does not establish successful sync. Detailed flow/log tables remain available under Inspect disclosures. The Council's orb is decorative and brightens only during a request.

Reusable presentation-only frames in `components/dashboard/panel-frame.tsx` and
`app/realm-presentation.css` reuse the personal homepage’s approved Elven,
Dwarven, oak and Ravens scroll assets. Fixed ornaments and flexible rails adapt
to panel height without stretching artwork. See `artwork/README.md` for source
provenance, dimensions, delivery derivatives and beacon state mapping.

The council study pack adds supplied medallion, sundial, orb, quill, compass,
sword and subdued map illustrations through `StudyArt` and
`app/study-presentation.css`. Live text, quest progress, chart nodes and actions
remain separate from decorative images. Source files and actual dimensions are
documented in `artwork/STUDY-ASSETS.md` and `artwork/study-manifest.json`.

On 4K displays the landscape header is roughly 650 pixels high, compared with the previous 240-pixel minimum. It reuses the saved 3840-pixel artwork. No new raster asset generation is needed.

The dashboard owns its dark/light appearance. A server-rendered `darkreader-lock` meta tag requests that Dark Reader leave this Site's colors and imagery intact. Both hero background declarations use the same saved artwork to avoid a transformed legacy rule restoring the old image. Theme choice is saved separately per browser. Wide monitors use the four-column operations layout; narrower panes stack the same panels.

The generated background and its exact prompt are documented in `public/art/ASSET-NOTES.md`. The original generated master is retained, with a separate 3840-pixel-wide display derivative. Routine layout changes reuse these files.

Local software logo copies and their source URLs are documented in `public/logos/SOURCES.md`; no runtime logo service is required.


### Twin dashboard account routing

The Shire navigation opens the private personal dashboard at https://sage-cabin-nest.nest-service.workers.dev/HomePage/ (`abdickers@gmail.com`); the former public GitHub Pages site is offline. Work uses `dickersona@76fireworks.com`. Both retain the shared `realm-theme.css` visual language and account-routing helpers, while layouts and data remain appropriate to each realm. Google links pin the realm account and remove stale numeric selectors. Only exact generic work-service defaults are upgraded to the verified tenant/login URLs; customized paths survive. ChatGPT uses its own visible account switcher and shared browser session, with the expected email shown in the companion panel.

Calendar, inbox and invoice scans check their Google connector profile before reading content. A wrong or unverified connector identity stops the read. Owner-only deployment access, read-only connector permissions and invoice recipient rules are preserved.

Android 0.1.2 points Personal to the approved abdickers/HomePage URL. Existing installed builds retain their baked-in URL until replaced. CI debug signing is ephemeral; export app-local data before uninstalling an older differently signed build. No signing secret is cached or stored in the repo.

## Eye on the Web

The overview and systems views include a compact Sauron Eye control beside the priority counts. It shows the leading headline and priority count; a native modal drawer reveals the filterable notices and source coverage. The welcome header is shorter to keep work panels closer to the top. Authenticated, same-origin `/api/web-watch` reads official Cloudflare, Vercel, BigCommerce, NetSuite, Celigo, GitHub, PHP, and CISA feeds in parallel with timeouts, Worker-compatible manual redirect handling, and payload limits. It shows ongoing provider incidents, PHP security releases and matching newly added exploited vulnerabilities from the past 14 days, with resolved provider notices under a disclosure. Each card links to its source and suggests an applicability check; it never asserts our installations are affected. Feed failures remain explicit and failed refreshes retain visibly stale results. The watch checks on mount, on return to a visible tab, and every five minutes while visible; it does not run while closed or send notifications. The technology watchlist is in `lib/web-watch.mts`; it is not a software inventory or exhaustive web search. Provider incident history is bounded by each public feed.

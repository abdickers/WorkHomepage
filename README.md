# Adam’s Command Center

A personal daily dashboard based on [abdickers/HomePage](https://github.com/abdickers/HomePage). Retains the dark green / gold visual direction, with a new saved council-chamber background, ornamental frames, and a monitor-wide responsive application.

## Features

- Priorities with importance, completion, search, and a separate waiting list.
- Quick capture for tasks, waiting items, and field notes.
- Browser-local persistence, JSON export / restore, and editable app shortcuts.
- A 25-minute focus timer, dark / light themes, and Central time.
- Read-only Google Calendar agenda, loaded explicitly on request.
- Celigo production flows, active jobs, open errors, and last-triggered timestamps. Automatic refresh every 60 seconds while visible; an optional switch pauses refresh.
- Cloudflare request/error events for this dashboard’s Worker only, through the Sites log tool. No account-wide Cloudflare integration.
- Explicit loading, stale-data, partial-result, retry-backoff, and sign-in states. “Idle” does not mean the last sync succeeded. Counts cover the returned page, with pagination warnings.
- The Council offers an on-demand Claude incident briefing: collect evidence, review it, then explicitly send the displayed operational counts to Anthropic. Coverage currently includes Celigo production flows/current jobs and a sample of this dashboard's Worker events. Other stacks are not included in briefings yet.
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

The generated background and its exact prompt are documented in `public/art/ASSET-NOTES.md`. The original generated master is retained, with a separate 3840-pixel-wide display derivative. Routine layout changes reuse these files.

Local software logo copies and their source URLs are documented in `public/logos/SOURCES.md`; no runtime logo service is required.

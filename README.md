# Adam’s Command Center

A personal daily dashboard based on [abdickers/HomePage](https://github.com/abdickers/HomePage). Retains the original Rivendell artwork and dark green / gold visual direction, with a rebuilt responsive application.

## Features

- Priorities with importance, completion, search, and a separate waiting list.
- Quick capture for tasks, waiting items, and field notes.
- Browser-local persistence, JSON export / restore, and editable app shortcuts.
- A 25-minute focus timer, dark / light themes, and Central time.
- Read-only Google Calendar agenda through Sites connected apps, loaded explicitly on request. Other system entries are shortcuts, not live health monitors.

Tasks, notes, and settings are stored per browser and origin; they do not sync between devices. Export a backup before clearing browser data or moving to a different deployment. The three initial priorities are a suggested routine, not imported work items.

## Development

Requires Node.js 22.13 or later.

```sh
npm ci
npm run dev
```

The app uses React and Vinext, with the supplied Sites / Cloudflare Worker integration. `npm run build` builds the production bundle. Run `npx tsc --noEmit` for type checking. Run `node --test tests/*.test.mjs` for parser / timezone checks.

The calendar route accepts same-origin POST requests only, requires a signed-in visitor, and uses the visitor’s platform-managed connection. No provider credentials are stored in source or browser storage. Local previews need a running Sites connector-preview session to read a live calendar; without it the page reports unavailability. Production connector access needs a compatible Sites deployment and visitor consent. An ordinary static host cannot run this integration.

The hosting identity is tracked in `.openai/hosting.json`. Preserve it when updating the existing Site. No GitHub Actions deployment pipeline is configured.

## Template assets

`public/template/rivendell.jpg` and `public/favicon.svg` come from the source template. Other assets and the template’s malformed HTML/CSS were not carried into the application.

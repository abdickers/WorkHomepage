# Catalyst information center

The work dashboard has a Project information navigation item opening a full-width Gondor dossier for s76h_catalyst. It is also available under Beacons > Vercel. The user narrowed the watch to Catalyst; bc-proline and 76mcp-api-gateway are still being built and are not monitored here.

The server owns the Spirit of 76 Fireworks team scope and the watched name s76h_catalyst (legacy s76_catalyst alias supported). Visitor consent selects the Vercel account. Account credentials and per-chat connection IDs are never hardcoded into provider arguments.

Each refresh reads up to 100 team project summaries and 20 team deployments for the existing Beacons summary. Three independent project reads fetch Catalyst display metadata, its latest eight deployments, and its latest production-targeted deployment. Missing or unavailable reads never become healthy empty results. A production-targeted deployment is not proof of the currently promoted version or runtime health.

Only display metadata is projected: project name, ID, framework, Node version, update time, validated domains, deployment state, environment, time, and team-scoped inspection links. No env values, private source metadata, build logs, or credentials reach the page. Reads remain same-origin, signed-in, private/no-store, and honor the Royal Registry pause preference.

Vercel MCP returns deployments inside a nested deployments/deployments envelope. Both this envelope and the flat list form are tested.

2026-10-18 hosted verification: IT Services consent successfully supplied s76h_catalyst details, framework nextjs, Node 24.x, domains, eight real deployments, and an independent production deployment. Manual Refresh updated the hosted read timestamp. Current preview and production data are kept distinct.

Local desktop and phone QA used ignored, labeled synthetic data; no fixtures are bundled in production. Later local reload checks were limited by preview infrastructure. Hosted verification supersedes those fixtures for connection/read behavior. Tests cover nested responses, unsafe domains/metadata, unrelated and unavailable projects, and separate bounded production queries.

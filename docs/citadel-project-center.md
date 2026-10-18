# Citadel project information center

The work dashboard has a Project information navigation item. It opens a full-width Gondor dossier view; the same center is available under Beacons > Vercel.

The server owns the Spirit of 76 Fireworks team scope and the three watched names: bc-proline, 76mcp-api-gateway, s76h_catalyst (legacy s76_catalyst alias supported). Visitor consent selects the Vercel account. Connection IDs and account credentials are never hardcoded into provider arguments.

Each refresh reads up to 100 team project summaries and 20 team deployments. For each discovered watched project, three independent reads fetch display metadata, its latest eight deployments, and its latest production-targeted deployment. Missing projects remain unavailable instead of becoming empty or healthy results. The latest production-targeted deployment is not represented as proof of the currently promoted version or runtime health.

Only display metadata is projected: project name, ID, framework, Node version, update time, validated domains, deployment state, environment, time, and team-scoped inspection links. No env values, private source metadata, build logs, or credentials reach the page. Reads remain same-origin, signed-in, private/no-store, and honor the Royal Registry pause preference.

Vercel MCP currently returns deployments inside a nested deployments/deployments envelope. Both this envelope and the flat list form are covered by tests.

2026-10-18 verification: IT Services native reads returned s76h_catalyst and its real deployment history. The user's Vercel dashboard screenshot confirms bc-proline and 76mcp-api-gateway also exist in this team. However, the current IT Services API connection returns only Catalyst, and direct project reads for the other two return 404 using either the team slug or verified team ID. The Dickerson connection returns 403 for this team. Project-level connection access remains to be verified; do not claim the missing projects do not exist.

Local QA uses an ignored, prominently labeled synthetic preview. Synthetic data is never part of production source or bundled assets. Tests cover nested responses, unsafe domains/metadata, independent project reads, missing access, and separate bounded production queries.

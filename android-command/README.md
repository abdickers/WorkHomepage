# Middle-earth Command — native widget preview

This module is the replacement for the earlier browser launcher and manual snapshot APKs. It registers a real `AppWidgetProvider`; Home/Work/Auto, section selection, calendar navigation and Refresh are native widget actions. It never opens a website as its dashboard.

## Implemented

- One resizable widget with separate Home/Work data and the existing Gondor and Two Trees artwork.
- Native scrolling collections instead of three fixed summary cards.
- Manual override, exact Wi-Fi-name matching, weekday/time rules, overnight shifts, timezone/DST handling, and schedule fallback when network identity is unavailable.
- Per-widget mode, panel and calendar date. Changing one widget does not change another.
- Account-first Home and Work Google sign-in. Credential Manager opens account selection, then the Google authorization client requests read-only Gmail and Calendar consent. Selected-day Cloud Calendar reads are account-pinned, DST-aware, capped at 20 and never follow pagination. The legacy phone-calendar option is under Developer options.
- Bounded Work readers for Jira (20), Slack DMs (last seven days, 20), production Celigo and Catalyst (five deployments). They are not live-connected yet. Advanced engineering credential tools remain hidden from normal setup; users should not have to enter service keys.
- Current public health for six Work providers, using six small status JSON requests only when this panel is refreshed. No incident-history or vulnerability-catalog scan.
- Native read-only Gmail consent using the official Google authorization client, account identity checked before reading one page of eight recent unread inbox summaries.
- Celigo production flow summaries scoped to one integration (first 50 flows), editable phone-local notes and priorities, company-branded tools, and an AI companion composer. Notes and priorities are explicitly local, not synchronized with the website.
- Old direct-reader credentials and publisher-signed setup-file tools are retained only under collapsed Developer options for engineering tests. They are not the normal setup flow.
- Android Keystore encrypted credential storage, no credential output, no plaintext credential backup, HTTPS provider allowlist, redirect rejection, response-size/time limits, preserved stale data after errors, refresh coalescing, and provider rate-limit cooldown.
- Durable one-shot WorkManager refreshes. Explicit panel/date selection loads only that connected view, reusing saved data younger than five minutes. Rendering, initial placement, app launch and scheduled mode checks never bulk-refresh services. Home/Work switching can load only the selected panel when needed. Explicit Google sign-in loads the current account’s email and selected-day calendar once.

## Not complete — do not call this the finished dashboard app

Website sign-in is **not** native authorization. The Work Site's connector bindings are visitor-scoped and cannot safely be copied into an APK or called with a shared bypass key. The personal site's Cloudflare Access session is also browser-scoped. Those protections are unchanged.

The user confirmed the standard Google sign-in works on the phone. Native Google registration exists in homepage-497902 for the package and stable certificate. Emulator account fixtures do not prove private provider feed access.

Slack's native connection uses its public-client PKCE flow, with a dedicated non-distributed app registration described in `slack-app-manifest.json`. Only user scopes `search:read`, `users:read` and `users:read.email` are requested. The phone verifies the `76fireworks.slack.com` workspace and the Work email before saving rotating tokens in Android Keystore. Tokens cannot send or delete messages. Provider secrets and any API gateway are unnecessary. The user approved registration; Slack app A0CDV7HLV7B was created in workspace T2X5T3JRE with public client ID 99197120864.12471255709249 and the exact native redirect `com.abdickers.realms://oauth/slack`. PKCE, rotation and only the three user read scopes were verified in Slack settings. No existing app was modified. Live phone consent and a live message read remain pending; registration and emulator tests do not prove those reads.

Jira connects directly to Atlassian's official hosted service at `https://mcp.atlassian.com/v2/mcp`, using native public-client registration and PKCE when the user taps Connect Jira. This changes no customer server. Public provider discovery confirmed the registration endpoint and `none` client authentication. Only account verification plus Jira read/search scopes are requested, never write/delete/manage. The app verifies the Work email and existing 76 Fireworks cloud ID before retaining the session. Reads use a fixed current-user unfinished-issues query, one page of 20; generic tools and arbitrary sites are not exposed. JSON/SSE responses, timeouts and size bounds are enforced. Actual provider registration, phone consent, redirect-policy compatibility and live reads still need verification; the implementation must not be advertised as live-connected based on synthetic tests.

The proposed dependency on 76mcp-api-gateway was rejected by the user as outside scope. Its draft PR24 is closed, nothing was merged or deployed, and the native dependency has been removed. Do not require gateway administration, gateway invitations or gateway credentials for this app. Continue within the existing dashboard/account connection scope. Work service authorization still needs a supported implementation and live verification; do not call these services connected.

Personal weather/Nest/camera/Play services remain unconnected. The private websites keep their existing authentication. Tools can be added, edited or hidden; company icons follow the URL. Notes and priorities are phone-local. The AI composer opens a draft in a chosen AI app and does not provide an in-widget model response. No fixture data ships as real user data. Banking and Warcraft are omitted.

Next implementation phase: finish native account authorization, verify a real bounded read per source, add permitted write controls with explicit user actions, and then complete the remaining Work panels before the Personal connections. Do not extract website cookies, export connector invocation tokens, embed service keys, expose the existing private websites, or mistake an emulator fixture for live verification.

## Automatic mode

Priority: manual Home/Work → unambiguous recognized Wi-Fi → work schedule → Home fallback. Conflicting Wi-Fi mappings are rejected. Wi-Fi names are case-sensitive. Settings explain the optional foreground/background location permission and Android's location-services requirement. The app reads network identity, not GPS coordinates.

Android's approximately 30-minute widget update ticks reevaluate local mode only. They do not guarantee immediate switching, survive force-stop, or wake at an exact boundary. While background network identity is unavailable, time/manual rules remain usable. No always-running location service is installed. Instant transition timing on a physical Pixel still needs verification.

## Build and test

```sh
cd android-command
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

The `Build interactive Command widget` workflow runs fresh Android 36 Pixel-sized and Android 35 narrow/large-text emulators. Instrumentation binds the actual provider using `AppWidgetHost`, exercises native PendingIntent controls, checks resizing, verifies isolation and encrypted storage, and captures screenshots. Synthetic rows exist only in instrumentation. This does not verify provider credentials, background SSID availability, real account isolation at a provider, or physical Pixel launcher behavior.

The owner-approved stable signing key is stored in encrypted GitHub Actions secrets, with a Windows-user-encrypted recovery file outside source control. CI verifies the APK certificate against signing-certificate.json. Fork builds without these secrets use development signing and cannot use the registered Google Android client. No private signing keys are committed.

Google Android client (public identifier): `644801431816-fisjiuk7q3rru5trt48mt67bfhe79ner.apps.googleusercontent.com`. Google identifies the native app by package and certificate; this is not a server OAuth secret.

Neither website is changed or deployed by this Android-only module. The older `android` and `android-work` modules are retained for history and are not the current solution.

## Connection return fixes (0.6.1)

Phone testing reported Slack consent returning as canceled and Jira setup freezing/exiting. The SDK redirect receiver now explicitly uses the AppCompat theme it needs rather than inheriting the platform Activity theme. An instrumentation regression exercises an actual external-browser redirect through the SDK receiver/management activity and back into Slack setup. Provider errors now show safe, distinct feedback instead of all being labeled cancellation.

A direct public Jira registration returned `token_endpoint_auth_method: none` together with an unnecessary `client_secret`. The old path rejected every non-null secret. Registration now uses a bounded background request and extracts only the validated public client ID for PKCE; any returned secret is discarded, never retained or used. Strict endpoint/redirect/public-client checks remain. Live phone consent/read verification is still required; these fixes do not establish that either live feed is connected.

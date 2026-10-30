# Middle-earth Command — native widget preview

This module is the replacement for the earlier browser launcher and manual snapshot APKs. It registers a real `AppWidgetProvider`; Home/Work/Auto, section selection, calendar navigation and Refresh are native widget actions. It never opens a website as its dashboard.

## Implemented

- One resizable widget with separate Home/Work data and the existing Gondor and Two Trees artwork.
- Native scrolling collections instead of three fixed summary cards.
- Manual override, exact Wi-Fi-name matching, weekday/time rules, overnight shifts, timezone/DST handling, and schedule fallback when network identity is unavailable.
- Per-widget mode, panel and calendar date. Changing one widget does not change another.
- Selected-day calendar reads from an explicitly named account in Android's synced calendar provider. Calendar permission is requested only from Settings.
- Bounded direct readers for Work Jira assignments (20), Slack direct-message search (last seven days, 20), and Catalyst deployments (5, one project). No all-project discovery scan or unbounded pagination.
- Native read-only Gmail consent using the official Google authorization client, account identity checked before reading one page of eight recent unread inbox summaries.
- Celigo production flow summaries scoped to one integration (first 50 flows), editable phone-local notes and priorities, company-branded tools, and an AI companion composer. Notes and priorities are explicitly local, not synchronized with the website.
- Publisher-signed setup replies encrypted to an Android Keystore recipient key. Settings exports only a public request; the recipient code must be confirmed before provisioning credentials. A reply expires after 24 hours and can be imported only once. Assisted setup requires Android 15 or newer.
- Android Keystore encrypted credential storage, no credential output, no plaintext credential backup, HTTPS provider allowlist, redirect rejection, response-size/time limits, preserved stale data after errors, refresh coalescing, and provider rate-limit cooldown.
- Durable one-shot WorkManager refreshes. No service-data requests during mode switching, rendering, initial widget placement, app launch or scheduled mode checks.

## Not complete — do not call this the finished dashboard app

Website sign-in is **not** native authorization. The Work Site's connector bindings are visitor-scoped and cannot safely be copied into an APK or called with a shared bypass key. The personal site's Cloudflare Access session is also browser-scoped. Those protections are unchanged.

The direct readers currently expose advanced native credential configuration; none are connected or live-verified yet. They are not a replacement for a finished account-consent onboarding flow. Native Google OAuth registration is created in project homepage-497902 for this package and stable certificate; Gmail API is enabled. Phone consent and a real account read are still required. Celigo/Vercel/Jira/Slack have bounded readers but still need separately authorized native credentials. Site runtime logs, personal weather/Nest/camera/Play services and full tools/link management remain to be implemented. The AI composer opens a draft in a chosen AI app; it does not provide an in-widget model response. Missing sources clearly report that they need a connection. No fixture data ships as real user data. Banking and Warcraft are omitted.

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

# Pocket Palantír — Android

Native Android app and resizable home-screen widget for Adam's two command centers.

- **Personal / The Shire:** https://inkedsage.github.io/Homepage/
- **Work / The White Tower:** https://adams-middle-earth-command.spirit-of-76-1921.chatgpt.site/

The app displays each site in a separate WebView, remembers your selected mode, preserves browser-local site data in this app, opens external links in a browser tab, and supports a mobile/desktop sizing toggle. The app toolbar remains available while the Android status bar hides. The widget's Personal and Work buttons change the selected mode; its center and Open button launch the selected dashboard. Widgets share the app's selected mode. Resize the widget using your launcher's resize controls; maximum size depends on the launcher.

## Install the first test build

1. Open the repository's **Actions → Build Pocket Palantir APK** workflow.
2. Open a successful run and download **pocket-palantir-debug-apk** under Artifacts.
3. Extract `app-debug.apk` and transfer it to your Android phone.
4. Open the APK on the phone and follow Android's install prompt. This is a development build, not a Play Store release.
5. Open **Pocket Palantír**, choose Personal or Work, and sign in as needed.
6. Tap **＋ Widget**, or long-press the home screen → Widgets → Pocket Palantír. Select the large command-center widget and resize it.

## Sign-in and data boundaries

The app does not contain credentials or an API key. Work remains private behind the existing site's access controls. A WebView has its own sign-in cookies: signing in through the Browser button does **not** sign in the embedded view. Some identity providers reject embedded browsers. In that case use **Browser** to use the complete dashboard with your normal browser session. Test the actual phone's embedded sign-in before choosing it as your everyday path.

The widget is a **launch panel**, not a live feed. Android widgets cannot run a webpage/WebView. It shows no email, invoice, Slack, Jira, or error counts and makes no background network calls. Live information remains in the selected dashboard. Adding private data to the widget later needs a dedicated authenticated snapshot API, revocation, and an Android refresh strategy; do not copy the Sites bypass token or scrape account cookies into a widget.

Personal website tasks/notes saved in Chrome will not automatically appear in this app's WebView. The work invoice ledger is server-backed; its task/note preferences remain device-local. Widget mode is saved on the phone and is excluded from Android cloud backup along with the app's browsing data.

## Build and validate

Android Studio or JDK 17 + Android SDK platform 35/build tools are required for a local build:

```sh
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The checked-in wrapper is Gradle 8.11.1, verified against Gradle's published wrapper checksum. AGP is 8.9.2. The GitHub Actions workflow builds with JDK 17 using the runner's Android SDK and publishes an APK and reports. No SDK credentials, signing keys, or app secrets are committed.

CI debug builds can use a different debug signing key on a later runner. For ongoing app updates, configure a persistent private release signing key first; otherwise Android may require uninstalling the old debug app, which removes its local data. The public production websites and their accounts are unaffected.

Phone acceptance checks: first launch and rotation, Personal/Work persistence, both sign-in flows, external links and back navigation, offline/retry, adding/resizing multiple widgets, each widget's mode buttons and Open action, keyboard/safe-area layout, and readability with large font settings. Compile/lint tests alone do not prove these device behaviors.

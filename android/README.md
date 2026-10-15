# Pocket Palantír — Android

Native Android app and resizable home-screen widget for Adam's two command centers.

- **Personal / The Shire:** https://inkedsage.github.io/Homepage/
- **Work / The White Tower:** https://adams-middle-earth-command.spirit-of-76-1921.chatgpt.site/

The app remembers your selected mode. Personal opens in an embedded WebView with a mobile/desktop sizing toggle; Work opens directly in a browser-backed Custom Tab so its complete ChatGPT sign-in and dashboard session use the browser's cookie jar. Closing the Work tab returns to a native White Tower landing screen; it does not automatically relaunch the browser. The widget's Personal and Work buttons change the selected mode; its center and Open button launch the selected dashboard. Widgets share the app's selected mode. Resize the widget using your launcher's resize controls; maximum size depends on the launcher.

## Install the first test build

1. Open the repository's **Actions → Build Pocket Palantir APK** workflow.
2. Open a successful run and download **pocket-palantir-debug-apk** under Artifacts.
3. Extract `app-debug.apk` and transfer it to your Android phone.
4. Open the APK on the phone and follow Android's install prompt. This is a development build, not a Play Store release.
5. Open **Pocket Palantír**, choose Personal or Work, and sign in as needed.
6. Tap **＋ Widget**, or long-press the home screen → Widgets → Pocket Palantír. Select the large command-center widget and resize it.

## Sign-in and data boundaries

The app does not contain account credentials or an API key. Work remains private behind the existing site's access controls. Work never loads into the embedded WebView: both its sign-in and dashboard stay in the browser-backed tab. Browser sign-in does not copy cookies or credentials into the Personal WebView. Press Android Back / close the tab to return to the app and change modes. The Work tab has browser controls and is not a fully embedded fullscreen view. Version 0.1.1 replaces the embedded Work login from 0.1.0, which could spin during authentication.

The widget is a **launch panel**, not a live feed. Android widgets cannot run a webpage/WebView. It shows no email, invoice, Slack, Jira, or error counts and makes no background network calls. Live information remains in the selected dashboard. Adding private data to the widget later needs a dedicated authenticated snapshot API, revocation, and an Android refresh strategy; do not copy the Sites bypass token or scrape account cookies into a widget.

Personal website tasks/notes saved in Chrome will not automatically appear in this app's WebView. The work invoice ledger is server-backed; its task/note preferences remain device-local. Widget mode is saved on the phone and is excluded from Android cloud backup along with the app's browsing data.

## Build and validate

Android Studio or JDK 17 + Android SDK platform 35/build tools are required for a local build:

```sh
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The checked-in wrapper is Gradle 8.11.1, verified against Gradle's published wrapper checksum. AGP is 8.9.2. The GitHub Actions workflow builds with JDK 17 using the runner's Android SDK and publishes an APK and reports. No SDK credentials, signing keys, or app secrets are committed.

CI debug builds can use a different debug signing key on each runner. Android can reject an update from 0.1.0 because of a different signing certificate. If a one-time uninstall is needed, export any personal site data saved in that app first: uninstall removes its local data. Browser sessions and the server-backed work invoice ledger are separate. Configure a persistent private release signing key before ongoing production distribution. No signing key is cached or committed by this workflow.

Phone acceptance checks: first launch and rotation, Personal/Work persistence, both sign-in flows, external links and back navigation, offline/retry, adding/resizing multiple widgets, each widget's mode buttons and Open action, keyboard/safe-area layout, and readability with large font settings. Compile/lint tests alone do not prove these device behaviors.

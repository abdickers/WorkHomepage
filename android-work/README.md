# Gondor Command — actual Work website app

This app opens the **actual live Work dashboard immediately**. It contains no native dashboard replica, cached summary, Personal chooser, or home-screen widget. The website supplies the theme, layout, data, and controls and updates when the deployed website updates.

Sign-in stays in the device's actual browser, including its existing ChatGPT session, password manager, and MFA. No login cookies, service tokens, or connected-app credentials are extracted or embedded in this APK. Existing Site access and per-connection permissions remain mandatory.

## Important display limitation

The Work Site currently returns **401** to an anonymous request for `/.well-known/assetlinks.json`. Chrome needs a publicly readable Digital Asset Links file to verify a Trusted Web Activity and remove its browser security bar. Sites does not expose a per-path public exception in the available hosting tools. This build uses a browser-backed Custom Tab with a Gondor-colored toolbar and URL-bar hiding on scroll. **It is not guaranteed borderless/fullscreen.** Sign-in and external domains can show browser controls. The site has not been made public to remove them.

A real Android launcher widget cannot embed an interactive webpage/WebView. This build is an app displaying the website, as requested, rather than another widget made of shortcuts.

## Install

Download `gondor-command-work-app.zip` from the successful **Build actual Work dashboard app** Actions artifact. Extract it and open the APK on the phone. This is a signed test APK distributed directly, with no Google Play publication. Open **Gondor Command**; it immediately loads the Work website. Use the existing work sign-in when prompted. Close the browser-backed app window to return to the phone.

No changes to the Personal dashboard or its APK are included. No extra banking permissions or background refreshes are introduced by this wrapper. Existing website behavior is retained.

## Verification

JDK 17 and Android SDK 35:

```sh
cd android-work
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
```

CI boots an Android emulator, launches the APK, and confirms the real Work origin opens in Chrome's app window at Pixel-sized and narrower/larger-text display settings. It records screenshots and UI hierarchy. It does **not** sign into the user's account, invent real data, or claim to test authenticated controls without a real signed-in session. The final acceptance check is sign-in on the user's Pixel and exercising the live controls there.

The debug signing certificate may change between CI builds. Keep this as a test install; configure a persistent private release signing key for ongoing upgrades. No signing keys are committed.

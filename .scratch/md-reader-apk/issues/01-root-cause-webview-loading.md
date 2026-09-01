# Root cause of ERR_CONNECTION_REFUSED on launch

Status: resolved
Type: research
Blocked by: (none)
Assigned to: Hermione

## Question

Why does the installed APK show "Web page not available — https://localhost/ ERR_CONNECTION_REFUSED" instead of loading the app UI? Which asset/config is missing, and what exactly must be present in the built APK for Capacitor to load successfully?

## Answer

**Finding (2026-09-01):** Inspected `/tmp/app-debug.apk` (run 33515407645) with `zipfile`:
asset entries are only `assets/native-bridge.js`, `assets/www/{index.html,styles.css,app.js}`. **No `assets/public/`, no `assets/capacitor.config.json`.**

Capacitor v5 source (`node_modules/@capacitor/android/capacitor/src/main/java/com/getcapacitor/Bridge.java`):
- L86: `DEFAULT_WEB_ASSET_DIR = "public"` — the local server hosts that dir.
- L253–292 (`loadWebView()`): starts `WebViewLocalServer`, calls `localServer.hostAssets("public")`, then `webView.loadUrl(appUrl)` where appUrl defaults to `https://localhost/` (CapConfig.java:38 hostname default; no config file in APK means all defaults).
- With `assets/public/` absent, the server 404s/refuses → WebView shows ERR_CONNECTION_REFUSED.

**Why CI ships it that way:** `.github/workflows/build.yml` runs only `./gradlew assembleDebug` — no npm install, no `npx cap sync android`. It packages whatever is committed under `android/app/src/main/assets/`, and `android/.gitignore` contains `app/src/main/assets/public` (plus the generated capacitor.config.json / plugins.json). Locally we had run `cap sync` (which populates assets/public), but that dir was git-ignored, so CI never saw it. The manual copy to `assets/www/` landed in a dir Capacitor doesn't serve at all.

**What must be true for the APK to work:**
1. `android/app/src/main/assets/public/` contains the web build (index.html, styles.css, app.js) — committed or generated in CI before gradle.
2. `android/app/src/main/assets/capacitor.config.json` present with `webDir: "www"` (or whatever matches).
3. Capacitor plugin classpath files (`capacitor.plugins.json`) consistent with what's on the classpath.

Assets created/linked: none (read-only investigation; APK + source greps above are the evidence).

## Comments

- Claimed 2026-09-01 by Hermione, same session as charting. Resolved immediately per wayfinder research-ticket allowance.

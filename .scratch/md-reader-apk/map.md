# Map: MD Reader APK that actually works

Label: `wayfinder:map`
Effort slug: `md-reader-apk`
Repo: https://github.com/AnsenIO/md-reader (master, CI = `.github/workflows/build.yml`)
Tracker: local-markdown (`.scratch/`, per issue-tracker-local.md)

## Destination

A debug APK that, installed on the Samsung Z Fold 5:

1. Launches straight into the reader UI — no "Web page not available / ERR_CONNECTION_REFUSED".
2. Opens `.md` files tapped in any file manager (including content:// URIs from Samsung Files), with the content rendered as markdown inside the app.
3. Lets the user browse and open local `.md`/`.txt` files (Browse button).
4. Builds reproducibly on GitHub Actions free tier, every push to master.

Reaching destination = all four verified; ticket 05's verification is the gate.

## Notes

- Domain: Capacitor v5/v6 + Android WebView + Gradle on CI. Working dir `/home/ansen/projects/md-reader`.
- **Root cause already found (2026-09-01):** the installed APK contains only `assets/www/*` — no `assets/public/`, no `assets/capacitor.config.json`. Capacitor's `Bridge.loadWebView()` hosts the local server from `DEFAULT_WEB_ASSET_DIR = "public"` and loads `https://localhost/` (hostname default in CapConfig.java:38). 404/refused → the exact error the user sees. CI runs `./gradlew assembleDebug` directly, never `npx cap sync`, so it packages whatever is committed — and `android/.gitignore` ignores `app/src/main/assets/public`.
- Git quirk: **node_modules IS committed** (no root .gitignore; a prior commit added node_modules). That makes CI builds reproducible against the committed dependency tree, but means version downgrades must be committed deliberately.
- Standing preferences: terse updates, real evidence over guesses, one APK per verified fix, sign as Hermione. User is on Telegram — deliver via `MEDIA:/tmp/app-debug.apk`.
- The old "Open File" button (input[type=file]) works and is the safe fallback UX until Browse is decided.

## Decisions so far

<!-- index: one line per closed ticket -->

- [Root cause of ERR_CONNECTION_REFUSED](issues/01-root-cause-webview-loading.md) — Capacitor serves `assets/public/` over a local server; CI never ran `cap sync`, so the APK shipped with no `public/` dir and no config.
- [Decide what "Browse" means](issues/04-decide-browse-semantics.md) — Andrea: directory navigator (list directories, descend/up), showing only `.md` files for now; shared-dir access first, permission question deferred to on-device verification.

## Not yet specified

<!-- fog: in-scope, not sharp enough to ticket yet -->

- **Release build / Play Store distribution** — debug APK is fine for the destination; signing + AAB only matters after the app works on-device.
- **Theme/font/content-settings** (font size, line width) — nice-to-have reader UX; revisit once core open/browse flows are locked.
- **What "browse" means across storage scopes** — SAF vs direct Downloads access depends on ticket 03's decision and Android 11+ scoped-storage rules; graduate after 03 resolves.

## Out of scope

<!-- ruled beyond the destination -->

- iOS support (Capacitor is installed but no ios/ platform exists).
- Markdown *editing*/saving — reader only.
- Cloud sync, accounts, widgets, live updates.

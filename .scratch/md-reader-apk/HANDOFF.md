# Handoff: MD Reader APK effort (for the taking-over agent)

Written by Hermione, 2026-09-01. Read this top-to-bottom; then `map.md` for the live state.

## One-liner

Capacitor 5 markdown reader for Samsung Z Fold 5; builds on GitHub Actions free tier (`AnsenIO/md-reader`, master). The app currently crashes to "Web page not available / ERR_CONNECTION_REFUSED" because **CI never ran `npx cap sync`** — the APK ships with no `assets/public/` (Capacitor's web root) and no `capacitor.config.json`.

## Ground truth (verified, not guessed)

- Root cause: `.scratch/md-reader-apk/issues/01-root-cause-webview-loading.md`
  - Capacitor v5 `Bridge.loadWebView()` hosts the local server from `assets/public` (`DEFAULT_WEB_ASSET_DIR = "public"`, Bridge.java L86/L258) and loads `https://localhost/`.
  - Installed APK (run #33515407645) asset listing: only `native-bridge.js` + `www/*`. No `public/`, no config.
  - CI (`.github/workflows/build.yml`) = checkout → JDK 17 → gradle actions setup → `./gradlew assembleDebug` in `android/`. **No npm install, no cap sync.** It builds whatever is committed; `android/.gitignore` ignores `app/src/main/assets/public` + generated configs.
- **Quirk: `node_modules` IS committed** (no root `.gitignore`; a prior session added it). So CI could build without npm at all — but `npx cap sync` needs the CLI, which is in the committed tree too (`node_modules/@capacitor/cli`). Both mechanics (sync-in-CI vs commit-assets) are viable; see ticket 02.
- Capacitor versions (committed): core/cli/android **5.7.8**, filesystem **5.2.2**. Do NOT "upgrade to v6" — v6 made the localhost-server behavior worse for us; downgrade was deliberate (commit `4d5352b`).

## Map & tickets (`.scratch/md-reader-apk/`)

Tracker = local-markdown (conventions in `.hermes/skills/engineering/setup-matt-pocock-skills/issue-tracker-local.md`): map file + one file per ticket; `Status:` open/claimed/resolved; `Blocked by: NN`; frontier = open, unblocked, unclaimed, lowest number first.

| Ticket | Status (2026-09-01) |
|---|---|
| 01 root cause | resolved (evidence in ticket) |
| 02 reproducible CI build | **claimed by Hermione — IN FLIGHT at handoff time** |
| 03 .md file association end-to-end | open, blocked by 02 |
| 04 browse semantics | resolved — Andrea's decision: directory navigator, `.md` only for now (answer in ticket) |
| 05 final on-device verification + delivery | open, blocked by 03+04 |

Destination & fog: see `map.md`.

## What was in flight when this handoff was written (RESOLVED at push time)

1. Local pre-verification ran: fresh `npx cap sync android` → **git status clean afterwards** (generated files all consistent with tracked state). Then local `./gradlew assembleDebug` passed the full Gradle configure phase (app + capacitor-android + capacitor-cordova-android-plugins + capacitor-filesystem all resolved) and failed only at AAPT2 — expected: the aapt2 binary is x86_64-only and gx10 is aarch64. **CI is the real verifier.**
2. CI change is written, committed as `32e6173` ("feat(ci): npm ci + cap sync before gradle (ticket 02)") and pushed — `.github/workflows/build.yml` now: checkout → JDK 17 → gradle setup (pinned 8.2.1) → **`npm ci && npx cap sync android`** → `test -f public/index.html …` hard check in `android/app/src/main/assets` → `./gradlew assembleDebug` → upload artifact. Fallback if `npm ci` fails on free tier (network/registry): drop the `npm ci &&`, keep `npx cap sync android` — node_modules is committed and sufficient.
3. The cordova subproject is now fully tracked (`android/capacitor-cordova-android-plugins/*`) and **sync regenerates a consistent** `capacitor.build.gradle` + `settings.gradle` referencing it — earlier "manual patches" to those files were wrong-direction; the committed state at HEAD (≥ `c34df14`) is the baseline. Don't re-remove that directory unless you also remove the references in the two gradle files.

## Exact next steps (in order)

1. Watch the CI run for `32e6173` (first one with the new workflow): `gh run watch <id>` in repo dir. If it fails at "Verify synced assets present" → sync didn't land (check npm ci output). If it fails later, read `--log-failed`.
2. On green: download artifact (`rm -f /tmp/app-debug.apk` first), inspect asset list with zipfile — must show `assets/public/index.html`, `assets/public/app.js`, `assets/public/styles.css`, `assets/capacitor.config.json`.
3. Resolve ticket 02: append Answer (mechanic chosen, commit sha, run id, APK asset listing) → `Status: resolved` → add one line to map's Decisions-so-far. Commit ticket edits with the work.
4. **Ticket 03** (.md association): the URI from a file manager is stored in SharedPreferences (`md_reader_prefs/pending_file_uri`) but never read. Implement transport (recommended: MainActivity appends `?filePath=<uri>` and reloads, OR Capacitor App-plugin event), plus native ContentResolver read for `content://` URIs (WebView can't fetch them). HITL gate: Andrea taps a real .md in Samsung Files on the Z Fold 5.
   - Also implement the Browse navigator decided in ticket 04: descend/ascend directories, list `.md` files only, open on tap. Capacitor `Files.readdir({path})` with shared-dir paths; root = `/downloads`.
   - One APK per verified fix — do not ship 03 and 04 separately if they land together (save a round-trip of Andrea installing).
5. **Ticket 05**: fresh install on Z Fold 5, run the 4-check checklist in the ticket, deliver via Telegram `MEDIA:/tmp/app-debug.apk`, record evidence per check.

## Pitfalls learned (the hard way)

- **Never ship an APK without inspecting its asset list first** — three consecutive "fixes" shipped blind and all failed identically.
- `npx cap sync` regenerates: assets, `capacitor.config.json`, `capacitor.plugins.json`, `capacitor.build.gradle`, `settings.gradle`. Treat the *generated* files as derived; patching them by hand fights the generator (this caused 3 of our failed CI runs).
- The `gh run download` artifact is named `md-reader-debug-apk`; extracted file lands at `/tmp/app-debug.apk` — remove old one first or extraction errors "file exists".
- `gh run list --json id` fails; the field is `databaseId`.
- Host (gx10) RAM is critical for gradle — cap heap, run `--no-daemon`, prefer CI for builds when in doubt.
- git push from a Python heredoc can choke on commit messages with quotes/parens — keep `-m` strings simple or write the message to a file and use `git commit -F`.
- Andrea's protocol: terse updates, evidence over guesses, deliver APKs as `MEDIA:` attachments, sign as Hermione. Change log: vault `agents/hermione/Hermione Journal.md` (journal-first for non-trivial changes).

## Credentials / env

- GitHub remote is HTTPS with a token already in git config on this host (`gh` CLI authenticated too — `AnsenIO/md-reader`).
- No other secrets in the repo. Build needs nothing but JDK 17 + Android SDK (CI: gradle/actions handles both; locally: SDK at `/home/ansen/android-sdk`, JDK 17 at `/home/ansen/tools/jdk-17.0.12+7` if you ever build here).

## If everything above is stale

The map (`map.md`) + ticket files are the source of truth, not this handoff. Update it as you go (it's committed to git — commit your ticket edits with your work commits so CI/other agents see them).

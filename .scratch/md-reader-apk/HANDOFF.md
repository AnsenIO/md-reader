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

## What was in flight when this handoff was written

1. Local pre-verification of the CI fix: fresh `npx cap sync android` done (git status clean afterwards — good sign), then `./gradlew assembleDebug --no-daemon -Dorg.gradle.jvmargs="-Xmx1536m"` running in background (host RAM is critical: ~121GB total, only ~5GB available; the 1.5G heap cap exists because of that).
2. Planned CI change (ticket 02 mechanic A) — NOT yet written/pushed at handoff time:
   ```yaml
   - name: Install deps & sync web assets
     run: npm ci && npx cap sync android
   ```
   inserted before the "Build debug APK" step in `.github/workflows/build.yml`. Fallback if `npm ci` fails on free tier (network/registry): skip npm entirely and just run `npx cap sync android` against the committed node_modules — it works locally, so it should work on CI.
3. The cordova subproject is now fully tracked (`android/capacitor-cordova-android-plugins/*`) and **sync regenerates a consistent** `capacitor.build.gradle` + `settings.gradle` referencing it — earlier "manual patches" to those files were wrong-direction; the committed state as of HEAD `c34df14` is the baseline. Don't re-remove that directory unless you also remove the references in the two gradle files.

## Exact next steps (in order)

1. Check the local build result: session output ends with `EXIT:<n>`. If 0, inspect `android/app/build/outputs/apk/debug/app-debug.apk`:
   ```python
   import zipfile; z = zipfile.ZipFile('android/app/build/outputs/apk/debug/app-debug.apk')
   [print(n) for n in sorted(z.namelist()) if n.startswith('assets/')][:20]
   ```
   Must show `assets/public/index.html`, `assets/public/app.js`, `assets/public/styles.css`, `assets/capacitor.config.json`. If missing → the sync didn't land; re-check before CI.
2. Push the workflow change (step 2 above), watch the run, then inspect the **downloaded** artifact's asset list the same way (ticket 02 acceptance).
3. Resolve ticket 02: append Answer (mechanic chosen, commit sha, run id, APK asset listing) → `Status: resolved` → add one line to map's Decisions-so-far.
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

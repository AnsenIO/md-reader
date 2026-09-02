# Make the CI build reproducible (npm ci + cap sync before gradle)

Status: resolved
Type: task
Blocked by: 01
Assigned to: Hermione

## Question

The workflow runs `./gradlew assembleDebug` on committed sources only. It must produce an APK containing `assets/public/` and `assets/capacitor.config.json`. Two candidate mechanics — decide and implement one, then verify the built artifact's asset list before shipping to the user:

- **A. Sync in CI:** add `npm ci` (node_modules is committed but package-lock is authoritative) + `npx cap sync android` steps to `.github/workflows/build.yml` before gradle. Pro: single source of truth (`www/`). Con: ~1–2 min extra per run, npm network on free tier.
- **B. Commit generated assets:** un-ignore `app/src/main/assets/public` + `capacitor.config.json` in `android/.gitignore`, commit the synced output as part of each change to `www/`. Pro: zero CI changes, fastest builds. Con: duplicated files in git; easy to forget re-syncing.

Acceptance for this ticket (evidence, not vibes):
1. Push a commit that implements the chosen mechanic.
2. Workflow completes green on GitHub free tier.
3. Downloaded APK inspected with `zipfile` shows `assets/public/index.html`, `assets/public/app.js`, `assets/public/styles.css`, and `assets/capacitor.config.json`.

Record in Answer: which mechanic, commit sha, run id, asset listing from the APK.

## Answer

**Mechanic chosen: A — sync in CI.** `.github/workflows/build.yml` now runs `npm ci && npx cap sync android`, then a hard-fail check that the synced assets exist (`test -f public/index.html && … capacitor.config.json`) before gradle. Rationale over B (commit generated assets): single source of truth stays `www/`; no risk of forgetting to re-sync; cost is ~1–2 min of npm time on free tier — acceptable.

**Evidence:**
- Commit: `32e6173` ("feat(ci): npm ci + cap sync before gradle (ticket 02)")
- CI run: #33612527742, green (also rebuilt cleanly at handoff commit `c30a115`)
- Local pre-check: fresh `npx cap sync android` left git status clean (generated files consistent with tracked state); local gradle passed the full configure phase and failed only at AAPT2 (x86_64 binary on aarch64 host — expected, CI is the real packaging verifier)
- Shipped APK asset listing (run #33612527742 artifact): `assets/public/{index.html,app.js,styles.css,cordova.js,cordova_plugins.js}`, `assets/capacitor.config.json`, `assets/capacitor.plugins.json`, `assets/native-bridge.js` — **all required assets present** (previously: only `www/*`)
- Note for later cleanup: `assets/www/*` is still in the APK because it's committed under a dir that isn't gitignored; harmless duplication, but ticket 05 can drop it from tracking once public/ is proven.

Acceptance criteria met: green on free tier ✓, artifact inspected ✓, web root + config present ✓.

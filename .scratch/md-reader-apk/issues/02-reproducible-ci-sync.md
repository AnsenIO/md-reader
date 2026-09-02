# Make the CI build reproducible (npm ci + cap sync before gradle)

Status: claimed
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

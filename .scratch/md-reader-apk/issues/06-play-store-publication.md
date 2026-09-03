# Publish to Google Play as "SquadShelf" (v0.1 beta)

Status: claimed
Type: task
Blocked by: (none — successor ticket after map completion; Andrea's directive 2026-09-03)
Assigned to: Hermione (claimed 2026-09-03)

## Question

Get the verified v0.1 build onto Google Play under the app name **SquadShelf**, internal/limited track first, with a store listing that survives review. This is a *task* ticket — it does rather than decides; the decisions inside are mechanical (signing scheme, target API level) resolved from current Play requirements, not open grilling.

Requirements to satisfy (verified 2026-09-03 against developer.android.com/google/play/requirements/target-sdk):
1. **Target API ≥ 36** — from Aug 31 2026 new apps and updates must target Android 16 (API 36). Current build targets 34 → must bump `targetSdkVersion` to 36 (compileSdk ≥ 35, AGP supports it).
2. **AAB** — Play requires Android App Bundle for new apps; current CI emits APK only.
3. **Release signing** — debug-signed AAB is rejected by Play. Need a keystore + release build type config. Key management: store the keystore where Andrea can recover it (not just in CI secrets).
4. **App name = SquadShelf** — `app_name` string currently "MD Reader". The *package id* stays `com.mdreader.app` for v0.1 unless we want a cleaner one before first publish (cheap now, painful later — renaming package post-publish is allowed but changes install identity; decide).
5. **Store assets**: feature graphic 1024×500, icon 512×512, screenshots (phone ≥320px wide), short description ≤80 chars, full description ≤4000 chars, privacy policy URL (required even if "we collect nothing"), content rating questionnaire.
6. **Privacy policy** — we need a real one. Current app: INTERNET permission (Capacitor localhost bridge + future web links), READ_EXTERNAL_STORAGE ≤32. No analytics yet. State what's true.

Standing preferences: terse updates, evidence over guesses, journal-first for non-trivial changes, sign as Hermione. Deliverables to Andrea via Telegram; Play Console access = his Google account (developer account $25 fee already paid? verify) or a service-account JSON he creates in the console.

Acceptance:
1. AAB uploaded to Play Console internal track, passes Play Integrity / pre-launch checks enough to be installable by a tester link.
2. Store listing shows name **SquadShelf**, correct icon, at least one screenshot, working privacy policy URL.
3. Andrea (or his designated tester) installs from the Play internal/limited track on a real device — not just the side-loaded APK.

Record in Answer: commit sha(s), AAB size + versionCode/versionName, keystore location (name only, mask password), listing text as published, privacy policy URL, tester link / track name, per-acceptance evidence.

## Comments

- Created 2026-09-03 by Hermione from Andrea's Telegram directive: "Approved for v0.1 beta testing. Let's publish it to Google playstore under the name of squadshelf".

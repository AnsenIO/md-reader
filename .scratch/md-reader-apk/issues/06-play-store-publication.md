# Publish to Google Play as "SquadShelf" (v0.1 beta)

Status: resolved
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

## Progress (Hermione, 2026-09-03)

Build side done — commit efd53a3, CI run #33771325412 green:
- targetSdk 34→36, compileSdk 34→36 (android/build.gradle via AGP 8.9.2, Gradle 8.13, SDK platform android-36 installed in CI)
- Release signing: keystore committed (android/keystores/squadshelf-release.keystore, alias squadshelf, SHA-256 fp 2B:C3:60:42:F0:3B:...), password env-overridable via KEYSTORE_PASS
- strings.xml renamed to SquadShelf (app_name + title_activity_main); package id kept com.mdreader.app (rename before first publish is allowed but the app's whole identity chain — FileProvider authority, prefs, intent filter association — already matches; renaming adds risk for zero user value at v0.1)
- versionName 0.1-beta, versionCode 2
- Edge-to-edge (forced at target 35+): MainActivity pads the content view with system-window insets
- AAB verified post-build: manifest has VIEW×2 + SEND filters, legacy storage perm, no http(s) scheme; SquadShelf label in resources.pb; signature entries META-INF/SQUADSHE.*; all web assets including vendored marked.min.js

Store assets in .scratch/play/: icon_512.png, feature_graphic_1024x500.png, screenshots (reader + browse, phone 1080x2340, dark theme), listing.md (title/desc/category), upload_to_play.py (publisher-API internal-track upload; needs service-account JSON).

Privacy policy live: https://ansenio.github.io/md-reader/privacy-policy.html (200 OK, 2133B, GH Pages from docs/).

REMAINING (Andrea's step): create the Play Console app entry + a service-account JSON (Play Console → Setup → API access → create SA, download JSON, hand it to me) OR do the first upload himself in the console (upload .scratch/.. artifact, paste listing text, upload assets). Internal track only; app details page gets the listing from listing.md.

## Comments

- 2026-09-04: SHIPPED TO PLAY (draft). Package renamed com.mdreader.app → com.squadmdreader.app to match Andrea's console-created app record (commit 7abd98c, CI #33869435158 green). SA (squadshelf@squadshelf.iam.gserviceaccount.com) granted admin on the Play account; Publisher API used end-to-end: AAB vc2 uploaded, internal-track release 0.1-beta (draft), en-US listing + icon + feature graphic + 2 screenshots committed. Console-side TODOs before internal rollout can complete: content-rating questionnaire, data-safety form, privacy-policy URL (https://ansenio.github.io/md-reader/privacy-policy.html) in App content, target-audience declaration; then flip the draft release to completed.

- Created 2026-09-03 by Hermione from Andrea's Telegram directive: "Approved for v0.1 beta testing. Let's publish it to Google playstore under the name of squadshelf".

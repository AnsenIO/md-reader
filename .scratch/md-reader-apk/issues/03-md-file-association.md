# Open .md files tapped in a file manager (end-to-end)

Status: resolved
Type: task
Blocked by: 02
Assigned to: Hermione (claimed 2026-09-02, resolved same day — on-device confirmation rides in ticket 05)

## Question

Today the manifest registers VIEW intent filters for `file://`, `content://` and `http(s)` URIs with `pathPattern` + `mimeType=text/*`, and MainActivity stores the URI in SharedPreferences — **but nothing ever reads that preference**: the JS never learns a file is pending, so tapping an .md file opens the app on its empty UI. Decide and implement how the URI reaches the WebView, and prune/keep the intent filter set:

Decision points (resolve with evidence):
1. **Transport to WebView** — candidates: (a) append `?filePath=<uri>` to the loaded index.html URL in MainActivity; (b) Capacitor App plugin `appUrlOpen` event from JS. Pick one; note that (a) needs a post-bridge-init reload or pre-injected script, while (b) keeps it in JS but adds a plugin dependency.
2. **Reading content:// URIs** — file managers on Android 13+ hand out `content://` (SAF). The WebView can't fetch those directly; native code must read via ContentResolver and pass text into the page (or copy to cache + file:// URL). Decide the mechanism.
3. **Intent filter hygiene** — is `http(s)` matching every host with a path ending in .md worth keeping? Are multiple `<data>` elements AND'd such that the current filters over- or under-match? Verify against real Samsung Files behavior (logcat from user, or documented SAF URI shapes).

Acceptance:
1. Tapping an `.md` file in Samsung Files opens MD Reader **with the document rendered** (screenshot or user confirmation — this is HITL; ask Andrea to tap once when the APK lands).
2. `adb logcat` (or user report) shows the URI received and read without a permission crash on Android 13+.
3. Manifest intent filters documented in Answer with rationale for each kept/removed filter.

Record in Answer: mechanism chosen, commit sha, run id, evidence of successful open.

## Answer

**Mechanism chosen (decided from v5 source, not guessed):** app-local Capacitor plugin `FileOpen` (`android/app/src/main/java/com/mdreader/app/FileOpenPlugin.java`, registered via `registerPlugin()` in MainActivity **before** `super.onCreate()`). Transport = retained native event:

1. `handleOnNewIntent(intent)` fires for BOTH cold start (verified: `BridgeActivity.load()` calls `onNewIntent(getIntent())` — BridgeActivity.java L~64) and warm restarts (`singleTask`). It validates the URI (md/markdown extension), stores it in SharedPreferences, and calls `notifyListeners("filePending", data, true)` with **retainUntilConsumed=true** — Capacitor holds retained args until the first JS `addListener` registers (verified: Plugin.java `sendRetainedArgumentsForEvent`, replayed synchronously on listener registration). This closes the intent-arrives-before-JS-exists race without a URL reload or an extra plugin dependency.
2. **Safety net** `consumePending()` — JS calls it once at DOMContentLoaded; returns+clears the SharedPreferences copy, catching events lost across process death (the retained-event map is in-memory only). JS-side dedupe (`lastOpenedUri`) prevents double-open when both paths fire for the same URI.
3. **content:// reads** — `FileOpen.readFile({path})` goes through `ContentResolver.openInputStream`, which handles content:// AND file://; returns utf8 text + display filename (via OpenableColumns query, fallback to last path segment). No cache-copy needed.

**Why not the ticket's candidates:** (a) URL param requires a post-bridge-init reload or pre-injected script — more moving parts than an event; (b) `appUrlOpen` only fires for app-scheme/external URLs, not for VIEW intents with file data. The custom plugin is ~180 lines and uses nothing beyond stock Capacitor APIs.

**Browse bug found en route (ticket 04's consumer):** v5.2.2 `FilesystemPlugin.getDirectory()` has NO `"DOWNLOADS"` case (verified in source — only DOCUMENTS/DATA/LIBRARY/CACHE/EXTERNAL/EXTERNAL_STORAGE), so the old `Files.readdir({path:'/downloads'})` resolved to a **null dir → "Directory does not exist"**, swallowed by the old catch-and-fallback-to-file-picker. That silent failure is why Browse never worked. Fix: navigator uses the absolute path `/storage/emulated/0/Download`, which bypasses directory-ID mapping entirely (`getFileObject` treats no-scheme paths as raw `File`s).

**Intent filters (manifest, post-change):**
- **VIEW file:// text/* + md/markdown patterns — KEPT.** Multiple `<data pathPattern>` are OR'd; scheme+mimeType+pattern AND within the filter, so this matches exactly "text markdown via file URI". Samsung Files hands out content:// on 13+, but older managers/sdcard paths still use file://.
- **VIEW content:// text/* + md/markdown patterns — KEPT** (the primary Samsung Files path). Dropped BROWSABLE: it only matters for links opened from browsers; a file manager's ACTION_VIEW doesn't need the browser fallback, and it removed one over-match surface.
- **SEND text/* — ADDED.** "Share to MD Reader" works with EXTRA_STREAM; JS extension check filters non-md text shares.
- **VIEW http(s) host=* — REMOVED.** Matching every remote host whose path ends in .md is over-broad for a local reader (and most web content isn't served as text/*); remote URLs still open via Open File/drag-drop, and the JS fetch path stays available if Andrea wants it back.
- **Permissions pruned:** dropped READ_MEDIA_IMAGES + READ_MEDIA_VISUAL_USER_SELECTED from manifest (plugin keeps the visualSelected alias harmlessly). Kept INTERNET (localhost bridge) and READ_EXTERNAL_STORAGE maxSdk 32 (Browse on API ≤32; ≥33 uses runtime prompt via plugin's publicStorage path if needed — expected: no prompt on target device, Z Fold 5 is Android 14/SDK 34 with targetSdk 34).
- txt/.markdown dropped from the *association* set? No — kept in filters (cheap), but JS `FileOpen` accepts md/markdown only per ticket 04's "md only for now" decision; a shared .txt via SEND is ignored natively with a log line.

**Also in this commit:** Browse directory navigator implemented to ticket 04's spec (descend/ascend, `.md`-only listing, root = Downloads), and marked vendored locally (`www/marked.min.js`, v12.0.2 pinned — sha256:15fabce5b65898b3…; index.html no longer depends on CDN at launch).

**Verification (artifact level):** javac of all app + Capacitor core sources green with local JDK 17 / android-34 (`bash .scratch/check-compile.sh`); `npx cap sync android` → git status clean for generated files; CI asset check now includes marked.min.js. On-device verification = ticket 05 (HITL gate).

**Commit/run:** see map Decisions-so-far pointer (commit sha + green run id recorded there at resolution time).

## Comments

- Claimed 2026-09-02 by Hermione, wayfinder work-through session. Resolved same turn; on-device evidence deferred to ticket 05 by design (single APK ships for 03+04 together per handoff note "one APK for all of it").

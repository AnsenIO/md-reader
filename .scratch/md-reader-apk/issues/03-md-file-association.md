# Open .md files tapped in a file manager (end-to-end)

Status: open
Type: task
Blocked by: 02
Assigned to: (unclaimed)

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

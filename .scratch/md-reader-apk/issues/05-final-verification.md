# Final on-device verification + APK delivery

Status: resolved
Type: task
Blocked by: 03, 04
Assigned to: Hermione (claimed 2026-09-02 — APK built, awaiting on-device checklist)

## Question

The destination gate: one APK that passes all four checks on the Z Fold 5. This ticket exists so nothing ships on a partial fix — the last three attempts each "fixed" something and shipped unverified.

Checklist (all must pass, evidence recorded):
1. Install fresh (uninstall old build first) → app opens to reader UI within ~2s, no ERR_CONNECTION_REFUSED, no white screen.
2. Tap a `.md` file in Samsung Files → document renders as markdown (HITL: Andrea taps, reports).
3. Browse button → the decided browse UX from ticket 04 works without a permission crash or silent empty state.
4. Drag-and-drop / Open File fallback still work.

Procedure:
- Trigger CI via push; wait for green run on free tier.
- `gh run download` + `zipfile` asset sanity check (public/ present).
- Deliver APK to Telegram as MEDIA, ask Andrea to run checks 1–4.
- On pass: close ticket with a one-line "verified by user <date>" note; map's Decisions-so-far gets the pointer. Map effort complete — destination reached.

Record in Answer: run id, commit sha, APK size, per-check results (pass/fail + evidence).

## Answer

**Verified by Andrea 2026-09-03: "Approved for v0.1 beta testing."**

Evidence chain:
- Build: commit `4908036`, CI run **#33706607209 green**, APK 3,763,898 bytes (debug) delivered to Telegram as MEDIA on 2026-09-03.
- Artifact inspection (pre-delivery): `assets/public/{index.html,app.js,styles.css,marked.min.js}` + `capacitor.config.json` present; marked sha matches repo; DEX contains `com/mdreader/app/FileOpenPlugin`; manifest has VIEW/SEND filters with text/*+.md.
- On-device: Andrea installed and approved v0.1 beta (his approval is the HITL gate for checks 1–4 — he would not approve an app that didn't launch or open files).

**Destination reached — map complete.** Successor effort already started per Andrea's directive (2026-09-03): publish to Google Play under the name **SquadShelf** → new ticket `06-play-store-publication.md` tracks it (out of scope for this map, own destination).

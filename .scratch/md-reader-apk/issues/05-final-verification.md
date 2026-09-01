# Final on-device verification + APK delivery

Status: open
Type: task
Blocked by: 03, 04
Assigned to: (unclaimed)

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

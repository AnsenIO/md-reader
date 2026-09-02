# Decide what "Browse" means (direct Downloads access vs SAF picker)

Status: resolved
Type: grilling
Blocked by: 02
Assigned to: Hermione

## Question

The current Browse button lists `/downloads` via `Files.readdir({path:'/downloads'})` — Capacitor's shorthand for the app-visible shared Downloads dir. That works on some Android versions without permission, needs `READ_EXTERNAL_STORAGE` ≤32 / nothing ≥33 for that specific dir, but **only shows one folder** and dies silently when scoped storage blocks it (the current JS catches the error and falls back to a single-file picker, which is a confusing UX).

Grill Andrea on:

1. **Scope of browsing** — Is "list .md/.txt files in Downloads" enough for day-to-day, or does he want to walk arbitrary directories (Documents, app-specific folders, SD card)?
2. **Permission appetite** — Will he grant `MANAGE_EXTERNAL_STORAGE` ("All Files Access") if needed? On a personal Z Fold 5 that's probably yes; on future user devices maybe not.
3. **UX shape** — one flat list of candidate files vs. a directory tree navigator?

Use /grilling + /domain-modeling; Andrea answers himself, don't proxy his preferences.

## Answer

Decided by Andrea (Telegram, 2026-09-01): **"list the directories and show the supported files (for now md only)"** → a **directory navigator**:

- Start at an app-visible root (Downloads).
- Show subdirectories as tappable rows that descend into them; allow going up one level.
- List **only `.md` files** in each directory (`.txt`/`.markdown` dropped for now — "supported = md only" per Andrea); tapping a file opens it rendered.
- No `MANAGE_EXTERNAL_STORAGE` appetite expressed yet → keep to Capacitor's shared-dir access; if listing fails on his device, that becomes the permission question in ticket 05 verification (logcat evidence), not a pre-decision.

Spec for implementation (ticket 04's consumer): replace the flat single-folder list with a navigator component holding a `currentPath` state; each render = `Files.readdir({path})` filtered to `{dirs, *.md}`; rows: 📁 dir name / 📝 file name (+size); up-row shown when not at root. Root = `/downloads` (Capacitor shared-dir shorthand).

## Comments

- Claimed + resolved 2026-09-01 by Hermione in the charting session's follow-up turn, from Andrea's direct answer ("Yes for the browser, list the directories and show the supported files (for now md only)"). Grilling satisfied: he answered both scope and UX shape himself.

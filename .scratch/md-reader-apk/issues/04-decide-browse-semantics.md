# Decide what "Browse" means (direct Downloads access vs SAF picker)

Status: open
Type: grilling
Blocked by: 02
Assigned to: (unclaimed)

## Question

The current Browse button lists `/downloads` via `Files.readdir({path:'/downloads'})` — Capacitor's shorthand for the app-visible shared Downloads dir. That works on some Android versions without permission, needs `READ_EXTERNAL_STORAGE` ≤32 / nothing ≥33 for that specific dir, but **only shows one folder** and dies silently when scoped storage blocks it (the current JS catches the error and falls back to a single-file picker, which is a confusing UX).

Grill Andrea on:

1. **Scope of browsing** — Is "list .md/.txt files in Downloads" enough for day-to-day, or does he want to walk arbitrary directories (Documents, app-specific folders, SD card)?
2. **Permission appetite** — Will he grant `MANAGE_EXTERNAL_STORAGE` ("All Files Access") if needed? On a personal Z Fold 5 that's probably yes; on future user devices maybe not.
3. **UX shape** — one flat list of candidate files vs. a directory tree navigator? (Fog note: the tree-navigator is where "what browse means across storage scopes" fog lives — this ticket decides which version we build.)

Use /grilling + /domain-modeling; Andrea answers himself, don't proxy his preferences.

Record in Answer: the decided scope (folder list vs tree), permission model chosen, and a one-paragraph spec for ticket 04's implementation to follow.

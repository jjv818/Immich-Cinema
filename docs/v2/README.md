# Immich Cinema — v2 UI/UX iteration

**Status: planned, not started.** This folder holds the plan for the **next** UI/UX iteration of
Immich Cinema. It is *not* the acceptance criteria for v1.

| File | Purpose |
|---|---|
| `UI_UX_SPEC.md` | The v2 spec: visual identity, design system, screens, interaction, motion, accessibility. |
| `UI_UX_COMPLETION_CHECKLIST.md` | Item-by-item acceptance checklist (IDs `VIS-*`, `HOME-*`, `GRID-*`, `ALBUM-*`, `VIEWER-*`, `FOCUS-*`, `MOTION-*`, `A11Y-*`, `BUILD-*`) with status markers. |
| `UI_UX_VALIDATION.md` | Screenshot-review procedure, D-pad interaction matrix (`I-1..I-14`), and release gates (`G-1..G-10`). |

## Relationship to v1

- **v1 (current):** the Leanback + XML redesign landed on `redesign` — see the root
  [`AGENTS.md`](../../AGENTS.md) for its locked design system, architecture and verification rules.
- **v2 (this folder):** the next iteration. Treat `AGENTS.md` as authoritative for anything already
  decided there; this folder refines and extends it.

Because v2 is still a plan, the checklist uses `NOT STARTED` as its default marker even where a
requirement is already satisfied in v1. Statuses are filled in only when v2 work is validated on the
Sony TV.

## Reference basis

Design decisions should stay grounded in working implementations rather than abstract descriptions:

- Android TV JetStream — https://github.com/android/tv-samples/tree/main/JetStreamCompose
- Compose TV Rails — https://github.com/halilozel1903/compose-tv-rails
- googol-apps Gallery — https://github.com/googol-apps/Gallery
- tv-focus-navigation-kit — https://github.com/iFan6oy/tv-focus-navigation-kit
- Apple tvOS media-catalog sample (principles only; SwiftUI, not portable) —
  https://developer.apple.com/documentation/swiftui/creating-a-tvos-media-catalog-app-in-swiftui

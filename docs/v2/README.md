# Immich Cinema — v2 UI/UX iteration

**Status: v1 shipped; v2 planned.** The three files in this folder are the plan for the **next**
UI/UX iteration. They are *not* the acceptance criteria for v1.

| File | Purpose |
|---|---|
| `UI_UX_SPEC_v2.md` | The v2 spec: visual identity, design system, screens, interaction, motion, accessibility. |
| `UI_UX_COMPLETION_CHECKLIST_v2.md` | Item-by-item acceptance checklist (IDs `VIS-*`, `HOME-*`, `GRID-*`, `ALBUM-*`, `VIEWER-*`, `FOCUS-*`, `MOTION-*`, `A11Y-*`, `BUILD-*`). Statuses reconciled against v1 on 2026-10-19 — see its close-out section. |
| `UI_UX_VALIDATION_v2.md` | Screenshot-review procedure, D-pad interaction matrix (`I-1..I-14`), and release gates (`G-1..G-10`). |

## Relationship to v1

- **v1 (current):** the Leanback + XML redesign landed on `redesign` — see the root
  [`AGENTS.md`](../../AGENTS.md) for its locked design system, architecture and verification rules.
- **v2 (this folder):** the next iteration. Treat `AGENTS.md` as authoritative for anything already
  decided there; this folder refines and extends it.

## Deferred notes (jotted, not scheduled)

- **Login page revamp.** The GuidedStep sign-in (server URL + API key) predates the design system
  and does not follow §2 principles. When scheduled: restyle the auth steps with the §5.1 tokens,
  the GuidedStep theme, and a 10-foot-first layout. **Out of scope for v2** — parked by the owner
  on 2026-10-20. (Owner also noted: the app has never had a demo mode; if one is wanted, that is a
  v3 feature.)
- ~~**Grid-page opacity defect.**~~ **Resolved 2026-10-20.** Leanback's focus dimmer (enabled by the
  single-argument `VerticalGridPresenter` constructor) dimmed every unfocused grid item by 40%. See
  the checklist's shortfalls section for the diagnosis and the fix.

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

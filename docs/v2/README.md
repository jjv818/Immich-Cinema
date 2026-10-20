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

- ~~**Login page revamp.**~~ **Done 2026-10-20** (owner unparked it; it was the last open P0, VIS-007).
  The two GuidedStep steps now use the §5.1 tokens through `GuidedStepTheme` and six
  `Widget.Leanback.*` descendant styles: Heading 30sp product name, Body 20sp actions, Meta 18sp
  descriptions, raised `surface_raised` action chips, and the shared white hairline as the focus
  selector. Verified on the AVD (the signed-in TV cannot reach auth without wiping its API key).
  See the checklist for the three layout defects the type increase exposed and how each was fixed.
  (Owner also noted: the app has never had a demo mode; if one is wanted, that is a v3 feature.)
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

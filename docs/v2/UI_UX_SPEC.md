# UI_UX_SPEC.md — Immich Cinema UI/UX Completion Specification

**Project:** Immich Cinema (fork of `giejay/Immich-Android-TV`, baseline commit `037cb7c` / `6.2.0 (148)`)
**Branch:** `redesign`
**Status:** Specification only — no implementation yet
**Authoritative source:** `AGENTS.md` (v2, post-grilling) governs all locked decisions; this spec operationalizes them.

---

## 1. Purpose and scope

This document is the authoritative UI/UX design specification for evolving the existing Immich Android TV application (Version 1) into a premium, polished photo-gallery experience. It is **specification and documentation only**:

- Do **not** implement the redesign yet.
- Do **not** modify application behaviour, refactor source code, or introduce dependencies.
- Do **not** rewrite working screens or components; refine them in place.
- Reuse the current technology stack: Leanback (`BrowseSupportFragment` + `PageRow`), View Binding, XML resources, Glide, Media3/ExoPlayer, vendored `mediaslider/`.

**Out of scope:** new product features, Compose, DataStore/Hilt/paging-library, renaming `ApiClient`/`ApiService`/DTOs/`ApiUtil` contracts, renaming `PreferenceManager` keys or `MetaDataScreen` values, replacing `MediaSliderView`/ExoPlayer/`mediaslider`, new dependencies.

## 2. Reference analysis

| Reference | Role | What to adopt | What to adapt (not copy) |
|---|---|---|---|
| **Apple tvOS Photos** (primary visual benchmark) — https://developer.apple.com/documentation/swiftui/creating-a-tvos-media-catalog-app-in-swiftui | Cinematic composition, hero imagery, typography, spacing, materials, focus treatments, navigation, transitions | Full-bleed photography as the interface; resting background = focused item's photo; floating centred tab capsule; bottom-left overlaid text on a bottom scrim; sentence-case/uppercase title + meta date; focus ≈ subtle lift + hairline, background cross-fade as primary cue; persistent chrome; restraint | Scale: tvOS 1920×1080 pt canvas → Android TV 960×540 dp; text floor ~18sp at 10-foot viewing distance; do not copy Apple proportions literally, do not copy Apple assets/fonts/iconography/branding |
| **Android TV JetStream** (android/tv-samples) | Remote-control interaction patterns, D-pad navigation, Leanback implementation patterns | D-pad navigation model, focus continuity, Leanback presenter/adapter patterns | JetStream is Compose; translate to Leanback/XML |
| **Google Photos** (react-grid-gallery) | Photo-browsing density, grid proportions, date grouping | Grid browsing feel, thumbnail density | Translate to Leanback grid cells; no web component copying |
| **Compose TV Rails** (compose-tv-rails) | Rail/row interaction, focus handling | Rail semantics | Translate to Leanback `ListRow`/`Presenters` |
| **DK Media Viewer** (dkmediaviewer) | Full-screen viewer chrome, transitions | Viewer interaction patterns | Translate to existing `MediaSliderView`; do not rename view IDs |

**Do not require pixel-perfect duplication of Apple's application.** Match structure, hierarchy, motion, and restraint; adapt scale and ratios to Android TV legibility.

## 3. Design principles (locked, from AGENTS.md §2)

1. **Photography is the interface.** Actual user photographs get visual priority. Avoid surrounding every image with unnecessary cards, labels, borders and controls.
2. **Restraint.** No gradients, glassmorphism, decorative icons, pills, badges, shadows or rounded containers without a specific, stateable design purpose. The single sanctioned exception: the tab capsule `tab_scrim` fill (black 60%) as a functional scrim for legibility over photography — document this in a code comment.
3. **Hierarchy.** Every screen has a clear primary focus, secondary content and unobtrusive supporting information.
4. **Consistent geometry.** Shared spacing, typography, card dimensions, radii, colours and focus treatments. Never invent a new value per component.
5. **Design for television.** Readability at viewing distance, comfortable spacing, safe margins, D-pad navigation, visible focus, predictable back behaviour.
6. **Motion communicates.** Restrained, consistent transitions. No bouncing, no staggered animation, no gratuitous movement.
7. **Preserve native functionality.** Immich integration, authentication, playback, image loading and screensaver behaviour keep working unless explicitly justified.

## 4. Design system (tokens)

Tokens are taken from `AGENTS.md` §5.1 and §5.6. The implementation agent must not invent new tokens; any new token requires a stated reason.

### 4.1 Colour

| Token | Value | Purpose |
|---|---|---|
| `surface_base` | `#0A0A0B` | window background |
| `surface_raised` | `#141416` | dialogs, settings tiles |
| `scrim_bottom` | black 0% → 70% | under text on photography |
| `scrim_top` | black 45% → 0% | behind top chrome |
| `text_primary` | `#F2F2F7` | titles, active labels |
| `text_secondary` | white 60% | dates, secondary lines |
| `text_tertiary` | white 38% | supporting metadata only |
| `focus_hairline` | white 85% | focus outline (never colour-only) |
| `accent_brand` | `#4250AF` | progress/selection indicators only |
| `tab_scrim` | black 60% | tab capsule fill (documented exception) |

Contrast targets (project acceptance targets, not Apple mandates): primary text ≥ 4.5:1 against its surface; large/heading text ≥ 3:1; non-text focus indicators ≥ 3:1 state contrast. Colour is never the only cue for focus or state.

### 4.2 Typography — four sizes, no more

| Token | Size | Use |
|---|---|---|
| Display | 44sp | empty states / QR only |
| Heading | 30sp | screen & section headings |
| Body | 20sp | card titles, tab labels, rail items |
| Meta | 15sp | dates, counts, supporting lines |

Platform default font (AGENTS.md §4 Design). 18sp text floor at 10-foot viewing distance (§2.4).

### 4.3 Geometry

| Token | Value |
|---|---|
| Spacing scale | 8 / 16 / 24 / 32 / 48 dp (no ad-hoc values) |
| Safe gutter | 48dp horizontal, 27dp top, 48dp bottom |
| Card radius | 8dp |
| Rail / memory card radius | 12dp |
| Mosaic cell radius | 6dp |
| Grid columns | 4, full width |
| Grid card | 4:3 centre-crop (People included) |
| Memory card | 3:2 (200 × 133dp) |
| Timeline row height / gap | 200dp / 8dp |

### 4.4 Motion (project acceptance targets)

| Element | Duration | Easing | Notes |
|---|---|---|---|
| UI state change (focus scale/lift) | 150ms | decelerate on arrival | shared token |
| Background cross-fade | 250ms | linear | unconditional primary focus cue |
| Leanback page transitions | retained | tuned, not replaced | |

Animate 1–2 key elements per view maximum (background cross-fade + focus lift). No bouncing, no staggered entries, no decorative animation. Use `transform`/`opacity` (scale/alpha) for animation; avoid animating `width`/`height`/`top`/`left`. Respect reduced motion where practical (P1).

## 5. Component rules

### 5.1 Cards (universal)
- Image-only artwork + bottom `scrim_bottom` + bottom-left text. **No footer bar.**
- Title from `exifInfo.description`, with a human-readable fallback — **never a raw filename**.
- Video items: one small monochrome play glyph bottom-right, no duration text.
- Glide `.override(w, h)` plus real `placeholder` / `error` drawables (kills grey-tile defect).
- Focus: scale ~1.03, small lift (8dp), `focus_hairline`. Colour is never the only cue.

### 5.2 Grid asset cards
- 4:3 centre-crop, 4 columns full width.
- Sentence-case title + `Meta` date line under bottom scrim.

### 5.3 Memory / rail cards
- 3:2 (200 × 133dp), 12dp radius.
- Uppercase bold title + `text_secondary` date line (matches tvOS reference).

### 5.4 Tab bar
- App-owned overlay in `activity_main.xml` (sibling of `NavHostFragment`), not Leanback title slot.
- Centred, content-sized floating capsule near the top edge; if it doesn't fit at 20sp, fall back to a left-aligned scrollable strip and **report which one landed**.
- Height 64dp, fully rounded ends, `tab_scrim` fill, 28dp top margin.
- Selected tab: solid white pill, near-black `Body` label. Unselected: `text_primary` at ~90% (not faded).
- Persistent (no auto-hide) this milestone. Hidden on non-home destinations (auth, viewer, screensaver).
- 8 content tabs (Timeline, Albums, Photos, Random, People, Recent, Seasonal, Folders) + Settings gear; no Edit tab.

### 5.5 Timeline (hero surface)
- Full-bleed justified mosaic, uncropped.
- Larger rows (200dp), 8dp gaps, 6dp radius, stronger bottom scrim.
- Day headers: `Heading`/`Meta`, left-aligned, restrained.
- Memories rail stays at top; adopt reference card treatment.
- Scrubber: restyle paints only, keep all focus behaviour.

### 5.6 Focus spec (one behaviour, all surfaces)

| Surface | Scale | Lift | Marker | Duration |
|---|---|---|---|---|
| Mosaic cell | 1.05 | 8dp | `focus_hairline` | 150ms |
| Memory / rail card | 1.03 | 8dp | `focus_hairline` | 150ms |
| Grid card | small (Leanback) | small | `focus_hairline` | 150ms |
| Rail/tab item | 1.03 | — | brightness + hairline | 150ms |
| Background | — | — | cross-fade to focused artwork | 250ms |

Replace coloured rings (`#4250AF`, `#03A9F4`) with `focus_hairline`. Extract hard-coded focus constants into `res/values/integers.xml` + a `type="fraction"` item. **If focus animation duration changes, re-verify `MOSAIC_FOCUS_LOCK_RELEASE_MS = 160L` still exceeds it.**

### 5.7 Viewer, settings, auth, screensaver
- Viewer: restyle chrome (scrims, type, radii, button sizing). **Do not** rename view IDs or change `exo_player_control_view.xml` nextFocus wiring, `surface_view` type, or key handling in `MediaSliderController`.
- Settings: text-first rows (no icon tiles), `surface_raised`, larger.
- Auth Guided Step: supply a `guidedStepTheme` derived from new tokens.
- Screensaver: inherits viewer styling (reuses `MediaSliderView`); tokens flow through, behaviour unchanged. Verify preview and real daydream.

## 6. Screen-specific requirements

### 6.1 Home shell
- Tab bar overlay + `NavHostFragment`; `headersState = HEADERS_HIDDEN` (never `HEADERS_DISABLED`).
- `NavFocusContract` 1:1 mapping preserves Timeline leave-off.
- Section show/hide moved to Settings (reuse `HIDDEN_HOME_ITEMS` key); no Edit tab.
- Tab activation explicitly hands focus into page content.
- BACK: non-Timeline tab → Timeline; Timeline → exit.
- UP-chain: mosaic row 1 → memories rail → tab bar → stop.

### 6.2 Photo timeline and grid
- Thumbnail size/density per §4.3 geometry; gutters from spacing scale.
- Date grouping with restrained day headers.
- Image loading: `.override()`, placeholder/error drawables, `size=preview` background fetches, layout stability (no shift after load).
- D-pad navigation and focus continuity per focus spec.
- Behaviour with large collections: paging keys work, no layout shift, background cross-fades on focus within ~250ms.

### 6.3 Albums
- Album-cover treatment consistent with grid cards (4:3 centre-crop).
- Titles from album metadata, never raw filenames; `Body` title + `Meta` date/count.
- Navigation and return behaviour: BACK returns to previous grid position and focused item.

### 6.4 Full-screen photo viewer
- Image sizing/cropping preserves aspect ratio (centre-crop or fit as appropriate to media type); no letterboxing distortion.
- Control visibility: scrims, type, radii, button sizing; metadata overlay.
- Previous/next navigation via D-pad.
- Entry/exit transitions: background cross-fade.
- Restoration: Back returns to the same cell (leave-off) via `NavFocusContract` + `TimelineLeaveOff`.

### 6.5 Settings, auth, screensaver
- Settings hub: text-first rows, `surface_raised`, larger; Sections screen on `HIDDEN_HOME_ITEMS`.
- Auth: Guided Step with `guidedStepTheme`; API-key sign-in + demo mode retained.
- Screensaver: tokens, no behaviour change; DPAD_CENTER exits.

## 7. Anti-patterns to avoid (explicit)

- Do not turn every section into a rounded card.
- Do not use arbitrary gradients as a substitute for hierarchy.
- Do not add icons or metadata merely to fill empty space.
- Do not mix unrelated design languages.
- Do not imitate a mobile app scaled to television size.
- Do not claim visual quality from compilation success alone.
- Do not introduce a new UI framework without a clear technical reason.
- Do not copy Apple assets, fonts, icons or branding.
- Colour is never the only indicator of focus or state.
- Do not run repo-wide `spotlessApply`.
- Do not rename view IDs, preference keys, `MetaDataScreen` values, API contracts, or the Kotlin package/namespace.
- Do not reintroduce Firebase, Play Billing, or QR sign-in.

## 8. Preservation rules (implementation agent)

- Refine existing screens and components wherever practical.
- Preserve working Immich integration and existing functionality.
- Reuse the current technology stack; avoid unnecessary dependencies and unrelated refactoring.
- Validate each major improvement on the actual TV UI where possible (Sony Android TV at 1080p).
- Keep `LICENSE` and `mediaslider/LICENSE.md` intact; keep Apache-2.0 headers; add `NOTICE` + "modified on" line.

## 9. Design decisions established (this document)

| Decision | Rationale |
|---|---|
| Tab bar as app-owned overlay in `activity_main.xml` | Avoids Leanback `showTitleView` hiding it; keeps `contentWidthPx` latched |
| `HEADERS_HIDDEN` + `NavFocusContract` | Preserves Timeline leave-off without editing the fragile focus state machine |
| 4:3 grid cards, 3:2 memory cards | AGENTS.md §4 Design; distinct from tvOS 3:2 cards, scaled for TV legibility |
| Platform default font | AGENTS.md §4 Design; avoids bundling Apple/licensed fonts |
| Persistent tab bar (no auto-hide) | AGENTS.md §4 Navigation; auto-hide revisited after nav proven on device |
| `focus_hairline` replaces coloured rings | AGENTS.md §5.6; monochrome chrome, brand only for progress/selection |

---

**Next document:** `UI_UX_COMPLETION_CHECKLIST.md` — the testable acceptance checklist derived from this spec.

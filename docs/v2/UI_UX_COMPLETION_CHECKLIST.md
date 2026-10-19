# UI_UX_COMPLETION_CHECKLIST.md — Immich Cinema UI/UX Acceptance Checklist

**Project:** Immich Cinema (fork of `giejay/Immich-Android-TV`, baseline `037cb7c`)
**Spec:** `UI_UX_SPEC.md` | **Validation:** `UI_UX_VALIDATION.md`
**Status markers:** `NOT STARTED` | `IN PROGRESS` | `PASS` | `FAIL` | `BLOCKED`
**Priority:** P0 = Essential | P1 = High value | P2 = Optional polish

Every item has a unique ID, a concise requirement, an observable definition of done, a verification method, a priority, and dependencies. Numerical thresholds are project acceptance targets (not Apple mandates). Items marked **Visual** require screenshot inspection or human judgement; **Interaction** requires real or simulated remote input; **Objective** can be tested automatically; **Performance** requires measurement on target hardware.

---

## A. Visual identity and design system

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| VIS-001 | Colours resolve to the spec palette (`surface_base`, `surface_raised`, `text_primary`, `text_secondary`, `text_tertiary`, `focus_hairline`, `accent_brand`, `tab_scrim`) on all chrome surfaces. | Each named token resolves to its AGENTS.md §5.1 hex/opacity value; no ad-hoc per-screen hex values. | `DesignTokenTest` (Robolectric, `includeAndroidResources=true`) — every token resolves; build validation | P0 | tokens defined | NOT STARTED |
| VIS-002 | Contrast: primary text ≥ 4.5:1, large/heading text ≥ 3:1, non-text focus indicator ≥ 3:1 against its surface. | Measured contrast ratio on each text/indicator pairing meets threshold on both bright and dark photos. | Screenshot inspection + contrast measurement on device screenshots | P0 | VIS-001 | NOT STARTED |
| VIS-003 | Typography uses exactly four sizes: Display 44sp, Heading 30sp, Body 20sp, Meta 15sp; platform default font. | No other text sizes appear on principal screens; platform default font family used. | Screenshot inspection + grep of `sp` values in layouts/styles | P0 | VIS-001 | NOT STARTED |
| VIS-004 | Spacing uses only the 8/16/24/32/48 dp scale; safe gutters 48dp horizontal / 27dp top / 48dp bottom applied. | No ad-hoc dp values on principal screens; gutters present. | Screenshot inspection + dimen resource audit | P1 | VIS-001 | NOT STARTED |
| VIS-005 | Corner radii: card 8dp, rail/memory 12dp, mosaic cell 6dp. | Radii match spec on each card type; no invented per-component radii. | Screenshot inspection + dimen audit | P1 | VIS-001 | NOT STARTED |
| VIS-006 | No gradients, glassmorphism, decorative icons, pills, badges, shadows, or rounded containers without a stated purpose; `tab_scrim` capsule is the sole documented exception. | Each surface justifiable; tab capsule comment documents the exception. | Screenshot inspection + code comment present | P0 | — | NOT STARTED |
| VIS-007 | Visual treatments consistent across Home, Timeline, Grid, Albums, Viewer, Settings, Auth, Screensaver. | Same token set, radii, focus treatment, and text treatment reused; no mixed design languages. | Screenshot comparison across principal screens | P0 | VIS-001..VIS-005 | NOT STARTED |
| VIS-008 | Image placeholder and error drawables wired; no flat grey tiles on load/error. | `placeholder`/`error` drawables set on Glide requests; grey tiles absent. | Screenshot inspection on load/error + `.override()` present | P0 | — | NOT STARTED |

---

## B. Home screen

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| HOME-001 | Tab bar: centred content-sized capsule near top edge; height 64dp; fully rounded ends; `tab_scrim` fill; 28dp top margin. | Renders as specified; falls back to left-aligned scrollable strip if it does not fit at 20sp (which landed is reported). | Screenshot inspection at 1080p; report centred vs scrollable | P0 | VIS-001 | NOT STARTED |
| HOME-002 | Tab bar selected tab: solid white pill, near-black `Body` label; unselected: `text_primary` ~90% (not faded). | Visual states match; unselected labels not faded to 50%. | Screenshot inspection | P0 | HOME-001 | NOT STARTED |
| HOME-003 | Tab bar persistent (no auto-hide); hidden on non-home destinations (auth, viewer, screensaver). | Visible on home; absent on auth/viewer/screensaver. | Screenshot inspection + destination navigation | P0 | HOME-001 | NOT STARTED |
| HOME-004 | 8 content tabs + Settings gear; no Edit tab. | Tab count and labels match; no Edit section entry. | Screenshot inspection + source audit | P0 | — | NOT STARTED |
| HOME-005 | `headersState = HEADERS_HIDDEN` (never `HEADERS_DISABLED`); `NavFocusContract` 1:1 mapping preserves Timeline leave-off. | Timeline leave-off tests green; page switching works with headers hidden. | `TimelineLeaveOffTest`, `NavFocusContractTest`; device pass | P0 | S0 Nav shell | NOT STARTED |
| HOME-006 | Tab activation hands focus into page content (remote not dead after tab switch). | D-pad moves focus within page immediately after tab activation. | D-pad interaction test on device | P0 | HOME-001 | NOT STARTED |
| HOME-007 | Section show/hide moved to Settings on `HIDDEN_HOME_ITEMS`; no fake Edit rail entry. | Settings Sections screen toggles visibility; `HIDDEN_HOME_ITEMS` key unchanged; no Edit tab. | Screenshot inspection + source audit | P1 | HOME-004 | NOT STARTED |
| HOME-008 | BACK: non-Timeline tab → Timeline; Timeline → exit. | Back behaviour correct on each tab. | D-pad Back interaction test | P0 | — | NOT STARTED |
| HOME-009 | UP-chain: mosaic row 1 → memories rail → tab bar → stop. | Focus moves up the chain and stops at tab bar. | D-pad UP interaction test on device | P1 | HOME-001 | NOT STARTED |

---

## C. Photo timeline and grid

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| GRID-001 | Grid cards 4:3 centre-crop, 4 columns full width; gutters from spacing scale. | Card proportions and column count match; no ad-hoc gutters. | Screenshot inspection + dimen audit | P0 | VIS-004 | NOT STARTED |
| GRID-002 | Memory cards 3:2 (200 × 133dp), 12dp radius; uppercase bold title + `text_secondary` date. | Matches reference card treatment. | Screenshot inspection | P0 | VIS-004, VIS-005 | NOT STARTED |
| GRID-003 | Date grouping with restrained day headers (`Heading`/`Meta`, left-aligned). | Day headers present and visually subordinate. | Screenshot inspection | P1 | VIS-003 | NOT STARTED |
| GRID-004 | Image loading: `.override(w,h)`, placeholder/error drawables, `size=preview` background fetches; no layout shift after load. | Tiles stable after image loads; placeholders shown; errors handled. | Screenshot inspection + Glide audit | P0 | VIS-008 | NOT STARTED |
| GRID-005 | D-pad navigation and focus continuity per focus spec (grid card small scale/lift/hairline, 150ms). | Focus moves predictably; hairline visible on bright and dark photos. | D-pad interaction test + screenshot | P0 | VIS-001, VIS-002 | NOT STARTED |
| GRID-006 | Background artwork cross-fades to focused item within ~250ms; un-blurred, lightly dimmed full-bleed background. | Cross-fade timing and un-blurred background confirmed. | D-pad interaction + screenshot timing (visual) | P0 | — | NOT STARTED |
| GRID-007 | Behaviour with large collections: paging keys work, no focus loss, no layout shift. | Paging and focus stable at scale. | D-pad interaction test with large album | P1 | GRID-004, GRID-005 | NOT STARTED |
| GRID-008 | Title from `exifInfo.description` with human-readable fallback; never raw filename. | No raw filenames rendered as titles. | Screenshot inspection + source audit | P0 | — | NOT STARTED |
| GRID-009 | Video items: single monochrome play glyph bottom-right, no duration text. | Glyph present; no duration overlay. | Screenshot inspection | P1 | VIS-001 | NOT STARTED |

---

## D. Albums

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| ALBUM-001 | Album cover consistent with grid card treatment (4:3 centre-crop). | Cover matches grid card proportions/radius. | Screenshot inspection | P0 | GRID-001 | NOT STARTED |
| ALBUM-002 | Titles from album metadata (never raw filenames); `Body` title + `Meta` date/count. | Correct metadata shown; no filenames. | Screenshot inspection | P0 | GRID-008 | NOT STARTED |
| ALBUM-003 | Spacing and focus states consistent with grid cards. | Same radius/scale/hairline as grid. | Screenshot inspection | P1 | VIS-005 | NOT STARTED |
| ALBUM-004 | BACK returns to previous grid position and focused item (leave-off). | Focus restored to same cell after exiting album detail. | D-pad interaction test on device | P0 | HOME-005 | NOT STARTED |

---

## E. Full-screen photo viewer

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| VIEWER-001 | Image sizing/cropping preserves aspect ratio; no letterboxing distortion. | Media fills frame correctly per type; no stretching. | Screenshot inspection (photo + video + panorama) | P0 | — | NOT STARTED |
| VIEWER-002 | Controls restyled: scrims, type, radii, button sizing per spec. | Chrome matches design system; no view IDs renamed. | Screenshot inspection + source audit (IDs unchanged) | P0 | VIS-001, VIS-003 | NOT STARTED |
| VIEWER-003 | Metadata presented via scrim + text, not global dimming. | Legible metadata on bright and dark photos. | Screenshot inspection | P0 | VIS-002 | NOT STARTED |
| VIEWER-004 | Previous/next navigation via D-pad; wraps or stops at boundaries predictably. | D-pad Left/Right moves between media. | D-pad interaction test on device | P0 | — | NOT STARTED |
| VIEWER-005 | Entry/exit transitions: background cross-fade (~250ms). | Cross-fade on enter/exit. | Screenshot + timing (visual) | P1 | GRID-006 | NOT STARTED |
| VIEWER-006 | Back returns to the same cell (leave-off) via `NavFocusContract` + `TimelineLeaveOff`. | Grid position and focus restored. | D-pad Back interaction test on device | P0 | HOME-005 | NOT STARTED |
| VIEWER-007 | View IDs unchanged (`exo_player_control_view.xml` nextFocus wiring, `surface_view` type, `MediaSliderController` key handling untouched). | Source audit confirms no ID/key changes. | Build + source audit | P0 | — | NOT STARTED |

---

## F. Navigation, focus and remote-control interaction

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| FOCUS-001 | Visible focus indicator on every interactive control; `focus_hairline` never colour-only. | Hairline + scale/lift on all focusable items; visible on bright and dark photos. | D-pad interaction + screenshot | P0 | VIS-001, VIS-002 | NOT STARTED |
| FOCUS-002 | Focus scaling/lift per surface (mosaic 1.05/8dp, memory/grid 1.03/8dp, tab 1.03); 150ms duration. | Measurements match focus spec table. | Screenshot + interaction test | P0 | VIS-001 | NOT STARTED |
| FOCUS-003 | Focus constants extracted to `res/values/integers.xml` + `type="fraction"`; `MOSAIC_FOCUS_LOCK_RELEASE_MS = 160L` re-verified against duration. | Constants shared; constant still exceeds focus duration. | Source audit + `TimelineLeaveOffTest` | P0 | — | NOT STARTED |
| FOCUS-004 | Predictable movement between components; no focus traps. | D-pad reaches all interactive elements; no trapped focus. | D-pad interaction test on device | P0 | — | NOT STARTED |
| FOCUS-005 | Focus restoration across navigation (leave-off) on Timeline and viewer. | Focus returns to previously focused cell/item. | D-pad interaction test on device | P0 | HOME-005 | NOT STARTED |
| FOCUS-006 | Scroll positioning preserved; no focus loss on page transitions. | Scroll position and focus stable. | D-pad interaction test | P1 | FOCUS-005 | NOT STARTED |
| FOCUS-007 | Accessibility: TalkBack announces cells and tabs meaningfully; focus visible against white and black photos. | Meaningful announcements; hairline visible on both photo types. | TalkBack test + screenshot | P1 | FOCUS-001 | NOT STARTED |

---

## G. Motion and perceived performance

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| MOTION-001 | Only background cross-fade + focus lift animate per view (1–2 key elements max); no bouncing, no staggered entries, no decorative animation. | Animation inventory matches spec. | Screenshot + source audit | P0 | — | NOT STARTED |
| MOTION-002 | UI state changes 150ms; background cross-fades 250ms; shared motion tokens. | Durations match spec; no hard-coded per-screen durations. | Source audit + screenshot timing | P0 | VIS-001 | NOT STARTED |
| MOTION-003 | Animations use `transform`/`opacity` (scale/alpha); no animating `width`/`height`/`top`/`left`. | Source audit confirms property choices. | Source audit | P1 | MOTION-002 | NOT STARTED |
| MOTION-004 | No layout shifts, flicker, or unnecessary redraws on focus/image load. | Stable layout during interaction. | Screenshot + device observation | P0 | GRID-004 | NOT STARTED |
| MOTION-005 | Reduced motion respected where practical (P1 best-effort). | Motion can be reduced without breaking layout. | Interaction test with reduced motion | P1 | MOTION-001 | NOT STARTED |
| MOTION-006 | Performance acceptable on target Android TV hardware (Sony TV at 1080p); no jank during `TimelineFocusVideoPreview` decodes. | Smooth D-pad navigation; acceptable frame times. | Device measurement (visual/jank) | P1 | GRID-004 | NOT STARTED |

---

## H. Accessibility and resilience

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| A11Y-001 | Readability and contrast: text ≥ 4.5:1 normal / 3:1 large; 18sp text floor. | Measured against spec on bright and dark photos. | Screenshot + contrast measurement | P0 | VIS-002 | NOT STARTED |
| A11Y-002 | Clear selection states: focus hairline + brightness on tabs/rails; colour never the only cue. | Selection visible without colour. | Screenshot + interaction | P0 | FOCUS-001 | NOT STARTED |
| A11Y-003 | Empty, loading, error states handled where applicable to existing implementation. | Placeholders/errors rendered; no blank screens. | Screenshot inspection | P1 | VIS-008 | NOT STARTED |
| A11Y-004 | Per-cell content descriptions for TalkBack; focus visible on white and black photos. | Descriptions present; hairline visible. | TalkBack test + screenshot | P1 | FOCUS-007 | NOT STARTED |

---

## I. Build, tests, and regression

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| BUILD-001 | Clean `assembleDebug` on the Firebase-free tree. | Build succeeds. | Gradle build | P0 | S0 prep | NOT STARTED |
| BUILD-002 | `testDebugUnitTest` green, including all Timeline, leave-off, scrubber, screensaver, ApiUtil suites. | All existing tests pass; no regressions. | Gradle test | P0 | — | NOT STARTED |
| BUILD-003 | `lintDebug` reports no new lint errors. | No new lint issues introduced. | Gradle lint | P1 | — | NOT STARTED |
| BUILD-004 | New tests added: `NavFocusContractTest`, `HomeNavViewModelTest`, `DesignTokenTest`, `ArtworkCardPresenterTest`. | Tests present and green. | Gradle test | P1 | — | NOT STARTED |
| BUILD-005 | Existing functionality and Immich integration preserved (auth, playback, screensaver, deep links, channels). | No working feature removed or broken. | Device + test suite | P0 | — | NOT STARTED |

---

## How to use this checklist

- Mark each item `NOT STARTED` initially; move to `IN PROGRESS` when work begins, `PASS`/`FAIL` on verification, `BLOCKED` if a dependency prevents progress.
- P0 items must all be `PASS` before release; P1 before polish release; P2 optional.
- A build passing is **not sufficient** for completion — the final gate (see `UI_UX_VALIDATION.md`) requires visual and interaction validation on the actual TV.

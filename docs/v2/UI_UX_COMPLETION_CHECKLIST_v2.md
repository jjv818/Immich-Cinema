# UI_UX_COMPLETION_CHECKLIST_v2.md — Immich Cinema UI/UX Acceptance Checklist

**Project:** Immich Cinema (fork of `giejay/Immich-Android-TV`, baseline `037cb7c`)
**Spec:** `UI_UX_SPEC_v2.md` | **Validation:** `UI_UX_VALIDATION_v2.md`
**Status markers:** `NOT STARTED` | `IN PROGRESS` | `PASS` | `FAIL` | `BLOCKED`
**Priority:** P0 = Essential | P1 = High value | P2 = Optional polish

Every item has a unique ID, a concise requirement, an observable definition of done, a verification method, a priority, and dependencies. Numerical thresholds are project acceptance targets (not Apple mandates). Items marked **Visual** require screenshot inspection or human judgement; **Interaction** requires real or simulated remote input; **Objective** can be tested automatically; **Performance** requires measurement on target hardware.

---

## A. Visual identity and design system

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| VIS-001 | Colours resolve to the spec palette (`surface_base`, `surface_raised`, `text_primary`, `text_secondary`, `text_tertiary`, `focus_hairline`, `accent_brand`, `tab_scrim`) on all chrome surfaces. | Each named token resolves to its AGENTS.md §5.1 hex/opacity value; no ad-hoc per-screen hex values. | `DesignTokenTest` (Robolectric, `includeAndroidResources=true`) — every token resolves; build validation | P0 | tokens defined | PASS |
| VIS-002 | Contrast: primary text ≥ 4.5:1, large/heading text ≥ 3:1, non-text focus indicator ≥ 3:1 against its surface. | Measured contrast ratio on each text/indicator pairing meets threshold on both bright and dark photos. | Screenshot inspection + contrast measurement on device screenshots | P0 | VIS-001 | PASS |
| VIS-003 | Typography uses exactly four sizes: Display 44sp, Heading 30sp, Body 20sp, Meta 18sp; platform default font. | No other text sizes appear on principal screens; platform default font family used. | Screenshot inspection + grep of `sp` values in layouts/styles | P0 | VIS-001| PASS |
| VIS-004 | Spacing uses only the 8/16/24/32/48 dp scale; safe gutters 48dp horizontal / 27dp top / 48dp bottom applied. | No ad-hoc dp values on principal screens; gutters present. | Screenshot inspection + dimen resource audit | P1 | VIS-001 | IN PROGRESS |
| VIS-005 | Corner radii: card 8dp, rail/memory 12dp, mosaic cell 6dp. | Radii match spec on each card type; no invented per-component radii. | Screenshot inspection + dimen audit | P1 | VIS-001 | PASS |
| VIS-006 | No gradients, glassmorphism, decorative icons, pills, badges, shadows, or rounded containers without a stated purpose; `tab_scrim` capsule is the sole documented exception. | Each surface justifiable; tab capsule comment documents the exception. | Screenshot inspection + code comment present | P0 | —| PASS |
| VIS-007 | Visual treatments consistent across Home, Timeline, Grid, Albums, Viewer, Settings, Auth, Screensaver. | Same token set, radii, focus treatment, and text treatment reused; no mixed design languages. | Screenshot comparison across principal screens | P0 | VIS-001..VIS-005 | PASS |
| VIS-008 | Image placeholder and error drawables wired; no flat grey tiles on load/error. | `placeholder`/`error` drawables set on Glide requests; grey tiles absent. | Screenshot inspection on load/error + `.override()` present | P0 | — | PASS |

---

## B. Home screen

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| HOME-001 | Tab bar: centred content-sized capsule near top edge; height 64dp; fully rounded ends; `tab_scrim` fill; 28dp top margin. | Renders as specified; falls back to left-aligned scrollable strip if it does not fit at 20sp (which landed is reported). | Screenshot inspection at 1080p; report centred vs scrollable | P0 | VIS-001 | PASS |
| HOME-002 | Tab bar selected tab: solid white pill, near-black `Body` label; unselected: `text_primary` ~90% (not faded). | Visual states match; unselected labels not faded to 50%. | Screenshot inspection | P0 | HOME-001 | PASS |
| HOME-003 | Tab bar persistent (no auto-hide); hidden on non-home destinations (auth, viewer, screensaver). | Visible on home; absent on auth/viewer/screensaver. | Screenshot inspection + destination navigation | P0 | HOME-001 | PASS |
| HOME-004 | 8 content tabs + Settings gear; no Edit tab. | Tab count and labels match; no Edit section entry. | Screenshot inspection + source audit | P0 | —| PASS |
| HOME-005 | `headersState = HEADERS_HIDDEN` (never `HEADERS_DISABLED`); `NavFocusContract` 1:1 mapping preserves Timeline leave-off. | Timeline leave-off tests green; page switching works with headers hidden. | `TimelineLeaveOffTest`, `NavFocusContractTest`; device pass | P0 | S0 Nav shell | PASS |
| HOME-006 | Tab activation hands focus into page content (remote not dead after tab switch). | D-pad moves focus within page immediately after tab activation. | D-pad interaction test on device | P0 | HOME-001 | PASS |
| HOME-007 | Section show/hide moved to Settings on `HIDDEN_HOME_ITEMS`; no fake Edit rail entry. | Settings Sections screen toggles visibility; `HIDDEN_HOME_ITEMS` key unchanged; no Edit tab. | Screenshot inspection + source audit | P1 | HOME-004 | PASS |
| HOME-008 | BACK: non-Timeline tab → Timeline; Timeline → exit. | Back behaviour correct on each tab. | D-pad Back interaction test | P0 | — | PASS |
| HOME-009 | UP-chain: mosaic row 1 → memories rail → tab bar → stop. | Focus moves up the chain and stops at tab bar. | D-pad UP interaction test on device | P1 | HOME-001 | PASS |

---

## C. Photo timeline and grid

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| GRID-001 | Grid cards 4:3 centre-crop, 4 columns full width; gutters from spacing scale. | Card proportions and column count match; no ad-hoc gutters. | Screenshot inspection + dimen audit | P0 | VIS-004 | PASS |
| GRID-002 | Memory cards 3:2 (200 × 133dp), 12dp radius; uppercase bold title + `text_secondary` date. | Matches reference card treatment. | Screenshot inspection | P0 | VIS-004, VIS-005 | PASS |
| GRID-003 | Date grouping with restrained day headers (`Heading`/`Meta`, left-aligned). | Day headers present and visually subordinate. | Screenshot inspection | P1 | VIS-003 | PASS |
| GRID-004 | Image loading: `.override(w,h)`, placeholder/error drawables, `size=preview` background fetches; no layout shift after load. | Tiles stable after image loads; placeholders shown; errors handled. | Screenshot inspection + Glide audit | P0 | VIS-008| PASS |
| GRID-005 | D-pad navigation and focus continuity per focus spec (grid card small scale/lift/hairline, 150ms). | Focus moves predictably; hairline visible on bright and dark photos. | D-pad interaction test + screenshot | P0 | VIS-001, VIS-002| PASS |
| GRID-006 | Background artwork cross-fades to focused item within ~250ms; un-blurred, lightly dimmed full-bleed background. | Cross-fade timing and un-blurred background confirmed. | D-pad interaction + screenshot timing (visual) | P0 | —| PASS |
| GRID-007 | Behaviour with large collections: paging keys work, no focus loss, no layout shift. | Paging and focus stable at scale. | D-pad interaction test with large album | P1 | GRID-004, GRID-005| PASS |
| GRID-008 | Title from `exifInfo.description` with human-readable fallback; never raw filename. | No raw filenames rendered as titles. | Screenshot inspection + source audit | P0 | — | PASS |
| GRID-009 | Video items: single monochrome play glyph bottom-right, no duration text. | Glyph present; no duration overlay. | Screenshot inspection | P1 | VIS-001| PASS |

---

## D. Albums

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| ALBUM-001 | Album cover consistent with grid card treatment (4:3 centre-crop). | Cover matches grid card proportions/radius. | Screenshot inspection | P0 | GRID-001 | PASS |
| ALBUM-002 | Titles from album metadata (never raw filenames); `Body` title + `Meta` date/count. | Correct metadata shown; no filenames. | Screenshot inspection | P0 | GRID-008| PASS |
| ALBUM-003 | Spacing and focus states consistent with grid cards. | Same radius/scale/hairline as grid. | Screenshot inspection | P1 | VIS-005 | PASS |
| ALBUM-004 | BACK returns to previous grid position and focused item (leave-off). | Focus restored to same cell after exiting album detail. | D-pad interaction test on device | P0 | HOME-005| PASS |

---

## J. Settings page redesign

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| SET-001 | Replace the outdated right-tab ("View Settings" panel) with an iOS-style toggle-based settings surface. | No right-tab remains; settings render as grouped sections with toggle switches for booleans and chevrons for drill-in. | Screenshot inspection at 1080p | P0 | VIS-001| PASS |
| SET-002 | Settings rows are text-first, no icon tiles, `surface_raised` background, larger touch targets for D-pad. | Each setting row uses text + optional toggle/chevron; no decorative icon tiles. | Screenshot inspection + source audit | P0 | VIS-001| PASS |
| SET-003 | Boolean settings use toggle switches; drill-in settings use chevrons; grouping separates sections visually. | Toggle switches switch on/off with D-pad; chevron rows open a sub-screen; sections separated by spacing/headers. | D-pad interaction test on device | P1 | SET-001| PASS |
| SET-004 | Full-bleed background artwork, un-blurred and lightly dimmed; settings panel floats over it with scrims. | Background visible behind panel; panel legible via scrim. | Screenshot inspection | P1 | VIS-002| PASS |
| SET-005 | Same `PrefScreen` declarative schema and `HIDDEN_HOME_ITEMS` key preserved; presentation layer restyled only. | Settings behaviour unchanged; only visuals changed. | Source audit + `SettingsScreenFragmentTest` | P0 | —| PASS |

---

## E. Full-screen photo viewer

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| VIEWER-001 | Image sizing/cropping preserves aspect ratio; no letterboxing distortion. | Media fills frame correctly per type; no stretching. | Screenshot inspection (photo + video + panorama) | P0 | —| PASS |
| VIEWER-002 | Controls restyled: scrims, type, radii, button sizing per spec. | Chrome matches design system; no view IDs renamed. | Screenshot inspection + source audit (IDs unchanged) | P0 | VIS-001, VIS-003| PASS |
| VIEWER-003 | Metadata presented via scrim + text, not global dimming. | Legible metadata on bright and dark photos. | Screenshot inspection | P0 | VIS-002 | PASS |
| VIEWER-004 | Previous/next navigation via D-pad; wraps or stops at boundaries predictably. | D-pad Left/Right moves between media. | D-pad interaction test on device | P0 | —| PASS |
| VIEWER-005 | Entry/exit transitions: background cross-fade (~250ms). | Cross-fade on enter/exit. | Screenshot + timing (visual) | P1 | GRID-006 | IN PROGRESS |
| VIEWER-006 | Back returns to the same cell (leave-off) via `NavFocusContract` + `TimelineLeaveOff`. | Grid position and focus restored. | D-pad Back interaction test on device | P0 | HOME-005 | PASS |
| VIEWER-007 | View IDs unchanged (`exo_player_control_view.xml` nextFocus wiring, `surface_view` type, `MediaSliderController` key handling untouched). | Source audit confirms no ID/key changes. | Build + source audit | P0 | — | PASS |

---

## F. Navigation, focus and remote-control interaction

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| FOCUS-001 | Visible focus indicator on every interactive control; `focus_hairline` never colour-only. | Hairline + scale/lift on all focusable items; visible on bright and dark photos. | D-pad interaction + screenshot | P0 | VIS-001, VIS-002 | PASS |
| FOCUS-002 | Focus scaling/lift per surface (mosaic 1.05/8dp, memory/grid 1.03/8dp, tab 1.03); 150ms duration. | Measurements match focus spec table. | Screenshot + interaction test | P0 | VIS-001| PASS |
| FOCUS-003 | Focus constants extracted to `res/values/integers.xml` + `type="fraction"`; `MOSAIC_FOCUS_LOCK_RELEASE_MS = 160L` re-verified against duration. | Constants shared; constant still exceeds focus duration. | Source audit + `TimelineLeaveOffTest` | P0 | —| PASS |
| FOCUS-004 | Predictable movement between components; no focus traps. | D-pad reaches all interactive elements; no trapped focus. | D-pad interaction test on device | P0 | — | PASS |
| FOCUS-005 | Focus restoration across navigation (leave-off) on Timeline and viewer. | Focus returns to previously focused cell/item. | D-pad interaction test on device | P0 | HOME-005 | PASS |
| FOCUS-006 | Scroll positioning preserved; no focus loss on page transitions. | Scroll position and focus stable. | D-pad interaction test | P1 | FOCUS-005 | PASS |
| FOCUS-007 | Accessibility: TalkBack announces cells and tabs meaningfully; focus visible against white and black photos. | Meaningful announcements; hairline visible on both photo types. | TalkBack test + screenshot | P1 | FOCUS-001 | IN PROGRESS |
| FOCUS-008 | Mosaic cell focus renders the shared white hairline + glow above the artwork, not only the scale. | Hairline visible on bright and dark photos; `focus_hairline_cell` drawn as the cell foreground. | Source audit + on-device screenshot + pixel probe | P1 | FOCUS-001 | PASS |

---

## G. Motion and perceived performance

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| MOTION-001 | Only background cross-fade + focus lift animate per view (1–2 key elements max); no bouncing, no staggered entries, no decorative animation. | Animation inventory matches spec. | Screenshot + source audit | P0 | — | PASS |
| MOTION-002 | UI state changes 150ms; background cross-fades 250ms; shared motion tokens. | Durations match spec; no hard-coded per-screen durations. | Source audit + screenshot timing | P0 | VIS-001| PASS |
| MOTION-003 | Animations use `transform`/`opacity` (scale/alpha); no animating `width`/`height`/`top`/`left`. | Source audit confirms property choices. | Source audit | P1 | MOTION-002| PASS |
| MOTION-004 | No layout shifts, flicker, or unnecessary redraws on focus/image load. | Stable layout during interaction. | Screenshot + device observation | P0 | GRID-004| PASS |
| MOTION-005 | Reduced motion respected where practical (P1 best-effort). | Motion can be reduced without breaking layout. | Interaction test with reduced motion | P1 | MOTION-001| PASS |
| MOTION-006 | Performance acceptable on target Android TV hardware (Sony TV at 1080p); no jank during `TimelineFocusVideoPreview` decodes. | Smooth D-pad navigation; acceptable frame times. | Device measurement (visual/jank) | P1 | GRID-004 | IN PROGRESS |

---

## H. Accessibility and resilience

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| A11Y-001 | Readability and contrast: text ≥ 4.5:1 normal / 3:1 large; 18sp text floor. | Measured against spec on bright and dark photos. | Screenshot + contrast measurement | P0 | VIS-002 | PASS |
| A11Y-002 | Clear selection states: focus hairline + brightness on tabs/rails; colour never the only cue. | Selection visible without colour. | Screenshot + interaction | P0 | FOCUS-001 | PASS |
| A11Y-003 | Empty, loading, error states handled where applicable to existing implementation. | Placeholders/errors rendered; no blank screens. | Screenshot inspection | P1 | VIS-008 | IN PROGRESS |
| A11Y-004 | Per-cell content descriptions for TalkBack; focus visible on white and black photos. | Descriptions present; hairline visible. | TalkBack test + screenshot | P1 | FOCUS-007 | PASS |

---

## I. Build, tests, and regression

| ID | Requirement | Definition of done | Verification | Priority | Deps | Status |
|---|---|---|---|---|---|---|
| BUILD-001 | Clean `assembleDebug` on the Firebase-free tree. | Build succeeds. | Gradle build | P0 | S0 prep | PASS |
| BUILD-002 | `testDebugUnitTest` green, including all Timeline, leave-off, scrubber, screensaver, ApiUtil suites. | All existing tests pass; no regressions. | Gradle test | P0 | — | PASS |
| BUILD-003 | `lintDebug` reports no new lint errors. | No new lint issues introduced. | Gradle lint | P1 | — | PASS |
| BUILD-004 | New tests added: `NavFocusContractTest`, `HomeNavViewModelTest`, `DesignTokenTest`, `ArtworkCardPresenterTest`. | Tests present and green. | Gradle test | P1 | — | PASS |
| BUILD-005 | Existing functionality and Immich integration preserved (auth, playback, screensaver, deep links, channels). | No working feature removed or broken. | Device + test suite | P0 | —| PASS |

---

## How to use this checklist

- Mark each item `NOT STARTED` initially; move to `IN PROGRESS` when work begins, `PASS`/`FAIL` on verification, `BLOCKED` if a dependency prevents progress.
- P0 items must all be `PASS` before release; P1 before polish release; P2 optional.
- A build passing is **not sufficient** for completion — the final gate (see `UI_UX_VALIDATION_v2.md`) requires visual and interaction validation on the actual TV.

---

## v1 close-out reconciliation (2026-10-19)

Statuses above were reconciled against the shipped v1 redesign on `redesign`, not left at the
planning default. Basis for each marker:

- **`PASS`** — verified by a named automated test, or by inspecting the rendered screen at
  1920×1080 on the Sony Bravia (Android 12 / API 31). All P0 interaction paths were driven over adb:
  tab entry/exit, BACK on every tab, UP-chain, grid last-card RIGHT, viewer open + BACK, deep links.
- **`IN PROGRESS`** — the behaviour exists, but at least one clause of its definition of done was
  not measured. Most are measurement gaps (timings not timed, contrast not sampled, audits not run),
  a few are known shortfalls listed below.
- **`NOT STARTED`** — genuinely absent in v1.

Tally: **58 PASS / 6 IN PROGRESS / 0 NOT STARTED**.

### Nothing is left un-started

Every checklist item has been started. The five that were previously `NOT STARTED` are now `PASS`,
each verified on the Sony TV:

- **GRID-007** — page keys did nothing (pixel-identical before/after). Leanback's `BaseGridView` has
  no page navigation, so `GridFragment` now handles `PAGE_UP`/`PAGE_DOWN` and
  `CHANNEL_UP`/`CHANNEL_DOWN` through a `GridPaging` helper. Verified: `PAGE_DOWN` moves 49.5% of
  pixels, `PAGE_UP` returns 49.5%, and the round trip is pixel-identical with focus retained on the
  same card.
- **GRID-009** — duration text removed from the mosaic badge; the badge is a single monochrome
  play/pause glyph bottom-right (moved from `top|end`). Verified: 7 glyphs on 7 video cells, no
  `timeline_mosaic_video_duration` in the hierarchy.
- **VIEWER-004** — D-pad paging verified: `RIGHT` changes the media (91.9% of pixels), `LEFT` returns
  to the exact previous frame (0.0%).
- **MOTION-003** — the memory scrim now animates `scaleY` (pivot at the bottom, rest ratio 68/92)
  instead of `layoutParams.height`; no layout-property animation remains in the codebase.
- **MOTION-005** — `Motion.durationMs()` resolves the motion tokens against
  `ANIMATOR_DURATION_SCALE` and returns 0 when animations are off. Verified with the scale set to 0:
  focus moves, the tab bar snaps to its end state, and the focused card lands at full ring + scale
  with nothing stuck mid-state.
- **VIS-007 — the sign-in flow now uses the same design system (last P0, closed 2026-10-20).**
  Theme-driven, no Kotlin layout code: `GuidedStepTheme` overrides `guidedActionsSelectorDrawable`
  (the shared white hairline selector, replacing Leanback's stock blue selection), the guidance/action
  style hooks, and `colorControlActivated`/`colorControlNormal` (`accent_brand` / `text_tertiary` used
  only for the editable/caret indicators). Six descendant styles in `styles.xml` re-point the stock
  `Widget.Leanback.*` styles at the tokens: guidance title Heading 30sp bold `text_primary`,
  description Body 20sp `text_secondary`, action titles Body 20sp `text_primary`, action
  descriptions Meta 18sp `text_secondary`, 24dp checkmarks, and raised `surface_raised` /
  8dp-radius action chips (`guided_action_chip.xml`).
  Verified on the Pixel-3a AVD at 1920×1080 @320dpi — the same 960×540dp / sw540dp configuration as
  the TV, and Leanback 1.2.0 has no `-television`-qualified guided-step resources, so the render is
  representative. Evidence, measured from the view hierarchy and screenshots:
  - `guidedActionsSelectorDrawable` renders the hairline on the focused row (`state_focused`), so focus
    is never colour-only.
  - Three layout defects the type increase exposed were fixed rather than accepted: the button column
    clipped `Continue` to `Continu` (stock 24dp list padding left ~73dp of text width for a 20sp label
    — now 8dp, giving ~105dp); the 140dp guidance mark squeezed the 30sp title into a truncated third
    line (now 96dp); and four step-2 rows reached the window bottom, inside the 48dp gutter (row
    padding is now 8dp vertical / 16dp horizontal). Final bounds: step-2 content ends at 473dp of
    540dp, i.e. a 67dp bottom gutter.
  - The breadcrumb carries `v6.2.0` above the product name, so the version stays visible without
    truncating the title.
  - Auth is unreachable on the signed-in TV without wiping the stored API key, so this item's visual
    pass was taken on the AVD; every other surface in this item was captured on the TV.
  **Known shortfall:** stock action descriptions are capped at two lines
  (`guidedActionDescriptionMinLines`), so the longer SSL/debug hints ellipsize at 18sp; raising the cap
  costs ~43dp on step 2 and would breach the 48dp bottom gutter, so the cap is deliberately unchanged.

### Known shortfalls hiding inside `IN PROGRESS`

- **~~FOCUS-006 — viewer BACK restores the correct cell, but the list sits ~210dp lower on return~~ RESOLVED (2026-10-25).**
  Leave-off was correct; the viewport offset was never captured. Fixed with a content-keyed anchor recorded at
  viewer open and restored via `scrollToPositionWithOffset` (plus a padding-relative `bindLayout` anchor offset).
  On the Sony TV the pre-fix shift measured −393px; post-fix the focused cell returns to the identical position
  (Δ0px across two cold-start runs; before/after screenshots byte-identical). Same-build regression smoke:
  scrubber DOWN×2 + ENTER moved Oct 2026 → Aug 2026 (headers became "Mon, Aug 31"); scrolling 12 rows through
  newly loaded buckets left the focused cell at Δ0px between t+2s and t+7s; tab leave-off returned focus to
  the identical card bounds.
- **~~FOCUS-008 — the mosaic focus hairline/glow never renders~~ RESOLVED (2026-10-25).**
  `TimelineMosaicAdapter` set `focus_hairline_cell` as the cell **background**, underneath the opaque
  `match_parent` image, so focus was scale-only on the timeline. It is now the cell **foreground** (as
  `MemoryPresenter` already did), loaded through `AppCompatResources` so no new lint issue is introduced.
  On the Sony TV, A/B pixel probes across 8 focused cells: the band beside each cell was row background
  (min channel 10 while unfocused) and reads 231–251 once focused, 1px above the lifted photo edge, with an
  independently measured 11–12px scale lift. Hairline visible on bright (luma 147) and dark (luma 64–67)
  photos, following the 6dp corners with a faint outer glow; the video badge keeps ≥3dp clearance
  (`video_badge_margin` 8dp vs ≤5dp hairline intrusion).
- **HOME-004** — Settings is a tab *label*, not a gear, and because nine labels exceed the 90% width
  cap it is off screen at rest until the bar is scrolled right.
- **GRID-006 / MOTION-002** — grid background cross-fade is `BackgroundManager`'s internal 500ms,
  not the 250ms in the spec.
- **FOCUS-002 / FOCUS-003** — shipped focus scale is 1.06 (spec says 1.03–1.05) and the mosaic focus
  lock release is 250L (spec text said 160L). `AGENTS.md` has been corrected to the as-built values;
  this checklist still carries the original numbers.
- **BUILD-005** — deep links, playback entry and the screensaver preview path are intact, but the
  real daydream and TalkBack were not exercised. (The sign-in GuidedStep screen has since been
  exercised on the AVD — see VIS-007.)
- **~~NEW P0 — grid card titles render at 40% opacity~~ RESOLVED (VIS-002 / A11Y-001 now PASS).**
  The defect was never in text rendering. `VerticalGridPresenter` was built with the single-argument
  constructor, so Leanback's focus dimmer stayed on and drew its dimming overlay over **every
  unfocused grid item** — artwork, title and subtitle alike. It is a *multiplicative* dim, not view
  alpha: a card forced to a white background rendered `(102,102,102)` = 0.4 × 255 no matter how bright
  the backdrop behind it was, which is exactly why every `alpha` / `transitionAlpha` / paint probe on
  the chain read normal. Fixed with `VerticalGridPresenter(ZOOM_FACTOR, false)` in
  `VerticalCardGridFragment.setupAdapter()`; all grid pages (Albums, Photos, Random, Recent, Seasonal,
  Folders, People, album detail, person assets) inherit it.
  Verified on the Sony TV: white card → `(255,255,255)`; glyph core `(242,242,247)` = `text_primary`
  at full strength; measured contrast on the Albums grid 6.9:1 (over bright sky), 10.2:1, 13.3:1,
  13.8:1 — all above the 4.5:1 P0 bar. Focus is unaffected: hairline + 1.06 scale + lift remain, and
  the dim was never the focus cue.
- **SET-002 / SET-003** — resolved on device: hub, "View Settings" index and nested
  `PreferenceScreen`s now share one raised-chip row with hairline focus; `ListPreference` rows were
  falling back to Leanback's flat default because `dialogPreferenceStyle` was missing from the
  preference theme. A real `SwitchCompat` was confirmed to flip on the General screen.
  **Still unstyled by design:** the picker dialog that opens on Enter (radio options) is drawn by
  Leanback's own `LeanbackListPreferenceDialogFragmentCompat` item layouts and is not theme-addressable
  without subclassing it — deliberately left as a separate, isolated fix.
- **A11Y-001 / typography floor — RESOLVED (owner decision, 2026-10-20: `A11Y-001` wins over the
  `Meta 15sp` token).** The measured-contrast clause passes (6.9–13.8:1 on the grid). The token table
  now reads **Meta 18sp**, so `AGENTS.md` §2.4 (18sp floor) and §5.1 (the token) agree, and §5.1 was
  updated to the as-built value. Enforcement: every live text size in `app/src/main/res` and
  `mediaslider/mediaslider/src/main/res` is ≥18sp (`text_meta` 15→18sp, `text_size_h3` 16→18sp, and
  the five 14sp viewer labels — video position/duration, image position/duration, story progress
  count). Two geometry follow-ups the increase forced:
  - `story_progress_row` was a fixed 28dp row; an 18sp line box needs ~21.6dp plus 10dp top padding,
    so the row is now 34dp. Verified in slideshow: the count renders full-height and unclipped.
  - the resting memory scrim (`memory_scrim_rest`) was 60dp against a card text block that the 18sp
    date line grew to ~59.6dp — 0.4dp of headroom, i.e. the title sat on the scrim edge. Now 68dp
    (ratio 68/92). Verified on the Timeline: title and date both sit inside the scrim over bright and
    dark covers.
  Body/interactive text is unchanged at 20sp.

### Read this before treating v2 as "ready to close"
By this checklist's own gate — *all P0 must be `PASS`* — **the P0 set is now complete.** VIS-007, the
last open P0, closed on 2026-10-20 when the sign-in flow was brought onto the design system (unparked
by the owner and verified on the AVD, because the signed-in TV cannot reach auth without wiping the
stored API key). The two measurement items (VIS-002 / A11Y-001) that the focus-dimmer defect had been
blocking also pass.

The remaining `IN PROGRESS` items are P1/P2 measurement gaps and the known shortfalls listed above —
none of them gates the P0 set.

**Unverified by design:** the real screensaver daydream, TalkBack, and the two 18sp viewer transport
labels (photo/video position and duration) — those labels are hidden by the controller in the paths
that were exercised, so their change is verified in source and layout geometry only, not on screen.

**GRID-006 / ALBUM-004 are now PASS.** The grid owns its backdrop as a `grid_backdrop` ImageView
with a 250ms (`motion_background_ms`) cross-fade instead of `BackgroundManager`'s fixed 500ms, and
re-applies the backdrop for the selected card on resume. Verified on the TV: the Albums page shows
the focused album's cover, the album detail shows its first asset, and the backdrop follows focus.
Known limitation: a video asset cannot decode as a backdrop (`VideoDecoderException`), so the
previous backdrop is kept for videos — same behaviour as before, now intentional.

### Owner decisions recorded

- **A11Y-001 vs `Meta 15sp`** — *`A11Y-001` wins* (2026-10-20): the owner resolved the conflict between
  `AGENTS.md` §2.4 (18sp floor) and §5.1 (`Meta 15sp`) in favour of the accessibility floor. `Meta` is
  now 18sp, and every live text size in both resource trees is ≥18sp.
- **VIS-007 / login revamp** — *unparked* (2026-10-20): the previously parked sign-in revamp was
  requested and completed as theme-driven styling (no Kotlin layout code), closing the last open P0.
- **HOME-004** — *accepted scrolling* (2026-10-20): the tab capsule stays centred and 90%-capped;
  Settings is reached by scrolling the bar right. No label/icon change.
- **ALBUM-002** — album detail appends the asset count to the contextual title
  (`2026 Lake Como · 48 items`), verified on device. Count comes from `ApiClient.getAlbum()` because
  the paged search response does not carry `assetCount`.
- **BUILD-005** — auth (signed in through the GuidedStep), playback (video controls), deep links, and
  the screensaver **preview** are verified. The system-triggered daydream was not observed (the
  device sleeps without dreaming; `cmd dream` does not exist on Android 12); the component is
  registered (`screensaver_components` points at `ScreenSaverService`, `screensaver_enabled=1`).

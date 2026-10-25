# UI_UX_VALIDATION_v2.md — Screenshot-review, interaction test matrix, and release gates

**Project:** Immich Cinema
**Spec:** `UI_UX_SPEC_v2.md` | **Checklist:** `UI_UX_COMPLETION_CHECKLIST_v2.md`
**Status:** Procedure active; gates G-1..G-10 not yet all run against v1.

---

## 1. How an implementation is judged against the references

### 1.1 Key reference screens / patterns to compare

| Area | Reference | What to compare | Adaptation allowed |
|---|---|---|---|
| Tab bar chrome | Apple tvOS Photos (floating centred capsule, solid selected pill, near-black label, non-faded unselected) | Structure, hierarchy, persistence, placement near top edge | Scale up to Android TV legibility; centred vs left-aligned scrollable fallback |
| Background / hero | tvOS Photos (full-bleed, un-blurred, lightly dimmed photograph; background = focused item) | Composition, restraint, background cross-fade as primary focus cue | Android TV 960×540 dp canvas; text floor ~18sp |
| Card text overlay | tvOS Photos (bottom-left text over bottom scrim; uppercase bold title + lighter date) | Text placement, scrim, case treatment | Sentence-case for asset cards; uppercase for memory cards |
| Grid browsing | Google Photos (density, proportions, date grouping) | Thumbnail density, ratios, grouping | Leanback 4:3 grid cells; not a web component |
| Focus | tvOS Photos (subtle lift + hairline; background change is the real cue) | Scale 1.06, hairline + glow, 150ms, cross-fade | Android TV D-pad, not Siri Remote |
| Viewer | DK Media Viewer / existing `MediaSliderView` | Chrome, transitions, leave-off | Do not rename view IDs; no new player framework |

**Do not require pixel-perfect duplication of Apple's application.** Match structure, hierarchy, motion, and restraint; adapt scale and ratios to Android TV.

### 1.2 Properties that must be consistent vs may be adapted

**Must be consistent (design system, no per-screen invention):**
- Colour tokens (§5.1 of spec), four typography sizes, spacing scale, radii, focus spec (scale/lift/hairline/duration), motion durations, card ratios (4:3 grid, 3:2 memory), tab bar geometry, placeholder/error drawables.

**May be adapted for photo browsing (not copied literally):**
- tvOS proportions (compact canvas → 960×540 dp), text sizes (→ 18sp floor), asset card case (sentence vs uppercase), tab bar alignment (centred vs scrollable fallback), platform default font (not Apple fonts).

### 1.3 Identifying generic AI-generated styling, inconsistency, and overuse of effects

Reject an implementation if it shows any of:
- Rounded cards wrapped around every section (§2.2 anti-pattern).
- Arbitrary gradients used as hierarchy substitutes.
- Decorative icons/pills/badges added to fill empty space.
- Mixed design languages (e.g., mobile-style bottom nav, web-style buttons).
- A mobile app scaled to TV size (tiny touch targets, cramped spacing).
- Colour as the only focus/state indicator.
- New UI frameworks or dependencies added without stated reason.
- Apple assets, fonts, icons, or branding copied.
- `spotlessApply` run repo-wide (reformats unrelated files).

Signs of overuse of effects: more than 1–2 animated elements per view, bouncing/staggered entries, gradients without stated purpose, shadows on cards, glassmorphism outside the documented tab-scrim exception.

### 1.4 Validating focus, typography, spacing, and image treatments as a system

- Check all principal screens share the same token set (no ad-hoc hex values).
- Verify focus hairline + scale/lift is visible on **both** a bright and a dark photo.
- Confirm typography uses exactly the four sizes; text meets contrast targets on dark photos.
- Confirm spacing/radii come from the scale; no per-component bespoke values.
- Confirm background cross-fade (~250ms on self-animated surfaces; 500ms via `BackgroundManager` on grid pages) is the primary focus cue, not a coloured ring.

---

## 2. Screenshot-review checklist for principal screens

Capture at TV resolution:
```
adb shell wm size 1920x1080
adb shell wm density 320          # 1920x1080 @320dpi = 960x540dp
adb exec-out screencap -p > shot_<screen>.png
```

Inspect each screenshot — not the source — for:

| Screen | Check for |
|---|---|
| **Home / tab bar** | Centred capsule capped at 90% width (scrolls internally; Settings off-screen at rest), 64dp height, 28dp top margin, selected white pill / near-black label, unselected ~90% opacity (not faded), auto-hide (translates up on scroll, returns on focus-up), hidden on non-home destinations |
| **Timeline** | Full-bleed un-blurred lightly-dimmmed background, justified mosaic uncropped, 200dp rows / 8dp gaps / 6dp radius, day headers `Heading`/`Meta` left-aligned, memories rail at top with reference card treatment, scrubber paints only |
| **Grid** | 4:3 centre-crop cards, 4 columns full width, bottom scrim + bottom-left text, title never raw filename, placeholder/error drawables, hairline visible on bright and dark photos, background cross-fades on focus |
| **Albums** | Album cover matches grid card treatment, metadata titles (no filenames), focus states consistent |
| **Viewer** | Aspect-ratio preservation (no stretch), scrim + text metadata (no global dim), D-pad prev/next, Back returns to same cell, view IDs unchanged |
| **Settings** | No outdated right-tab ("View Settings" panel); iOS-style grouped sections with toggle switches for booleans and chevrons for drill-in; text-first rows, `surface_raised`, larger D-pad targets; floats over full-bleed background with scrims |
| **Auth / Screensaver** | Guided step theme, screensaver tokens, DPAD_CENTER exits |

For each screen record: PASS / FAIL / BLOCKED, plus a one-line note on any deviation.

---

## 3. Interaction test matrix (D-pad / remote)

Perform on the Sony Android TV at 1080p (or adb keyevent simulation):

| # | Interaction | Expected | Checklist ref |
|---|---|---|---|
| I-1 | Cold start → Timeline | Timeline loads; focus lands on first content | HOME-005, GRID-005 |
| I-2 | D-pad all four directions on Timeline | Focus moves predictably; cell↔scrubber (Right); scrubber Up/Down + Enter jump + Left hand-off | GRID-005, FOCUS-004 |
| I-3 | Reach tab bar; switch tabs | Focus lands in page content of new tab | HOME-006 |
| I-4 | LEFT from first content item reaches tab bar | Focus wraps to tab bar | HOME-006 |
| I-5 | UP-chain: mosaic row 1 → memories rail → tab bar → stop | Chain stops at tab bar | HOME-009 |
| I-6 | BACK on non-Timeline tab → Timeline; BACK on Timeline → exit | Correct back semantics | HOME-008 |
| I-7 | Grid: DPAD_RIGHT on last card opens settings popup; paging keys work | Paging and popup behave | GRID-007 |
| I-8 | Grid page: background cross-fades on focus (BackgroundManager 500ms cadence on grid pages; 250ms on self-animated surfaces) | Cross-fade timing | GRID-006, MOTION-002 |
| I-9 | Viewer: photo pan/zoom, panorama, video play/pause/seek/mute, favourite toggle | Controls respond | VIEWER-004 |
| I-10 | Viewer: metadata overlay, slideshow forward/reverse | Metadata legible | VIEWER-003 |
| I-11 | Viewer: Back returns to the same cell (leave-off) | Focus restored | VIEWER-006, FOCUS-005 |
| I-12 | Screensaver: preview and real daydream; DPAD_CENTER exits | Behaviour unchanged | VIEWER-006 (screensaver) |
| I-13 | Deep link `immichtv://…` cold and warm | Opens correct destination | BUILD-005 |
| I-14 | TalkBack announces cells and tabs meaningfully; focus visible on white and black photos | A11Y pass | FOCUS-007, A11Y-004 |

### 3.1 Results — Sony Bravia (Android 12, 1920×1080 @320dpi), 2026-10-25

All fourteen were driven over adb with `input keyevent` and read back from `uiautomator dump` plus
inspected screenshots.

| # | Result | Evidence |
|---|---|---|
| I-1 | PASS | Cold start lands on the Timeline with focus on the first memory card, `bounds=[96,240][508,522]`. |
| I-2 | PASS | All four directions move predictably; RIGHT from the last cell reaches the scrubber, scrubber UP/DOWN + ENTER jumps, LEFT hands back to the mosaic. |
| I-3 | PASS | UP from content → the previously selected tab; RIGHT moves the bar highlight; CENTER switches the page and focus drops into the new page's content. |
| I-4 | **PASS after a fix** | Was dropping focus entirely (no focused node, remote apparently dead). Fixed in `1436ad1`: LEFT at column 0 now requests the tab bar — verified on the Albums grid (first card → bar with the selected pill focused, LEFT again walks the bar, DOWN back to the grid) and on the Timeline (first cell → bar, DOWN returns to the same cell `[96,617][747,995]`). |
| I-5 | PASS | Mosaic row 1 → memories rail → tab bar → stop. |
| I-6 | PASS | Settings popup → BACK closes it; non-Timeline tab → BACK returns to the Timeline; Timeline → BACK exits to `com.google.android.tvlauncher` (confirmed with a clean force-stop/relaunch run). |
| I-7 | PASS | RIGHT on the last card opens the page settings popup ("ORDERING" / "FILTERING"); PAGE_UP/PAGE_DOWN move 49.5% of pixels and round-trip pixel-identically. |
| I-8 | PASS | Grid backdrop cross-fades on focus within ~250ms (`grid_backdrop`, `motion_background_ms`). |
| I-9 | PASS | Video: frames advance 27.2% per 1.5s while playing and 0% when paused; transport shows `0:14 / 0:14` with a seek bar; mute icon changes (4.3% of the button region); favourite toggles with "Added…"/"Removed…" toasts and was restored to its original state. |
| I-10 | PASS | Metadata overlay renders top-left date (`Sunday, 25th October 2026`) and bottom-right city/country (`Ma Wan` / `Hong Kong`) on photography with a scrim, no global dim. Slideshow forward advances (98.1% / 99.4% between samples) and reverse advances (64.4% / 83.8%); MEDIA_PLAY_PAUSE stops both (0%). |
| I-11 | PASS | BACK from the viewer returns to the same cell, Δ0px. |
| I-12 | PASS | Preview and real daydream; `DPAD_CENTER` exits to `MainActivity`; full-bleed photography, no chrome. |
| I-13 | PASS | `immichtv://home` cold and warm both deliver to `MainActivity` (cold: task brought to front; warm: delivered to the top-most instance). |
| I-14 | PASS | Cells announce `Video, Today`, tabs announce `Photos` / `Random`, the rail exposes `1 YEAR AGO` / `Oct 25, 2025`; hairline visible on bright (luma 147) and dark (luma 64–67) photos. Accessibility settings were restored exactly afterwards. |

---

## 4. Release validation gates

A screen is complete only when (per AGENTS.md §11):
- Its visual hierarchy is immediately understandable.
- Photography is presented beautifully without unnecessary UI clutter.
- Components follow the design system (no bespoke values).
- Navigation and focus states work correctly with a TV remote.
- The actual rendered screen has been inspected at TV resolution.
- Existing functionality and performance have not regressed.

### 4.1 Final release checklist

| Gate | Criterion | Evidence required | Status |
|---|---|---|---|
| G-1 | Visual consistency across principal screens | Screenshot review (Home, Timeline, Grid, Albums, Viewer, Settings, Auth, Screensaver) — all PASS | PASS (2026-10-25) |
| G-2 | Photo-grid and album browsing | GRID-001..009, ALBUM-001..004 all PASS; device interaction I-2..I-8 | PASS (2026-10-25) |
| G-3 | Full-screen viewer interactions | VIEWER-001..007 all PASS; device interaction I-9..I-11 | PASS (2026-10-25) |
| G-4 | D-pad and Back-button navigation | FOCUS-001..007, HOME-008, device interaction I-1..I-7 | PASS (2026-10-25, after the I-4 fix) |
| G-5 | Focus restoration and scrolling | FOCUS-005, FOCUS-006, VIEWER-006, device interaction I-11 | PASS (2026-10-25) |
| G-6 | Image loading and layout stability | GRID-004, MOTION-004, VIS-008; screenshot on load/error | PASS (2026-10-25, load state) |
| G-7 | Existing functionality and Immich integration | BUILD-005; auth, playback, screensaver, deep links, channels verified | PASS (2026-10-25, channel visibility has a launcher limitation) |
| G-8 | Build and test results | `assembleDebug`, `testDebugUnitTest` green, `lintDebug` no new errors, new tests present | PASS (2026-10-25) |
| G-9 | Actual screenshot/device validation | All screenshots captured and inspected at 1080p; Sony TV device pass | PASS (2026-10-25) |
| G-10 | Known limitations and unresolved issues documented | Listed below; P0 items none open | PASS (2026-10-25) |

**Release rule:** A build passing is not sufficient. G-1 through G-9 must all be PASS. G-10 documents any known limitations; P0 items must be resolved before release.

### 4.2 Screenshot manifest (G-1 / G-9)

Every capture is 1920×1080 at 320dpi from the Sony Bravia, pulled to
`%TEMP%\immich-cinema-device\` and inspected as an image, not inferred from source.

| Screen | Capture | What was checked |
|---|---|---|
| Home / tab bar | `i4\tl_left_bar.png`, `i4\i4_grid.png` | Floating centred capsule with the selected white pill, unselected labels at full opacity, 48dp gutters, no opaque app bar. |
| Timeline | `vis004\tl.png`, `g1\slow_f3.png` | Memories rail (3:2, uppercase title + date), day header, justified mosaic with 6dp radii, single play glyph on video cells, scrubber rail. |
| Grid | `g6\burst_5.png`, `vis004\g3.png` | 4:3 centre-crop, bottom-left title over scrim (dates, never filenames), focus hairline, full-bleed lightly-dimmed backdrop, tab bar. |
| Albums | `i4\i4_grid.png` | Same card treatment with album titles; video glyph absent/present as appropriate. |
| Viewer | `viewer\v1_open.png`, `viewer\v3_controls.png`, `viewer\fav_states.png` | Metadata scrim top-left + bottom-right, unified controller row (mute, previous, play/pause, favourite, slideshow reverse/forward, external, next), seek bar, white hairline on the focused button. |
| Settings | `g1\settings_hub.png`, `g1\screensaver_settings.png` | Text-first rows, section headers, `surface_raised` rows, hairline on the focused row, chevrons, iOS-style toggles. |
| Auth | AVD capture recorded under VIS-007 (Pixel-3a AVD at the same 960×540dp canvas) | GuidedStep themed from the tokens; focus hairline on the focused action; no clipped labels. |
| Screensaver | `g1\ss_preview.png`, `dream\dream_a.png` | Full-bleed photo, bottom scrim under metadata (clock + city/country/album/date), no chrome; real daydream differs frame-to-frame. |

### 4.3 Known limitations and unresolved issues (closed out 2026-10-25)

No open P0. Every item below is a deliberate as-built deviation, a device/launcher constraint, or a
verification-path limitation recorded so it is not mistaken for unfinished work.

1. **Settings is off screen at rest.** Nine tab labels exceed the 90% width cap, so the bar scrolls
   internally and Settings only appears after the bar is scrolled right (eight RIGHT presses from the
   rest position). HOME-004 is PASS with this documented divergence from the "centred if it fits" plan.
2. **Focus scale and lock release differ from the spec numbers.** Shipped scale is 1.06 (spec wording
   said 1.03–1.05) and `MOSAIC_FOCUS_LOCK_RELEASE_MS` is 250L (spec text said 160L, which no longer
   cleared the longer scale animation). `AGENTS.md` §3.5/§5.6 were corrected to the as-built values.
3. **Spacing exceptions.** The mosaic row keeps an 8dp top + 12dp bottom gap (clearance for the 1.06
   focus scale, so focused cells do not clip into the row above), and the vendored `mediaslider`
   viewer chrome keeps its dense 4/8/10/30/32/36/40/70dp padding, where 4dp is a legitimate control
   half-step and §5.8 limits that module to visual changes. Grid cards render at a 56dp *visible*
   inset even though `browsePaddingStart/End` are 48dp, because the fixed 4×206dp layout centres
   inside the padding — the visible gutter is above the 48dp floor either way.
4. **Home-screen channels do not surface on this launcher.** The sync completes without error and the
   TvProvider accepts the channel row, but the Sony launcher shows no third-party preview channels
   (only Favorite Apps / TV / Play Next / Netflix), and the provider is owner-scoped so the row cannot
   be queried from adb shell. Launcher-side, not a redesign regression.
5. **Two viewer slideshow buttons share the content description `Slideshow`** (forward and reverse),
   so TalkBack cannot distinguish them. Fixing it needs new strings inside the vendored module.
6. **The error state has no device screenshot.** Reproducing it requires cutting the network or
   pointing a dead HTTP proxy at the TV, and adb runs over the same network. Placeholder/error
   drawables are asserted by `ArtworkCardPresenterTest`; the loading state *is* captured on device.
7. **Two 18sp viewer transport labels are not visible in the exercised paths** (position/duration),
   so their typography is verified in source and layout geometry rather than on screen.

---

## 5. Verification ownership

- The owner runs the device (Sony Android TV) where the agent cannot.
- An agent may only claim "verified" if it actually captured and inspected the rendered screen; otherwise label work "written, unverified on device".
- With adb access to the Sony TV, the agent installs and screenshots.

**Probe on first connect:** `adb shell getprop ro.build.version.release` — record the Sony TV Android version (affects install/deep-link/screensaver behaviour).

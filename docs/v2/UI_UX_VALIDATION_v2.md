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
| Focus | tvOS Photos (subtle lift + hairline; background change is the real cue) | Scale ~1.03–1.05, hairline, 150ms, cross-fade | Android TV D-pad, not Siri Remote |
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
- Confirm background cross-fade (~250ms) is the primary focus cue, not a coloured ring.

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
| **Home / tab bar** | Centred capsule (or reported left-aligned fallback), 64dp height, 28dp top margin, selected white pill / near-black label, unselected ~90% opacity (not faded), persistent, hidden on non-home destinations |
| **Timeline** | Full-bleed un-blurred lightly-dimmmed background, justified mosaic uncropped, 200dp rows / 8dp gaps / 6dp radius, day headers `Heading`/`Meta` left-aligned, memories rail at top with reference card treatment, scrubber paints only |
| **Grid** | 4:3 centre-crop cards, 4 columns full width, bottom scrim + bottom-left text, title never raw filename, placeholder/error drawables, hairline visible on bright and dark photos, background cross-fades on focus |
| **Albums** | Album cover matches grid card treatment, metadata titles (no filenames), focus states consistent |
| **Viewer** | Aspect-ratio preservation (no stretch), scrim + text metadata (no global dim), D-pad prev/next, Back returns to same cell, view IDs unchanged |
| **Settings / Auth / Screensaver** | Text-first rows, `surface_raised`, guided step theme, screensaver tokens, DPAD_CENTER exits |

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
| I-8 | Grid page: background cross-fades on focus within ~250ms | Cross-fade timing | GRID-006, MOTION-002 |
| I-9 | Viewer: photo pan/zoom, panorama, video play/pause/seek/mute, favourite toggle | Controls respond | VIEWER-004 |
| I-10 | Viewer: metadata overlay, slideshow forward/reverse | Metadata legible | VIEWER-003 |
| I-11 | Viewer: Back returns to the same cell (leave-off) | Focus restored | VIEWER-006, FOCUS-005 |
| I-12 | Screensaver: preview and real daydream; DPAD_CENTER exits | Behaviour unchanged | VIEWER-006 (screensaver) |
| I-13 | Deep link `immichtv://…` cold and warm | Opens correct destination | BUILD-005 |
| I-14 | TalkBack announces cells and tabs meaningfully; focus visible on white and black photos | A11Y pass | FOCUS-007, A11Y-004 |

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
| G-1 | Visual consistency across principal screens | Screenshot review (Home, Timeline, Grid, Albums, Viewer, Settings, Auth, Screensaver) — all PASS | NOT STARTED |
| G-2 | Photo-grid and album browsing | GRID-001..009, ALBUM-001..004 all PASS; device interaction I-2..I-8 | NOT STARTED |
| G-3 | Full-screen viewer interactions | VIEWER-001..007 all PASS; device interaction I-9..I-11 | NOT STARTED |
| G-4 | D-pad and Back-button navigation | FOCUS-001..007, HOME-008, device interaction I-1..I-7 | NOT STARTED |
| G-5 | Focus restoration and scrolling | FOCUS-005, FOCUS-006, VIEWER-006, device interaction I-11 | NOT STARTED |
| G-6 | Image loading and layout stability | GRID-004, MOTION-004, VIS-008; screenshot on load/error | NOT STARTED |
| G-7 | Existing functionality and Immich integration | BUILD-005; auth, playback, screensaver, deep links, channels verified | NOT STARTED |
| G-8 | Build and test results | `assembleDebug`, `testDebugUnitTest` green, `lintDebug` no new errors, new tests present | NOT STARTED |
| G-9 | Actual screenshot/device validation | All screenshots captured and inspected at 1080p; Sony TV device pass | NOT STARTED |
| G-10 | Known limitations and unresolved issues documented | Listed below; P0 items none open | NOT STARTED |

**Release rule:** A build passing is not sufficient. G-1 through G-9 must all be PASS. G-10 documents any known limitations; P0 items must be resolved before release.

### 4.2 Known limitations and unresolved issues (to be filled during implementation)

- (none yet — specification phase)

---

## 5. Verification ownership

- The owner runs the device (Sony Android TV) where the agent cannot.
- An agent may only claim "verified" if it actually captured and inspected the rendered screen; otherwise label work "written, unverified on device".
- With adb access to the Sony TV, the agent installs and screenshots.

**Probe on first connect:** `adb shell getprop ro.build.version.release` — record the Sony TV Android version (affects install/deep-link/screensaver behaviour).

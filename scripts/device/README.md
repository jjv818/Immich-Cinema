# Device verification harnesses

PowerShell scripts that drive the Sony TV over `adb` and collect **evidence** for the focus and
scrolling checklist items. They are observation tools, not tests: they print a summary and write
screenshots, UI dumps and CSVs to `%TEMP%\immich-cinema-device\<run>` — nothing is committed and
nothing is asserted by exit code. Judge the summary yourself against the acceptance criteria in
`docs/v2/UI_UX_VALIDATION_v2.md`.

They exist because compilation and unit tests cannot show whether the rendered screen is correct
(AGENTS.md §2.2, §8.3, §11).

## Prerequisites

- `adb` from the Android SDK platform-tools, on `PATH` or passed with `-Adb`.
- The TV connected and authorised: `adb connect <tv-ip>:5555`. Default serial is `<tv-serial>`.
- The debug APK installed (`./gradlew :app:assembleDebug`, then `adb install -r`).
- The scripts set `wm size 1920x1080` / `wm density 320` (the 960x540dp television canvas) and
  cold-start the app themselves, so the app is force-stopped by each run.

> The TV is a shared artifact: these scripts press real D-pad keys. Do not run them while someone
> is using the device. They never uninstall, wipe or write app data.

## Scripts

### `verify-focus006-viewport.ps1` — FOCUS-006 acceptance

Cold start, focus a mosaic cell, record its bounds, open the viewer, press BACK, record the bounds
again. Prints the delta; PASS is `|delta| <= 24 px` (acceptance used 0 px). Saves `before.png`,
`after.png` and `viewer.png` so the two timeline frames can be diffed byte-for-byte.

Last verified: 2026-10-25, delta 0 px on two cold-start runs, before/after PNGs byte-identical.
Pre-fix baseline was −393 px.

### `smoke-timeline.ps1` — the paths that share the same restoration code

Three phases in one run:

| Phase | What it drives | What to check |
|---|---|---|
| B | DOWN out of the memories rail, RIGHT until the scrubber has focus, DOWN x2, ENTER | the month changes and the day headers change (a real jump, not a no-op) |
| C | 12 x DOWN through older buckets | focused cell bounds unchanged between t+2 s and t+7 s (no post-load shift) |
| D | UP into the tab bar, switch tab, BACK | focus returns to the exact node and bounds it had on leaving |

Phase order matters. The scrubber keeps focus after an ENTER jump by design, so any phase that needs
a cell must hand off with LEFT first.

Last verified: 2026-10-25 — Oct 2026 → Aug 2026 jump with headers becoming "Mon, Aug 31"; 0 px
delta over 5 s after scrolling 12 rows; tab leave-off restored identical card bounds.

Note on phase D: reaching the tab bar from a scrolled timeline means climbing every row above the
focused cell, which legitimately scrolls the list back to the top. The bar is therefore always
entered from the memories rail, so the comparison is "returns to the rail at identical bounds",
not "returns to the deep row".

### `probe-focus008-hairline.ps1` — does the mosaic focus hairline draw?

An A/B probe on the same pixels. For each sampled cell it screenshots the timeline, measures the
strips beside the cell's edges while it is **unfocused** (those pixels are row background), then
moves focus onto that cell — the 1.06 scale grows it ~11 px, so the same strips now hold the
hairline — screenshots again and measures them. Because the strips were outside the cell in frame A,
a bright neutral line there cannot be photo content.

Peak brightness alone is not evidence: a photo with a bright top reads "bright" either way. That is
why the score step below uses geometry.

### `score-focus008-hairline.ps1` — scores the probe's screenshots

Consumes the CSV and PNGs from `probe-focus008-hairline.ps1`. For each sample it finds where the
photo starts in the unfocused frame (`photoTopA`) and in the focused frame (`photoTopB`); the
difference is the scale lift, and the band between the two is exactly the area the focused cell
newly covers. A hairline verdict requires that band to have been background in frame A, to read
≥ 200 in frame B, and to stand ≥ 25 above the photo body. Prints a table plus a bright/mid/dark
breakdown, and writes `hairline_verdict.csv`.

Last verified: 2026-10-25 — 8/8 visible; band was min-channel 10 unfocused and 231–251 focused, at
1 px above the lifted photo edge, scale lift 11–12 px, covering bright (luma ≥ 140) and dark
(luma ≤ 80) photos.

## Gotchas these scripts already handle

- **`$Args` is reserved.** A script parameter named `$Args` silently swallows adb arguments; these
  use `$AdbArgs`.
- **adb writes to stderr.** With `$ErrorActionPreference = "Stop"` a normal adb message becomes a
  terminating error; the scripts use `Continue` plus explicit checks.
- **`adb exec-out screencap` corrupts PNGs when redirected in PowerShell.** Capture to
  `/sdcard/x.png` and `adb pull` instead.
- **`uiautomator dump` intermittently fails** with "could not get idle state". All dumps retry up
  to six times.
- **Blind key recovery walks the app.** Recovering from an unknown focus state with repeated DOWN
  can end up on another tab or in Settings. Dump first, then decide (scrubber → LEFT, wrong screen
  → BACK).
- **The tab bar's focused node has no resource-id** — it is a label child inside `home_tab_bar`.
  Detect the bar by geometry (64dp tall, 28dp top margin, centred) rather than by id.

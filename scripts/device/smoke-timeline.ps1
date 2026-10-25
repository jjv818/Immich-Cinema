# FOCUS-006 smoke pass, v2 (device).
# v1 missed two things: the tab bar's focused node is a TextView child (no resource-id), so the
# bar check failed; and phase B started from the bar, so RIGHT walked tabs instead of cells.
# v2 detects the bar by geometry + parent walk, and re-enters content before each phase.
#   B: scrubber reach + DOWN + ENTER month jump
#   C: scroll into older buckets; focused cell must not move once settled
#   D: tab away -> BACK (leave-off), compared against the pre-leave cell bounds
param(
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = $env:ADB_SERIAL,
    [string]$Package = "com.immichcinema.tv",
    [string]$Activity = "com.immichcinema.tv/nl.giejay.android.tv.immich.MainActivity",
    [string]$OutDir = "$env:TEMP\immich-cinema-device\timeline_smoke"
)

# The device serial is deliberately not committed. Find it with `adb devices`, then either export
# ADB_SERIAL for the session ($env:ADB_SERIAL = "<serial>") or pass -Serial explicitly.
if (-not $Serial) {
    throw "No device serial. Export ADB_SERIAL (see 'adb devices') or pass -Serial <serial>."
}

$ErrorActionPreference = "Continue"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Invoke-Adb([string[]]$AdbArgs) { & $Adb -s $Serial @AdbArgs 2>&1 }

function Dump-Ui([string]$Name) {
    $remote = "/sdcard/$Name.xml"
    $local = Join-Path $OutDir "$Name.xml"
    $lastOut = ""
    for ($attempt = 1; $attempt -le 6; $attempt++) {
        $lastOut = (Invoke-Adb @("shell", "uiautomator", "dump", $remote)) -join "`n"
        if ($lastOut -match "dumped to") {
            Invoke-Adb @("pull", $remote, $local) | Out-Null
            if (Test-Path $local) { try { return [xml](Get-Content $local -Raw) } catch { } }
        }
        Start-Sleep -Milliseconds 1200
    }
    throw "uiautomator dump '$Name' failed after 6 attempts. Last output: $lastOut"
}

function Get-Focused($Xml) {
    foreach ($n in $Xml.SelectNodes("//node[@focused='true']")) { return $n }
    return $null
}

function Get-FocusedCell($Xml) {
    foreach ($n in $Xml.SelectNodes("//node[@focused='true']")) {
        if ($n.'resource-id' -like "*timeline_mosaic_cell") { return $n }
    }
    return $null
}

function Describe($Node) {
    if (-not $Node) { return "(none)" }
    return "$($Node.'resource-id') @ $($Node.bounds) text='$($Node.text)' desc='$($Node.'content-desc')'"
}

function In-Bar($Node) {
    # The bar is a 64dp capsule with a 28dp top margin: y 56..184 px at 320dpi.
    if (-not $Node) { return $false }
    if ($Node.bounds -notmatch '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') { return $false }
    $cy = ([int]$matches[2] + [int]$matches[4]) / 2.0
    return ($cy -ge 40 -and $cy -le 200)
}

function Get-CenterY($Node) {
    if ($Node -and $Node.bounds -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        return ([int]$matches[2] + [int]$matches[4]) / 2.0
    }
    return $null
}

function Get-HeaderTexts($Xml) {
    $out = @()
    foreach ($n in $Xml.SelectNodes("//node[@resource-id='com.immichcinema.tv:id/timeline_day_header']")) {
        if ($n.text) { $out += $n.text }
    }
    if ($out.Count -eq 0) { return "(none)" }
    return ($out -join " | ")
}

function Capture([string]$Name) {
    Invoke-Adb @("shell", "screencap", "-p", "/sdcard/$Name.png") | Out-Null
    Invoke-Adb @("pull", "/sdcard/$Name.png", (Join-Path $OutDir "$Name.png")) | Out-Null
}

function Press([string]$Key, [int]$Settle = 450) {
    Invoke-Adb @("shell", "input", "keyevent", $Key) | Out-Null
    Start-Sleep -Milliseconds $Settle
}

function Enter-Content([string]$Tag) {
    # Make sure a mosaic cell owns focus: DOWN out of the bar/memories rail.
    $xml = Dump-Ui "$Tag"
    for ($i = 1; $i -le 3; $i++) {
        if (Get-FocusedCell $xml) { return $xml }
        Press "KEYCODE_DPAD_DOWN" 700
        $xml = Dump-Ui "$Tag-down$i"
    }
    return $xml
}

$r = [ordered]@{}

Invoke-Adb @("connect", $Serial) | Out-Null
Invoke-Adb @("shell", "wm", "size", "1920x1080") | Out-Null
Invoke-Adb @("shell", "wm", "density", "320") | Out-Null
Invoke-Adb @("shell", "am", "force-stop", $Package) | Out-Null
Start-Sleep -Seconds 1
Invoke-Adb @("shell", "am", "start", "-n", $Activity) | Out-Null
Start-Sleep -Seconds 8

$xml = Dump-Ui "s2_00_start"
$r.start = Describe (Get-Focused $xml)
Write-Output "START: $($r.start)"

# ---------- Phase B: scrubber + ENTER jump ----------
$xml = Enter-Content "s2_b_content"
$c = Get-FocusedCell $xml
$r.b_cell = Describe $c
$reached = $false
for ($i = 1; $i -le 6; $i++) {
    $f = Get-Focused $xml
    if ($f -and $f.'resource-id' -like "*timeline_scrubber") { $reached = $true; break }
    Press "KEYCODE_DPAD_RIGHT"
    $xml = Dump-Ui "s2_b_right_$i"
}
if ($reached) {
    $r.b_scrub = Describe (Get-Focused $xml)
    Press "KEYCODE_DPAD_DOWN" 600
    Press "KEYCODE_DPAD_DOWN" 600
    $xml = Dump-Ui "s2_b_scrub_down"
    $r.b_scrub_down = Describe (Get-Focused $xml)
    $before = Get-HeaderTexts $xml
    Press "KEYCODE_DPAD_CENTER" 3000
    $xml = Dump-Ui "s2_b_jump"
    $r.b_jump_focus = Describe (Get-Focused $xml)
    $r.b_headers_before = $before
    $r.b_headers_after = Get-HeaderTexts $xml
    Capture "s2_b_jump"
} else {
    $r.b_scrub = "NOT REACHED after 6 RIGHTs"
    $r.b_jump_focus = "SKIPPED"
}

# ---------- Phase C: scroll into older buckets, sample stability ----------
$xml = Enter-Content "s2_c_content"
1..12 | ForEach-Object { Press "KEYCODE_DPAD_DOWN" 450 }
Start-Sleep -Seconds 2
$xml1 = Dump-Ui "s2_c_sample1"
$c1 = Get-FocusedCell $xml1
$r.c_sample1 = "$(Describe $c1) headers=$(Get-HeaderTexts $xml1)"
Start-Sleep -Seconds 5
$xml2 = Dump-Ui "s2_c_sample2"
$c2 = Get-FocusedCell $xml2
$r.c_sample2 = "$(Describe $c2) headers=$(Get-HeaderTexts $xml2)"
$y1 = Get-CenterY $c1; $y2 = Get-CenterY $c2
$r.c_delta = if ($null -ne $y1 -and $null -ne $y2) { [math]::Round($y2 - $y1, 1) } else { "n/a" }
$r.c_headers_changed = (Get-HeaderTexts $xml1) -ne (Get-HeaderTexts $xml2)
Capture "s2_c_scrolled"

# ---------- Phase D: tab away -> BACK (leave-off) ----------
$beforeCell = Get-FocusedCell $xml2
$r.d_cell_before = Describe $beforeCell
$yBefore = Get-CenterY $beforeCell

$xml = Dump-Ui "s2_d_pre_up"
for ($i = 1; $i -le 5; $i++) {
    if (In-Bar (Get-Focused $xml)) { break }
    Press "KEYCODE_DPAD_UP" 700
    $xml = Dump-Ui "s2_d_up$i"
}
$barFocus = Get-Focused $xml
$r.d_bar = Describe $barFocus
$r.d_bar_reached = In-Bar $barFocus

if (In-Bar $barFocus) {
    Press "KEYCODE_DPAD_RIGHT" 600
    Press "KEYCODE_DPAD_RIGHT" 600
    $xml = Dump-Ui "s2_d_tabsel"
    $r.d_tab_target = Describe (Get-Focused $xml)
    Press "KEYCODE_DPAD_CENTER" 3500
    $xml = Dump-Ui "s2_d_tabbed"
    $r.d_tabbed = Describe (Get-Focused $xml)
    Capture "s2_d_tabbed"
    Press "KEYCODE_BACK" 3500
    $xml = Dump-Ui "s2_d_back"
    $afterCell = Get-FocusedCell $xml
    $r.d_after_back = Describe (Get-Focused $xml)
    Capture "s2_d_after_back"
    $yAfter = Get-CenterY $afterCell
    $r.d_delta = if ($null -ne $yBefore -and $null -ne $yAfter) { [math]::Round($yAfter - $yBefore, 1) } else { "n/a" }
} else {
    $r.d_tabbed = "SKIPPED: bar not reached"
    $r.d_after_back = "SKIPPED"
}

Write-Output ""
Write-Output "SMOKE SUMMARY"
foreach ($k in $r.Keys) { Write-Output ("  {0}: {1}" -f $k, $r[$k]) }
Write-Output "  evidence: $OutDir"

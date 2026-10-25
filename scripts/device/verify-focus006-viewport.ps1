# FOCUS-006 device verification: viewer BACK must return the focused mosaic cell
# to its pre-viewer screen position.
# v2: retries uiautomator dump (idle-state flakiness), binary-safe screenshots (shell screencap + pull),
#     focus diagnostics on failure, before/after content-desc printed as identity evidence.
param(
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = "<tv-serial>",
    [string]$Package = "com.immichcinema.tv",
    [string]$Activity = "com.immichcinema.tv/nl.giejay.android.tv.immich.MainActivity",
    [int]$TolerancePx = 24,
    [string]$OutDir = "$env:TEMP\immich-cinema-device\focus006_verify"
)

$ErrorActionPreference = "Continue"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

function Invoke-Adb([string[]]$AdbArgs) {
    & $Adb -s $Serial @AdbArgs 2>&1
}

function Dump-Ui([string]$Name) {
    $remote = "/sdcard/$Name.xml"
    $local = Join-Path $OutDir "$Name.xml"
    $lastOut = ""
    for ($attempt = 1; $attempt -le 6; $attempt++) {
        $lastOut = (Invoke-Adb @("shell", "uiautomator", "dump", $remote)) -join "`n"
        if ($lastOut -match "dumped to") {
            Invoke-Adb @("pull", $remote, $local) | Out-Null
            if (Test-Path $local) {
                try { return [xml](Get-Content $local -Raw) } catch { }
            }
        }
        Start-Sleep -Milliseconds 1500
    }
    throw "uiautomator dump '$Name' failed after 6 attempts. Last output: $lastOut"
}

function Find-FocusedCell($Xml) {
    foreach ($c in $Xml.SelectNodes("//node[@focused='true']")) {
        if ($c.'resource-id' -like "*timeline_mosaic_cell") { return $c }
    }
    return $null
}

function Describe-Focus($Xml) {
    $foci = $Xml.SelectNodes("//node[@focused='true']") | ForEach-Object { "$($_.'resource-id') @ $($_.bounds)" }
    if (-not $foci) { return "(none)" }
    return ($foci -join " | ")
}

function Parse-Bounds([string]$Text) {
    if ($Text -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        return [pscustomobject]@{
            Left = [int]$matches[1]; Top = [int]$matches[2]
            Right = [int]$matches[3]; Bottom = [int]$matches[4]
            CenterY = ([int]$matches[2] + [int]$matches[4]) / 2.0
        }
    }
    throw "Unparseable bounds: $Text"
}

function Capture-Screenshot([string]$Name) {
    # On-device file + pull keeps the PNG binary intact (PowerShell text redirection corrupts it).
    $remote = "/sdcard/$Name.png"
    $local = Join-Path $OutDir "$Name.png"
    Invoke-Adb @("shell", "screencap", "-p", $remote) | Out-Null
    Invoke-Adb @("pull", $remote, $local) | Out-Null
    if (-not (Test-Path $local)) { throw "screenshot missing: $local" }
    $fi = Get-Item $local
    if ($fi.Length -lt 100000) { throw "screenshot suspiciously small ($($fi.Length) bytes): $local" }
    return $local
}

Invoke-Adb @("connect", $Serial) | Out-Null
Start-Sleep -Milliseconds 500
Invoke-Adb @("shell", "wm", "size", "1920x1080") | Out-Null
Invoke-Adb @("shell", "wm", "density", "320") | Out-Null

# Non-destructive clean start (no data cleared).
Invoke-Adb @("shell", "am", "force-stop", $Package) | Out-Null
Start-Sleep -Seconds 1
Invoke-Adb @("shell", "am", "start", "-n", $Activity) | Out-Null
Start-Sleep -Seconds 7

# Move focus into the mosaic and scroll down so the restore has a non-zero offset.
1..4 | ForEach-Object {
    Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_DPAD_DOWN") | Out-Null
    Start-Sleep -Milliseconds 650
}
Start-Sleep -Seconds 2

$beforeXml = Dump-Ui "focus006_before"
Invoke-Adb @("shell", "dumpsys", "activity", "top") | Out-File (Join-Path $OutDir "focus006_before_top.txt") -Encoding utf8
$before = Find-FocusedCell $beforeXml
if (-not $before) { throw "No focused mosaic cell before opening the viewer. Focused: $(Describe-Focus $beforeXml)" }
$beforeBounds = Parse-Bounds $before.bounds
Capture-Screenshot "focus006_before" | Out-Null

# Open viewer, then BACK.
Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_DPAD_CENTER") | Out-Null
Start-Sleep -Seconds 3
Capture-Screenshot "focus006_viewer" | Out-Null
Invoke-Adb @("shell", "input", "keyevent", "KEYCODE_BACK") | Out-Null
Start-Sleep -Seconds 3

$afterXml = Dump-Ui "focus006_after"
Invoke-Adb @("shell", "dumpsys", "activity", "top") | Out-File (Join-Path $OutDir "focus006_after_top.txt") -Encoding utf8
$after = Find-FocusedCell $afterXml
if (-not $after) { throw "No focused mosaic cell after returning from the viewer. Focused: $(Describe-Focus $afterXml)" }
$afterBounds = Parse-Bounds $after.bounds
Capture-Screenshot "focus006_after" | Out-Null

$delta = [math]::Round($afterBounds.CenterY - $beforeBounds.CenterY, 1)
$result = if ([math]::Abs($delta) -le $TolerancePx) { "PASS" } else { "FAIL" }

$beforeDesc = $before.'content-desc'
$afterDesc = $after.'content-desc'
Write-Output "FOCUS-006 verification"
Write-Output "  before: $($before.bounds)  centerY=$($beforeBounds.CenterY)  desc='$beforeDesc'"
Write-Output "  after : $($after.bounds)  centerY=$($afterBounds.CenterY)  desc='$afterDesc'"
Write-Output "  deltaY: $delta px (tolerance +/-$TolerancePx px)"
Write-Output "  result: $result"
Write-Output "  evidence: $OutDir"
if ($result -ne "PASS") { exit 1 }

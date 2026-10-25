# FOCUS-008 verification, A/B on the same pixels (v3 - state aware).
#
# v2's blind "DOWN when lost" recovery walked the app into the Settings hub. v3 never presses a key
# without knowing the current state: it dumps, then reacts (scrubber -> LEFT, wrong screen -> BACK,
# no right neighbour -> DOWN). Row-end RIGHT hands focus to the scrubber by design, so that is a
# normal state, not a failure.
#
# Measurement: strips beside a target cell are row background while it is unfocused (A); after
# focus, the 1.06 scale grows the cell into those exact pixels (B), so a bright neutral line there
# is the hairline, not photo content.
param(
    [string]$Adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    [string]$Serial = $env:ADB_SERIAL,
    [string]$Package = "com.immichcinema.tv",
    [string]$Activity = "com.immichcinema.tv/nl.giejay.android.tv.immich.MainActivity",
    [string]$OutDir = "$env:TEMP\immich-cinema-device\focus008_ab",
    [int]$Samples = 8
)

# The device serial is deliberately not committed. Find it with `adb devices`, then either export
# ADB_SERIAL for the session ($env:ADB_SERIAL = "<serial>") or pass -Serial explicitly.
if (-not $Serial) {
    throw "No device serial. Export ADB_SERIAL (see 'adb devices') or pass -Serial <serial>."
}

$ErrorActionPreference = "Continue"
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null
Add-Type -AssemblyName System.Drawing

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

function Press([string]$Key, [int]$Settle = 800) {
    Invoke-Adb @("shell", "input", "keyevent", $Key) | Out-Null
    Start-Sleep -Milliseconds $Settle
}

function Shot([string]$Name) {
    Invoke-Adb @("shell", "screencap", "-p", "/sdcard/$Name.png") | Out-Null
    $local = Join-Path $OutDir "$Name.png"
    Invoke-Adb @("pull", "/sdcard/$Name.png", $local) | Out-Null
    return $local
}

function Parse-Bounds([string]$Text) {
    if ($Text -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
        return [pscustomobject]@{ L = [int]$matches[1]; T = [int]$matches[2]; R = [int]$matches[3]; B = [int]$matches[4] }
    }
    return $null
}

function Get-Cells($Xml) {
    $out = @()
    foreach ($node in $Xml.SelectNodes("//node[@resource-id='com.immichcinema.tv:id/timeline_mosaic_cell']")) {
        $b = Parse-Bounds $node.bounds
        if ($b) { $out += [pscustomobject]@{ Bounds = $b; Focused = ($node.focused -eq 'true'); Desc = $node.'content-desc' } }
    }
    return $out
}

function Focus-Info($Xml) {
    $node = $Xml.SelectNodes("//node[@focused='true']") | Select-Object -First 1
    if (-not $node) { return [pscustomobject]@{ Id = ""; IsCell = $false; IsScrubber = $false } }
    $id = [string]$node.'resource-id'
    return [pscustomobject]@{
        Id = $id
        IsCell = ($id -like "*timeline_mosaic_cell")
        IsScrubber = ($id -like "*timeline_scrubber")
    }
}

function Strip-Stats([System.Drawing.Bitmap]$Bmp, [int]$X0, [int]$Y0, [int]$X1, [int]$Y1) {
    $x0 = [math]::Max(0, $X0); $y0 = [math]::Max(0, $Y0)
    $x1 = [math]::Min($Bmp.Width - 1, $X1); $y1 = [math]::Min($Bmp.Height - 1, $Y1)
    if ($x1 -le $x0 -or $y1 -le $y0) { return $null }
    $max = 0; $total = 0.0; $count = 0
    $xStep = [math]::Max(2, [int](($x1 - $x0) / 60))
    $yStep = [math]::Max(2, [int](($y1 - $y0) / 12))
    for ($yy = $y0; $yy -le $y1; $yy += $yStep) {
        for ($xx = $x0; $xx -le $x1; $xx += $xStep) {
            $p = $Bmp.GetPixel($xx, $yy)
            $m = [math]::Min([math]::Min($p.R, $p.G), $p.B)
            if ($m -gt $max) { $max = $m }
            $total += $m; $count++
        }
    }
    if ($count -eq 0) { return $null }
    return [pscustomobject]@{ Max = $max; Mean = [math]::Round($total / $count, 1) }
}

function Body-Luma([System.Drawing.Bitmap]$Bmp, [pscustomobject]$R) {
    $x0 = $R.L + [int](($R.R - $R.L) * 0.25); $x1 = $R.R - [int](($R.R - $R.L) * 0.25)
    $y0 = $R.T + [int](($R.B - $R.T) * 0.25); $y1 = $R.B - [int](($R.B - $R.T) * 0.25)
    $total = 0.0; $count = 0
    for ($yy = [math]::Max(0, $y0); $yy -le [math]::Min($Bmp.Height - 1, $y1); $yy += 16) {
        for ($xx = [math]::Max(0, $x0); $xx -le [math]::Min($Bmp.Width - 1, $x1); $xx += 16) {
            $p = $Bmp.GetPixel($xx, $yy)
            $total += (0.299 * $p.R + 0.587 * $p.G + 0.114 * $p.B); $count++
        }
    }
    if ($count -eq 0) { return 0 }
    return [math]::Round($total / $count, 1)
}

$results = @()

Invoke-Adb @("connect", $Serial) | Out-Null
Invoke-Adb @("shell", "wm", "size", "1920x1080") | Out-Null
Invoke-Adb @("shell", "wm", "density", "320") | Out-Null
Invoke-Adb @("shell", "am", "force-stop", $Package) | Out-Null
Start-Sleep -Seconds 1
Invoke-Adb @("shell", "am", "start", "-n", $Activity) | Out-Null
Start-Sleep -Seconds 8

# Enter the mosaic: cold start focuses the memories rail, one DOWN lands on a cell.
Press "KEYCODE_DPAD_DOWN" 900

$attempt = 0
$guard = 0
while ($results.Count -lt $Samples -and $attempt -lt 30 -and $guard -lt 90) {
    $attempt++
    $guard++
    $tag = "ab3_{0:d2}" -f $attempt
    $xml = Dump-Ui "${tag}_a"
    $cells = @(Get-Cells $xml)
    $focus = Focus-Info $xml

    if ($cells.Count -eq 0) {
        Write-Output "attempt $attempt : not the timeline (focus='$($focus.Id)') -> BACK"
        Press "KEYCODE_BACK" 1800
        $guard += 1
        continue
    }
    if ($focus.IsScrubber) {
        Write-Output "attempt $attempt : scrubber owns focus -> LEFT"
        Press "KEYCODE_DPAD_LEFT" 700
        $attempt--
        continue
    }
    $focused = $cells | Where-Object { $_.Focused } | Select-Object -First 1
    if (-not $focused) {
        Press "KEYCODE_DPAD_DOWN" 800
        $attempt--
        continue
    }

    $target = $cells | Where-Object {
        -not $_.Focused -and $_.Bounds.L -gt ($focused.Bounds.R + 4) -and
        $_.Bounds.L -lt ($focused.Bounds.R + 60) -and
        [math]::Abs($_.Bounds.T - $focused.Bounds.T) -lt 30 -and
        $_.Bounds.R -le 1918
    } | Sort-Object { $_.Bounds.L } | Select-Object -First 1

    if (-not $target) {
        Write-Output "attempt $attempt : row end -> DOWN"
        Press "KEYCODE_DPAD_DOWN" 900
        $attempt--
        continue
    }

    $bnd = $target.Bounds
    $nl = [int]$bnd.L; $nt = [int]$bnd.T; $nr = [int]$bnd.R; $nb = [int]$bnd.B
    $stripTop   = @(($nl + 40), ($nt - 18), ($nr - 40), ($nt + 2))
    $stripRight = @(($nr - 6),  ($nt + 30), ($nr + 14), ($nb - 30))
    $stripLeft  = @(($nl - 14), ($nt + 30), ($nl + 6),  ($nb - 30))

    $shotA = Shot "${tag}_a"
    $bmpA = $null; $topA = $null; $rightA = $null; $leftA = $null
    try {
        $bmpA = [System.Drawing.Bitmap]::FromFile($shotA)
        $topA   = Strip-Stats $bmpA $stripTop[0]   $stripTop[1]   $stripTop[2]   $stripTop[3]
        $rightA = Strip-Stats $bmpA $stripRight[0] $stripRight[1] $stripRight[2] $stripRight[3]
        $leftA  = Strip-Stats $bmpA $stripLeft[0]  $stripLeft[1]  $stripLeft[2]  $stripLeft[3]
    } finally { if ($bmpA) { $bmpA.Dispose() } }

    Press "KEYCODE_DPAD_RIGHT" 1000

    $xml2 = Dump-Ui "${tag}_b"
    $nowFocused = (Get-Cells $xml2) | Where-Object { $_.Focused } | Select-Object -First 1
    if (-not $nowFocused -or [math]::Abs($nowFocused.Bounds.L - $nl) -gt 30) {
        Write-Output "attempt $attempt : focus did not land on $nl -> retry"
        $attempt--
        continue
    }
    $shotB = Shot "${tag}_b"
    $bmpB = $null; $topB = $null; $rightB = $null; $leftB = $null; $luma = 0
    try {
        $bmpB = [System.Drawing.Bitmap]::FromFile($shotB)
        $topB   = Strip-Stats $bmpB $stripTop[0]   $stripTop[1]   $stripTop[2]   $stripTop[3]
        $rightB = Strip-Stats $bmpB $stripRight[0] $stripRight[1] $stripRight[2] $stripRight[3]
        $leftB  = Strip-Stats $bmpB $stripLeft[0]  $stripLeft[1]  $stripLeft[2]  $stripLeft[3]
        $luma = Body-Luma $bmpB $nowFocused.Bounds
    } finally { if ($bmpB) { $bmpB.Dispose() } }

    $topGain = $topB.Max - $topA.Max
    $rightGain = $rightB.Max - $rightA.Max
    $leftGain = $leftB.Max - $leftA.Max
    $hair = (($topGain -ge 120 -and $topB.Max -ge 200) -or
             ($rightGain -ge 60 -and $rightB.Max -ge 200) -or
             ($leftGain -ge 60 -and $leftB.Max -ge 200))

    $row = [pscustomobject]@{
        Attempt = $attempt
        Target = "$nl,$nt $($nr - $nl)x$($nb - $nt)"
        Desc = $target.Desc
        Luma = $luma
        Tone = if ($luma -ge 140) { "bright" } elseif ($luma -le 80) { "dark" } else { "mid" }
        TopA = $topA.Max; TopB = $topB.Max; TopGain = $topGain
        RightA = $rightA.Max; RightB = $rightB.Max; RightGain = $rightGain
        LeftA = $leftA.Max; LeftB = $leftB.Max; LeftGain = $leftGain
        Hairline = $hair
        Files = $tag
    }
    $results += $row
    Write-Output ("{0,2} tgt={1,-20} {2,-6} luma={3,5} | top {4,3}->{5,-3} (+{6,-4}) | right {7,3}->{8,-3} (+{9,-4}) | left {10,3}->{11,-3} (+{12,-4}) | HAIRLINE={13}" -f `
        $attempt, $row.Target, $row.Tone, $luma, $topA.Max, $topB.Max, $topGain, $rightA.Max, $rightB.Max, $rightGain, $leftA.Max, $leftB.Max, $leftGain, $hair)
}

Write-Output ""
Write-Output "=== A/B SUMMARY (same pixels, cell unfocused -> focused) ==="
Write-Output ("valid samples={0}  hairline_detected={1}" -f $results.Count, (@($results | Where-Object { $_.Hairline }).Count))
foreach ($tone in @("bright","mid","dark")) {
    $g = @($results | Where-Object { $_.Tone -eq $tone })
    if ($g.Count -gt 0) { Write-Output ("{0}: n={1} hairline={2}" -f $tone, $g.Count, (@($g | Where-Object { $_.Hairline }).Count)) }
}
if ($results.Count -gt 0) {
    Write-Output ("top gain min/max: {0} / {1}" -f (($results | Measure-Object TopGain -Minimum).Minimum), (($results | Measure-Object TopGain -Maximum).Maximum))
}
$results | Export-Csv -NoTypeInformation -Path (Join-Path $OutDir "ab_samples.csv")
Write-Output "evidence: $OutDir"

# Re-score the FOCUS-008 A/B screenshots using geometry instead of raw strip maxima.
#
# The earlier metric sampled a strip that extended 2px inside the unfocused cell, so a white photo
# (or the neighbouring focused cell) could read "bright" in frame A and mask a real hairline.
# This analysis uses the scale growth itself as the reference band:
#   photoTop_A  = first row above the cell where the photo appears (frame A, unfocused)
#   photoTop_B  = same in frame B (focused; the 1.06 scale lifts it ~10-12px)
# The band between the two is screen area the focused cell newly covers. That is exactly where the
# hairline must be, and it was row background in frame A, so photo content cannot explain a bright
# neutral line there.
param(
    [string]$Dir = "$env:TEMP\immich-cinema-device\focus008_ab"
)

$ErrorActionPreference = "Continue"
Add-Type -AssemblyName System.Drawing

function Profile([string]$Path, [int]$X0, [int]$X1, [int]$YFrom, [int]$YTo) {
    $bmp = [System.Drawing.Bitmap]::FromFile($Path)
    try {
        $out = @{}
        for ($y = $YFrom; $y -le $YTo; $y++) {
            if ($y -lt 0 -or $y -ge $bmp.Height) { continue }
            $max = 0; $sum = 0; $n = 0
            for ($x = $X0; $x -le $X1; $x += 4) {
                if ($x -lt 0 -or $x -ge $bmp.Width) { continue }
                $p = $bmp.GetPixel($x, $y)
                $m = [math]::Min([math]::Min($p.R, $p.G), $p.B)
                if ($m -gt $max) { $max = $m }
                $sum += $m; $n++
            }
            if ($n -gt 0) { $out[$y] = [pscustomobject]@{ Max = $max; Mean = [math]::Round($sum / $n, 0) } }
        }
        return $out
    } finally { $bmp.Dispose() }
}

$csv = Join-Path $Dir "ab_samples.csv"
$rows = Import-Csv $csv
$report = @()

foreach ($r in $rows) {
    if ($r.Target -notmatch '^(\d+),(\d+) (\d+)x(\d+)$') { continue }
    $L = [int]$matches[1]; $T = [int]$matches[2]; $W = [int]$matches[3]; $H = [int]$matches[4]
    $x0 = $L + [int]($W * 0.30); $x1 = $L + [int]($W * 0.70)
    $tag = $r.Files
    $pa = Join-Path $Dir "${tag}_a.png"; $pb = Join-Path $Dir "${tag}_b.png"
    if (-not (Test-Path $pa) -or -not (Test-Path $pb)) { continue }

    $pA = Profile $pa $x0 $x1 ($T - 40) ($T + 60)
    $pB = Profile $pb $x0 $x1 ($T - 40) ($T + 60)

    # First row (scanning down) where actual image content starts: min-channel >= 45.
    $photoTopA = -1; $photoTopB = -1
    for ($y = $T - 40; $y -le $T + 60; $y++) {
        if ($photoTopA -lt 0 -and $pA.ContainsKey($y) -and $pA[$y].Mean -ge 45) { $photoTopA = $y }
        if ($photoTopB -lt 0 -and $pB.ContainsKey($y) -and $pB[$y].Mean -ge 45) { $photoTopB = $y }
    }

    # Hairline band: focused frame, just above where the photo starts.
    $bandMax = 0; $bandAt = -1
    for ($y = $photoTopB - 8; $y -le $photoTopB + 2; $y++) {
        if ($pB.ContainsKey($y) -and $pB[$y].Mean -gt $bandMax) { $bandMax = $pB[$y].Mean; $bandAt = $y }
    }
    # Body reference: 25..45px inside the photo.
    $bodySum = 0; $bodyN = 0
    for ($y = $photoTopB + 25; $y -le $photoTopB + 45; $y++) {
        if ($pB.ContainsKey($y)) { $bodySum += $pB[$y].Mean; $bodyN++ }
    }
    $body = if ($bodyN -gt 0) { [math]::Round($bodySum / $bodyN, 0) } else { 0 }

    # Same band in frame A must have been background (proves it was outside the cell).
    $bandABg = 0
    for ($y = $photoTopB - 8; $y -le $photoTopB + 2; $y++) {
        if ($pA.ContainsKey($y) -and $pA[$y].Mean -gt $bandABg) { $bandABg = $pA[$y].Mean }
    }

    $delta = $bandMax - $body
    $verdict = if ($bandABg -ge 100) { "BAND NOT BACKGROUND (inconclusive)" }
               elseif ($bandMax -ge 200 -and $delta -ge 25) { "HAIRLINE VISIBLE" }
               elseif ($bandMax -ge 200) { "coinincident with photo (white-on-white)" }
               else { "NO HAIRLINE" }

    $report += [pscustomobject]@{
        Sample = $r.Attempt; Tone = $r.Tone; Luma = $r.Luma
        PhotoTopA = $photoTopA; PhotoTopB = $photoTopB; ScaleLift = ($photoTopA - $photoTopB)
        BandMax = $bandMax; BandAt = $bandAt; BandInA = $bandABg
        Body = $body; Delta = $delta; Verdict = $verdict
    }
}

$report | Format-Table -AutoSize | Out-String -Width 200
$report | Export-Csv -NoTypeInformation -Path (Join-Path $Dir "hairline_verdict.csv")
"visible={0}  of={1}" -f (@($report | Where-Object { $_.Verdict -eq 'HAIRLINE VISIBLE' }).Count), $report.Count
foreach ($tone in @("bright","mid","dark")) {
    $g = @($report | Where-Object { $_.Tone -eq $tone })
    if ($g.Count -gt 0) { "  {0}: n={1} visible={2}" -f $tone, $g.Count, (@($g | Where-Object { $_.Verdict -eq 'HAIRLINE VISIBLE' }).Count) }
}

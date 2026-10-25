# Generates the Immich Cinema brand assets from geometry rather than shipping a bitmap, so the
# artwork is original, auditable and reproducible (AGENTS.md §1.2: own icon, no Immich
# logo/wordmark artwork).
#
# The mark is a photograph: a sun over two mountain ridges, on the same near-black tile the UI uses.
# It states what the product actually shows - a personal photo library on a television - instead of
# borrowing another project's identity, and it stays legible at launcher size.
#
# Outputs (all under app/src/main/res):
#   drawable/icon.png            512x512  launcher icon + sign-in guidance icon
#   drawable/icon_600px.png      600x600  home-screen channel logo (80dp, circular mask)
#   mipmap-xhdpi/icon_banner_v2.png       TV banner, 640x360 px = 320x180 dp at xhdpi
#
# Run: powershell -ExecutionPolicy Bypass -File scripts/branding/generate-icons.ps1

param(
    [string]$RepoRoot = (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

# Palette: AGENTS.md §5.1. No colour outside the token set; the sun uses a tint of accent_brand so it
# stays visible on the dark tile.
$SurfaceBase = [System.Drawing.Color]::FromArgb(255, 0x0A, 0x0A, 0x0B)
$TextPrimary = [System.Drawing.Color]::FromArgb(255, 0xF2, 0xF2, 0xF7)
$AccentBrand = [System.Drawing.Color]::FromArgb(255, 0x42, 0x50, 0xAF)
$SunTint = [System.Drawing.Color]::FromArgb(255, 0xB9, 0xBF, 0xF0)

$SS = 4

function New-RoundedRectPath {
    param([single]$X, [single]$Y, [single]$W, [single]$H, [single]$R)
    $p = New-Object System.Drawing.Drawing2D.GraphicsPath
    $d = $R * 2
    $p.AddArc($X, $Y, $d, $d, 180, 90)
    $p.AddArc($X + $W - $d, $Y, $d, $d, 270, 90)
    $p.AddArc($X + $W - $d, $Y + $H - $d, $d, $d, 0, 90)
    $p.AddArc($X, $Y + $H - $d, $d, $d, 90, 90)
    $p.CloseFigure()
    return $p
}

function Draw-Mark {
    <#
      Sun over two ridges, drawn into $G inside the square box at ($Ox,$Oy) of width $Size.
      All geometry is expressed as fractions of $Size so it scales without a second spec.
    #>
    param($G, [single]$Ox, [single]$Oy, [single]$Size)

    # Ridges: one silhouette, drawn back-to-front so the near ridge reads as nearer.
    function Ridge([single[]]$Pts, [System.Drawing.Color]$Colour) {
        $arr = New-Object 'System.Drawing.PointF[]' ($Pts.Length / 2)
        for ($i = 0; $i -lt $arr.Length; $i++) {
            $arr[$i] = New-Object System.Drawing.PointF((($Ox + $Pts[$i * 2] * $Size)), (($Oy + $Pts[$i * 2 + 1] * $Size)))
        }
        $brush = New-Object System.Drawing.SolidBrush($Colour)
        $G.FillPolygon($brush, $arr)
        $brush.Dispose()
    }

    # Sun, behind the far ridge.
    $sunR = 0.105 * $Size
    $sunBrush = New-Object System.Drawing.SolidBrush($SunTint)
    $G.FillEllipse($sunBrush, ($Ox + 0.615 * $Size - $sunR), ($Oy + 0.335 * $Size - $sunR), ($sunR * 2), ($sunR * 2))
    $sunBrush.Dispose()

    # Far ridge, dimmer so the two ranges separate.
    Ridge -Pts @(0.10, 0.70,  0.33, 0.43,  0.46, 0.56,  0.60, 0.40,  0.90, 0.70) -Colour ([System.Drawing.Color]::FromArgb(255, 0x9C, 0xA2, 0xC8))

    # Near ridge, full text_primary.
    Ridge -Pts @(0.10, 0.74,  0.28, 0.56,  0.40, 0.66,  0.54, 0.50,  0.72, 0.68,  0.90, 0.74) -Colour $TextPrimary

    # Ground: a single hairline under both ridges keeps the silhouette anchored on dark launchers.
    $pen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(120, $TextPrimary.R, $TextPrimary.G, $TextPrimary.B), (0.014 * $Size))
    $pen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
    $G.DrawLine($pen, ($Ox + 0.10 * $Size), ($Oy + 0.775 * $Size), ($Ox + 0.90 * $Size), ($Oy + 0.775 * $Size))
    $pen.Dispose()
}

function New-SquareIcon {
    param([int]$Size, [string]$Path)
    $big = $Size * $SS
    $canvas = New-Object System.Drawing.Bitmap($big, $big, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.Clear([System.Drawing.Color]::Transparent)

    $bg = New-RoundedRectPath -X 0 -Y 0 -W $big -H $big -R (0.22 * $big)
    $bgBrush = New-Object System.Drawing.SolidBrush($SurfaceBase)
    $g.FillPath($bgBrush, $bg)
    $rimPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(38, $TextPrimary.R, $TextPrimary.G, $TextPrimary.B), (0.006 * $big))
    $g.DrawPath($rimPen, $bg)
    $rimPen.Dispose()
    $bgBrush.Dispose()
    $bg.Dispose()

    Draw-Mark -G $g -Ox 0 -Oy 0 -Size $big
    $g.Dispose()

    $out = New-Object System.Drawing.Bitmap($Size, $Size, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $og = [System.Drawing.Graphics]::FromImage($out)
    $og.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $og.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $og.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $og.DrawImage($canvas, 0, 0, $Size, $Size)
    $og.Dispose()
    $canvas.Dispose()
    $out.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $out.Dispose()
}

function Draw-LetterspacedText {
    param($G, [string]$Text, [single]$X, [single]$Y, $Font, $Brush, [single]$Tracking)
    $x = $X
    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    foreach ($ch in $Text.ToCharArray()) {
        $s = [string]$ch
        $G.DrawString($s, $Font, $Brush, $x, $Y, $fmt)
        $x += $G.MeasureString($s, $Font, [System.Drawing.PointF]::new(0, 0), $fmt).Width + $Tracking
    }
}

function Measure-LetterspacedText {
    param($G, [string]$Text, $Font, [single]$Tracking)
    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    $w = 0.0
    foreach ($ch in $Text.ToCharArray()) {
        $w += $G.MeasureString([string]$ch, $Font, [System.Drawing.PointF]::new(0, 0), $fmt).Width + $Tracking
    }
    return $w
}

function New-Banner {
    param([int]$W, [int]$H, [string]$Path)
    $bigW = $W * 2
    $bigH = $H * 2
    $canvas = New-Object System.Drawing.Bitmap($bigW, $bigH, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($canvas)
    $g.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $g.Clear($SurfaceBase)

    # Faint accent wash, bottom-left to top-right, so the tile is not a flat rectangle.
    $washRect = New-Object System.Drawing.Rectangle(0, 0, $bigW, $bigH)
    $wash = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
        $washRect,
        [System.Drawing.Color]::FromArgb(46, $AccentBrand.R, $AccentBrand.G, $AccentBrand.B),
        [System.Drawing.Color]::FromArgb(0, $AccentBrand.R, $AccentBrand.G, $AccentBrand.B),
        [single]45)
    $g.FillRectangle($wash, 0, 0, $bigW, $bigH)
    $wash.Dispose()

    $markSize = 0.58 * $bigH
    $markX = 0.07 * $bigW
    Draw-Mark -G $g -Ox $markX -Oy (($bigH - $markSize) / 2) -Size $markSize

    # Wordmark. Measured and scaled with the same call that draws it (MeasureString on one string),
    # so it can never run off the tile the way hand-tracked glyphs can.
    $fontName = "Segoe UI"
    $spaceX = $markX + $markSize + (0.06 * $bigW)
    $availW = $bigW - $spaceX - (0.06 * $bigW)
    $text = "IMMICH CINEMA"
    $fmt = [System.Drawing.StringFormat]::GenericTypographic
    $emSize = 0.155 * $bigH
    $font = New-Object System.Drawing.Font($fontName, $emSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
    $measured = $g.MeasureString($text, $font, [System.Drawing.PointF]::new(0, 0), $fmt)
    if ($measured.Width -gt $availW) {
        $scale = $availW / $measured.Width
        $font.Dispose()
        $emSize = $emSize * $scale
        $font = New-Object System.Drawing.Font($fontName, $emSize, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
        $measured = $g.MeasureString($text, $font, [System.Drawing.PointF]::new(0, 0), $fmt)
    }
    # Two-tone: "IMMICH" in text_primary, "CINEMA" in a lighter accent tint.
    $brush = New-Object System.Drawing.SolidBrush($TextPrimary)
    $textY = ($bigH - $measured.Height) / 2
    $g.DrawString($text, $font, $brush, $spaceX, $textY, $fmt)
    $brush.Dispose()
    $font.Dispose()
    $g.Dispose()

    $out = New-Object System.Drawing.Bitmap($W, $H, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $og = [System.Drawing.Graphics]::FromImage($out)
    $og.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $og.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $og.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $og.DrawImage($canvas, 0, 0, $W, $H)
    $og.Dispose()
    $canvas.Dispose()
    $out.Save($Path, [System.Drawing.Imaging.ImageFormat]::Png)
    $out.Dispose()
}

$res = Join-Path $RepoRoot "app\src\main\res"
New-SquareIcon -Size 512 -Path (Join-Path $res "drawable\icon.png")
New-SquareIcon -Size 600 -Path (Join-Path $res "drawable\icon_600px.png")
New-Banner -W 640 -H 360 -Path (Join-Path $res "mipmap-xhdpi\icon_banner_v2.png")

foreach ($p in @("drawable\icon.png", "drawable\icon_600px.png", "mipmap-xhdpi\icon_banner_v2.png")) {
    $full = Join-Path $res $p
    $b = New-Object System.Drawing.Bitmap($full)
    "wrote {0,-40} {1}x{2}  {3:N0} bytes" -f $p, $b.Width, $b.Height, (Get-Item $full).Length
    $b.Dispose()
}

[CmdletBinding()]
param(
    [ValidateSet("Auto", "AppImage", "Exe")]
    [string]$PackageType = "Auto",
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$KeepBuildDirectories
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendDirectory = Join-Path $projectRoot "frontend"
$backendDirectory = Join-Path $projectRoot "backend"
$releaseDirectory = Join-Path $projectRoot "release"
$stagingDirectory = Join-Path $releaseDirectory "staging"
$appName = "CNPC Smart Retail"
$appVersion = "1.0.0"
$wixDownloadUrl = "https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip"
$wixArchiveSha256 = "6AC824E1642D6F7277D0ED7EA09411A508F6116BA6FAE0AA5F2C7DAA2FF43D31"

function Require-Command {
    param([Parameter(Mandatory)][string]$Name)
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "Required build command was not found: $Name"
    }
    return $command.Source
}

function Invoke-Checked {
    param(
        [Parameter(Mandatory)][string]$Executable,
        [Parameter(Mandatory)][string[]]$Arguments,
        [Parameter(Mandatory)][string]$WorkingDirectory
    )
    Push-Location $WorkingDirectory
    try {
        & $Executable @Arguments
        if ($LASTEXITCODE -ne 0) {
            throw "$Executable failed with exit code $LASTEXITCODE"
        }
    } finally {
        Pop-Location
    }
}

function Remove-BuildDirectory {
    param([Parameter(Mandatory)][string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }
    Get-ChildItem -LiteralPath $Path -Recurse -Force -File -ErrorAction SilentlyContinue |
        Where-Object { $_.IsReadOnly } |
        ForEach-Object { $_.IsReadOnly = $false }
    Remove-Item -LiteralPath $Path -Recurse -Force
}

function New-AppIcon {
    param([Parameter(Mandatory)][string]$Path)
    Add-Type -AssemblyName System.Drawing
    $bitmap = [System.Drawing.Bitmap]::new(64, 64)
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([System.Drawing.Color]::Transparent)
    $brush = [System.Drawing.SolidBrush]::new([System.Drawing.Color]::FromArgb(198, 24, 36))
    $graphics.FillRectangle($brush, 2, 2, 60, 60)
    $font = [System.Drawing.Font]::new("Arial", 23, [System.Drawing.FontStyle]::Bold)
    $textBrush = [System.Drawing.Brushes]::White
    $graphics.DrawString("CN", $font, $textBrush, 7, 17)
    $iconHandle = $bitmap.GetHicon()
    $icon = [System.Drawing.Icon]::FromHandle($iconHandle)
    $stream = [System.IO.File]::Create($Path)
    try {
        $icon.Save($stream)
    } finally {
        $stream.Dispose()
        $icon.Dispose()
        $font.Dispose()
        $brush.Dispose()
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Enable-WixToolset {
    if ((Get-Command candle.exe -ErrorAction SilentlyContinue) -and
        (Get-Command light.exe -ErrorAction SilentlyContinue)) {
        return $true
    }

    $wixToolsDirectory = Join-Path $releaseDirectory "tools\wix314"
    $candlePath = Join-Path $wixToolsDirectory "candle.exe"
    $lightPath = Join-Path $wixToolsDirectory "light.exe"
    if (-not (Test-Path $candlePath) -or -not (Test-Path $lightPath)) {
        Write-Host "WiX Toolset 3 was not found. Downloading the verified portable build..."
        $curl = Require-Command "curl.exe"
        $toolsDirectory = Split-Path -Parent $wixToolsDirectory
        $archivePath = Join-Path $toolsDirectory "wix314-binaries.zip"
        New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
        Invoke-Checked -Executable $curl -Arguments @(
            "-L", "--fail", "--silent", "--show-error", "--output", $archivePath, $wixDownloadUrl
        ) -WorkingDirectory $projectRoot
        $archiveHash = (Get-FileHash -LiteralPath $archivePath -Algorithm SHA256).Hash
        if ($archiveHash -ne $wixArchiveSha256) {
            throw "WiX archive checksum validation failed."
        }
        Remove-BuildDirectory -Path $wixToolsDirectory
        Expand-Archive -LiteralPath $archivePath -DestinationPath $wixToolsDirectory -Force
    }
    $env:Path = "$wixToolsDirectory;$env:Path"
    return (Test-Path $candlePath) -and (Test-Path $lightPath)
}

$jpackage = Require-Command "jpackage"

if (-not $SkipBuild) {
    $npm = Require-Command "npm"
    $maven = Require-Command "mvn"
    Write-Host "[1/5] Building frontend..."
    Invoke-Checked -Executable $npm -Arguments @("ci") -WorkingDirectory $frontendDirectory
    if (-not $SkipTests) {
        Invoke-Checked -Executable $npm -Arguments @("test") -WorkingDirectory $frontendDirectory
    }
    Invoke-Checked -Executable $npm -Arguments @("run", "build") -WorkingDirectory $frontendDirectory

    Write-Host "[2/5] Building backend with embedded frontend..."
    $mavenArguments = if ($SkipTests) { @("-DskipTests", "clean", "package") } else { @("clean", "package") }
    Invoke-Checked -Executable $maven -Arguments $mavenArguments -WorkingDirectory $backendDirectory
} else {
    Write-Host "[1/5] Reusing existing frontend and backend build outputs..."
    if (-not (Test-Path (Join-Path $frontendDirectory "dist\index.html"))) {
        throw "Frontend build output is missing. Run without -SkipBuild first."
    }
}

$wixAvailable = $false
if ($PackageType -ne "AppImage") {
    try {
        $wixAvailable = Enable-WixToolset
    } catch {
        if ($PackageType -eq "Exe") {
            throw
        }
        Write-Warning "WiX could not be prepared; falling back to an app image: $($_.Exception.Message)"
    }
}
$effectiveType = switch ($PackageType) {
    "Exe" {
        if (-not $wixAvailable) {
            throw "WiX Toolset 3 is required for an EXE installer and could not be prepared."
        }
        "exe"
    }
    "AppImage" { "app-image" }
    default { if ($wixAvailable) { "exe" } else { "app-image" } }
}
$packageDirectory = if ($effectiveType -eq "exe") {
    Join-Path $releaseDirectory "installer"
} else {
    Join-Path $releaseDirectory "package"
}

Write-Host "[3/5] Preparing package input..."
Remove-BuildDirectory -Path $stagingDirectory
Remove-BuildDirectory -Path $packageDirectory
New-Item -ItemType Directory -Path $stagingDirectory, $packageDirectory -Force | Out-Null
$backendJar = Get-ChildItem -LiteralPath (Join-Path $backendDirectory "target") -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Select-Object -First 1
if (-not $backendJar) {
    throw "Backend executable jar was not produced."
}
Copy-Item -LiteralPath $backendJar.FullName -Destination $stagingDirectory
$iconPath = Join-Path $stagingDirectory "cnpc-smart-retail.ico"
New-AppIcon -Path $iconPath

Write-Host "[4/5] Creating Windows $effectiveType package..."
$jpackageArguments = @(
    "--type", $effectiveType,
    "--dest", $packageDirectory,
    "--name", $appName,
    "--app-version", $appVersion,
    "--vendor", "China National Petroleum Corporation",
    "--description", "CNPC promotion intelligent retail system",
    "--input", $stagingDirectory,
    "--main-jar", $backendJar.Name,
    "--icon", $iconPath,
    "--add-modules", "ALL-MODULE-PATH",
    "--java-options", "-Dcnpc.desktop=true",
    "--java-options", "-Dserver.address=127.0.0.1",
    "--java-options", "-Dserver.port=18083",
    "--java-options", "-Dfile.encoding=UTF-8",
    "--java-options", "-Xmx1024m"
)
if ($effectiveType -eq "exe") {
    $jpackageArguments += @(
        "--win-dir-chooser",
        "--win-per-user-install",
        "--win-menu",
        "--win-menu-group", "CNPC Smart Retail",
        "--win-shortcut"
    )
}
Invoke-Checked -Executable $jpackage -Arguments $jpackageArguments -WorkingDirectory $projectRoot

Write-Host "[5/5] Finalizing release..."
if ($effectiveType -eq "app-image") {
    $appImage = Join-Path $packageDirectory $appName
    $zipPath = Join-Path $releaseDirectory "CNPC-Smart-Retail-$appVersion-portable.zip"
    if (Test-Path $zipPath) {
        Remove-Item -LiteralPath $zipPath -Force
    }
    Compress-Archive -LiteralPath $appImage -DestinationPath $zipPath -CompressionLevel Optimal
    Write-Host "Portable app: $appImage"
    Write-Host "Transfer archive: $zipPath"
} else {
    $installer = Get-ChildItem -LiteralPath $packageDirectory -Filter "*.exe" | Select-Object -First 1
    Write-Host "Installer: $($installer.FullName)"
}

if (-not $KeepBuildDirectories) {
    Remove-BuildDirectory -Path $stagingDirectory
}

Write-Host "Build completed."

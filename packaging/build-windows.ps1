[CmdletBinding()]
param(
    [ValidateSet("Auto", "AppImage", "Exe", "Msi")]
    [string]$PackageType = "Auto",
    [switch]$SkipTests,
    [switch]$SkipBuild,
    [switch]$KeepBuildDirectories,
    [string]$SigningCertificateThumbprint,
    [ValidateSet("CurrentUser", "LocalMachine")]
    [string]$SigningCertificateStore = "CurrentUser",
    [string]$TimestampUrl,
    [switch]$RequireSignature,
    [switch]$DefenderScan
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$projectRoot = Split-Path -Parent $PSScriptRoot
$frontendDirectory = Join-Path $projectRoot "frontend"
$backendDirectory = Join-Path $projectRoot "backend"
$releaseDirectory = Join-Path $projectRoot "release"
$stagingDirectory = Join-Path $releaseDirectory "staging"
$appName = "CNPC Smart Retail"
$appVersion = "1.1.1"
$upgradeUuid = "5fbbb47e-a4a7-4d6c-8624-6ef6beccf099"
$postgresBinaryArtifact = "io.zonky.test.postgres:embedded-postgres-binaries-windows-amd64:16.14.0:jar"
$postgresArchiveName = "postgres-windows-x86_64.txz"
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

function Find-SignTool {
    $command = Get-Command "signtool.exe" -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $sdkRoot = Join-Path ${env:ProgramFiles(x86)} "Windows Kits\10\bin"
    if (Test-Path -LiteralPath $sdkRoot) {
        $candidate = Get-ChildItem -LiteralPath $sdkRoot -Recurse -Filter "signtool.exe" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match "\\x64\\signtool\.exe$" } |
            Sort-Object FullName -Descending |
            Select-Object -First 1
        if ($candidate) {
            return $candidate.FullName
        }
    }
    throw "signtool.exe was not found. Install the Windows SDK Signing Tools feature."
}

function Get-CodeSigningCertificate {
    param(
        [Parameter(Mandatory)][string]$Thumbprint,
        [Parameter(Mandatory)][string]$Store
    )
    $normalizedThumbprint = $Thumbprint.Replace(" ", "").ToUpperInvariant()
    $certificatePath = "Cert:\$Store\My\$normalizedThumbprint"
    $certificate = Get-Item -LiteralPath $certificatePath -ErrorAction SilentlyContinue
    if (-not $certificate -or -not $certificate.HasPrivateKey) {
        throw "A code-signing certificate with a private key was not found at $certificatePath"
    }
    if ($certificate.NotAfter -le (Get-Date)) {
        throw "The code-signing certificate has expired: $($certificate.Subject)"
    }
    $hasCodeSigningUsage = $certificate.EnhancedKeyUsageList.ObjectId.Value -contains "1.3.6.1.5.5.7.3.3"
    if (-not $hasCodeSigningUsage) {
        throw "The selected certificate is not valid for code signing: $($certificate.Subject)"
    }
    return $certificate
}

function Invoke-CodeSigning {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$SignTool,
        [Parameter(Mandatory)][System.Security.Cryptography.X509Certificates.X509Certificate2]$Certificate,
        [Parameter(Mandatory)][string]$Store,
        [string]$TimestampServer
    )
    $arguments = @("sign", "/fd", "SHA256", "/sha1", $Certificate.Thumbprint)
    if ($Store -eq "LocalMachine") {
        $arguments += "/sm"
    }
    if (-not [string]::IsNullOrWhiteSpace($TimestampServer)) {
        $arguments += @("/tr", $TimestampServer, "/td", "SHA256")
    }
    $arguments += @("/d", $appName, $Path)
    Invoke-Checked -Executable $SignTool -Arguments $arguments -WorkingDirectory $projectRoot | Out-Host

    $signature = Get-AuthenticodeSignature -LiteralPath $Path
    if ($signature.Status -ne "Valid") {
        throw "Signature verification failed for ${Path}: $($signature.StatusMessage)"
    }
}

function Add-PostgresBinaryArchive {
    param(
        [Parameter(Mandatory)][string]$Maven,
        [Parameter(Mandatory)][string]$Jar,
        [Parameter(Mandatory)][string]$DestinationDirectory,
        [string]$SignTool,
        [System.Security.Cryptography.X509Certificates.X509Certificate2]$Certificate,
        [string]$CertificateStore,
        [string]$TimestampServer
    )
    $dependencyDirectory = Join-Path $DestinationDirectory "postgres-dependency"
    $archiveExtractionDirectory = Join-Path $DestinationDirectory "postgres-archive"
    New-Item -ItemType Directory -Path $dependencyDirectory, $archiveExtractionDirectory -Force | Out-Null
    Invoke-Checked -Executable $Maven -Arguments @(
        "org.apache.maven.plugins:maven-dependency-plugin:3.8.1:copy",
        "-Dartifact=$postgresBinaryArtifact",
        "-DoutputDirectory=$dependencyDirectory",
        "-Dmdep.stripVersion=true"
    ) -WorkingDirectory $backendDirectory | Out-Host

    $binaryJar = Get-ChildItem -LiteralPath $dependencyDirectory -Filter "*.jar" | Select-Object -First 1
    if (-not $binaryJar) {
        throw "The PostgreSQL binary dependency could not be prepared."
    }
    Invoke-Checked -Executable $Jar -Arguments @("xf", $binaryJar.FullName, $postgresArchiveName) `
        -WorkingDirectory $archiveExtractionDirectory | Out-Host
    $sourceArchive = Join-Path $archiveExtractionDirectory $postgresArchiveName
    $destinationArchive = Join-Path $DestinationDirectory $postgresArchiveName

    if ($Certificate) {
        $tar = Require-Command "tar.exe"
        $nativeDirectory = Join-Path $DestinationDirectory "postgres-native"
        New-Item -ItemType Directory -Path $nativeDirectory -Force | Out-Null
        Invoke-Checked -Executable $tar -Arguments @("-xf", $sourceArchive, "-C", $nativeDirectory) `
            -WorkingDirectory $projectRoot | Out-Host
        $nativeFiles = Get-ChildItem -LiteralPath $nativeDirectory -Recurse -File |
            Where-Object { $_.Extension -in @(".exe", ".dll") }
        foreach ($nativeFile in $nativeFiles) {
            Invoke-CodeSigning -Path $nativeFile.FullName -SignTool $SignTool -Certificate $Certificate `
                -Store $CertificateStore -TimestampServer $TimestampServer | Out-Host
        }
        Invoke-Checked -Executable $tar -Arguments @("-cJf", $destinationArchive, "-C", $nativeDirectory, ".") `
            -WorkingDirectory $projectRoot | Out-Host
    } else {
        Copy-Item -LiteralPath $sourceArchive -Destination $destinationArchive
    }

    Remove-BuildDirectory -Path $dependencyDirectory
    Remove-BuildDirectory -Path $archiveExtractionDirectory
    $nativePath = Join-Path $DestinationDirectory "postgres-native"
    if (Test-Path -LiteralPath $nativePath) {
        Remove-BuildDirectory -Path $nativePath
    }
    return $destinationArchive
}

function Invoke-DefenderArtifactScan {
    param([Parameter(Mandatory)][string]$Path)
    $platformRoot = Join-Path $env:ProgramData "Microsoft\Windows Defender\Platform"
    if (-not (Test-Path -LiteralPath $platformRoot)) {
        throw "Microsoft Defender command-line scanner was not found."
    }
    $scanner = Get-ChildItem -LiteralPath $platformRoot -Directory |
        Sort-Object Name -Descending |
        Select-Object -First 1 |
        ForEach-Object { Join-Path $_.FullName "MpCmdRun.exe" }
    if (-not (Test-Path -LiteralPath $scanner)) {
        throw "Microsoft Defender command-line scanner was not found."
    }
    & $scanner -Scan -ScanType 3 -File $Path -DisableRemediation | Out-Host
    if ($LASTEXITCODE -ne 0) {
        throw "Microsoft Defender rejected the release artifact with exit code $LASTEXITCODE"
    }
    $status = Get-MpComputerStatus
    return "No threats found (definitions $($status.AntivirusSignatureVersion))"
}

$jpackage = Require-Command "jpackage"
$maven = Require-Command "mvn"
$jar = Require-Command "jar"
$signTool = $null
$signingCertificate = $null
if (-not [string]::IsNullOrWhiteSpace($SigningCertificateThumbprint)) {
    $signTool = Find-SignTool
    $signingCertificate = Get-CodeSigningCertificate `
        -Thumbprint $SigningCertificateThumbprint `
        -Store $SigningCertificateStore
    Write-Host "Signing as $($signingCertificate.Subject)"
} elseif ($RequireSignature) {
    throw "-RequireSignature requires -SigningCertificateThumbprint."
} else {
    Write-Warning "No code-signing certificate was supplied. This build is for validation only, not production deployment."
}

if (-not $SkipBuild) {
    $npm = Require-Command "npm"
    Write-Host "[1/6] Building frontend..."
    Invoke-Checked -Executable $npm -Arguments @("ci") -WorkingDirectory $frontendDirectory
    if (-not $SkipTests) {
        Invoke-Checked -Executable $npm -Arguments @("test") -WorkingDirectory $frontendDirectory
    }
    Invoke-Checked -Executable $npm -Arguments @("run", "build") -WorkingDirectory $frontendDirectory

    Write-Host "[2/6] Building backend with embedded frontend..."
    $mavenArguments = if ($SkipTests) { @("-DskipTests", "clean", "package") } else { @("clean", "package") }
    Invoke-Checked -Executable $maven -Arguments $mavenArguments -WorkingDirectory $backendDirectory
} else {
    Write-Host "[1/6] Reusing existing frontend and backend build outputs..."
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
    "Msi" {
        if (-not $wixAvailable) {
            throw "WiX Toolset 3 is required for an MSI package and could not be prepared."
        }
        "msi"
    }
    "AppImage" { "app-image" }
    default { if ($wixAvailable) { "exe" } else { "app-image" } }
}
$packageDirectory = if ($effectiveType -eq "exe") {
    Join-Path $releaseDirectory "installer"
} elseif ($effectiveType -eq "msi") {
    Join-Path $releaseDirectory "installer"
} else {
    Join-Path $releaseDirectory "package"
}
$appImageRoot = if ($effectiveType -eq "app-image") {
    $packageDirectory
} else {
    Join-Path $releaseDirectory "app-image-staging"
}

Write-Host "[3/6] Preparing package input..."
Remove-BuildDirectory -Path $stagingDirectory
Remove-BuildDirectory -Path $packageDirectory
if ($appImageRoot -ne $packageDirectory) {
    Remove-BuildDirectory -Path $appImageRoot
}
New-Item -ItemType Directory -Path $stagingDirectory, $packageDirectory, $appImageRoot -Force | Out-Null
$backendJar = Get-ChildItem -LiteralPath (Join-Path $backendDirectory "target") -Filter "*.jar" |
    Where-Object { $_.Name -notlike "*.original" } |
    Select-Object -First 1
if (-not $backendJar) {
    throw "Backend executable jar was not produced."
}
Copy-Item -LiteralPath $backendJar.FullName -Destination $stagingDirectory
$iconPath = Join-Path $stagingDirectory "cnpc-smart-retail.ico"
New-AppIcon -Path $iconPath
$postgresArchive = Add-PostgresBinaryArchive `
    -Maven $maven `
    -Jar $jar `
    -DestinationDirectory $stagingDirectory `
    -SignTool $signTool `
    -Certificate $signingCertificate `
    -CertificateStore $SigningCertificateStore `
    -TimestampServer $TimestampUrl
$postgresArchiveHash = (Get-FileHash -LiteralPath $postgresArchive -Algorithm SHA256).Hash

Write-Host "[4/6] Creating application image..."
$jpackageImageArguments = @(
    "--type", "app-image",
    "--dest", $appImageRoot,
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
Invoke-Checked -Executable $jpackage -Arguments $jpackageImageArguments -WorkingDirectory $projectRoot
$appImage = Join-Path $appImageRoot $appName
$launcher = Join-Path $appImage "$appName.exe"
if ($signingCertificate) {
    Invoke-CodeSigning -Path $launcher -SignTool $signTool -Certificate $signingCertificate `
        -Store $SigningCertificateStore -TimestampServer $TimestampUrl
}

$artifact = $null
if ($effectiveType -eq "app-image") {
    $zipPath = Join-Path $releaseDirectory "CNPC-Smart-Retail-$appVersion-portable.zip"
    if (Test-Path -LiteralPath $zipPath) {
        Remove-Item -LiteralPath $zipPath -Force
    }
    Compress-Archive -LiteralPath $appImage -DestinationPath $zipPath -CompressionLevel Optimal
    $artifact = Get-Item -LiteralPath $zipPath
    Write-Host "Portable app: $appImage"
    Write-Host "Transfer archive: $zipPath"
} else {
    Write-Host "[5/6] Creating Windows $effectiveType package..."
    $jpackageInstallerArguments = @(
        "--type", $effectiveType,
        "--dest", $packageDirectory,
        "--name", $appName,
        "--app-version", $appVersion,
        "--vendor", "China National Petroleum Corporation",
        "--description", "CNPC promotion intelligent retail system",
        "--app-image", $appImage,
        "--win-dir-chooser",
        "--win-menu",
        "--win-menu-group", "CNPC Smart Retail",
        "--win-shortcut",
        "--win-upgrade-uuid", $upgradeUuid
    )
    if ($effectiveType -eq "exe") {
        $jpackageInstallerArguments += "--win-per-user-install"
    }
    Invoke-Checked -Executable $jpackage -Arguments $jpackageInstallerArguments -WorkingDirectory $projectRoot
    $artifact = Get-ChildItem -LiteralPath $packageDirectory -Filter "*.$effectiveType" | Select-Object -First 1
    if (-not $artifact) {
        throw "The Windows $effectiveType package was not produced."
    }
    if ($signingCertificate) {
        Invoke-CodeSigning -Path $artifact.FullName -SignTool $signTool -Certificate $signingCertificate `
            -Store $SigningCertificateStore -TimestampServer $TimestampUrl
    }
    Write-Host "Installer: $($artifact.FullName)"
}

Write-Host "[6/6] Writing security manifest..."
$defenderResult = "Not requested"
if ($DefenderScan) {
    $defenderResult = Invoke-DefenderArtifactScan -Path $artifact.FullName
}
$artifactHash = (Get-FileHash -LiteralPath $artifact.FullName -Algorithm SHA256).Hash
$artifactSignature = if ($artifact.Extension -eq ".zip") {
    "Not applicable to ZIP"
} else {
    (Get-AuthenticodeSignature -LiteralPath $artifact.FullName).Status.ToString()
}
if ($RequireSignature -and $artifactSignature -ne "Valid") {
    throw "The release artifact is not signed with a valid Authenticode signature."
}
$hashFile = Join-Path $releaseDirectory "$($artifact.BaseName).sha256"
Set-Content -LiteralPath $hashFile -Encoding ASCII -Value "$artifactHash *$($artifact.Name)"
$manifestPath = Join-Path $releaseDirectory "$($artifact.BaseName)-security.txt"
$manifest = @(
    "Product: $appName",
    "Version: $appVersion",
    "Publisher: China National Petroleum Corporation",
    "Created: $((Get-Date).ToString('yyyy-MM-dd HH:mm:ss zzz'))",
    "Artifact: $($artifact.Name)",
    "Size: $($artifact.Length)",
    "SHA-256: $artifactHash",
    "Authenticode: $artifactSignature",
    "PostgreSQL archive SHA-256: $postgresArchiveHash",
    "PostgreSQL native signing: $(if ($signingCertificate) { 'Signed' } else { 'Not signed' })",
    "Microsoft Defender: $defenderResult"
)
Set-Content -LiteralPath $manifestPath -Encoding UTF8 -Value $manifest
Write-Host "SHA-256: $artifactHash"
Write-Host "Security manifest: $manifestPath"

if (-not $KeepBuildDirectories) {
    Remove-BuildDirectory -Path $stagingDirectory
    if ($appImageRoot -ne $packageDirectory) {
        Remove-BuildDirectory -Path $appImageRoot
    }
}

Write-Host "Build completed."

$ErrorActionPreference = "Stop"

$RootDir = Resolve-Path (Join-Path $PSScriptRoot "..\..")
Set-Location $RootDir

$IsWindowsRuntime = [System.Runtime.InteropServices.RuntimeInformation]::IsOSPlatform(
    [System.Runtime.InteropServices.OSPlatform]::Windows
)
if (-not $IsWindowsRuntime) {
    throw "Windows packages must be built on Windows. Current OS is not Windows."
}

$ConfigPath = Join-Path $RootDir "packaging\common\package-config.env"
$Config = @{}
Get-Content $ConfigPath | ForEach-Object {
    $Line = $_.Trim()
    if ($Line -eq "" -or $Line.StartsWith("#")) { return }
    if ($Line -match '^([A-Z_]+)="(.*)"$') {
        $Config[$Matches[1]] = $Matches[2]
    }
}

$AppName = $Config["APP_NAME"]
$AppId = $Config["APP_ID"]
$AppVersion = if ($env:APP_VERSION_OVERRIDE) { $env:APP_VERSION_OVERRIDE } else { $Config["APP_VERSION"] }
$Vendor = $Config["VENDOR"]
$MainClass = $Config["MAIN_CLASS"]
$AppModule = $Config["APP_MODULE"]
$AppArtifactPrefix = $Config["APP_ARTIFACT_PREFIX"]
$OutputArtifactPrefix = $Config["OUTPUT_ARTIFACT_PREFIX"]
$BuildDir = $Config["BUILD_DIR"]

$InputDir = Join-Path $RootDir $BuildDir
$DistDir = Join-Path $RootDir "dist\windows"
$IconPath = Join-Path $RootDir "packaging\icons\app.ico"
$ExePath = Join-Path $DistDir "$OutputArtifactPrefix-$AppVersion-windows-x64.exe"
$MsiPath = Join-Path $DistDir "$OutputArtifactPrefix-$AppVersion-windows-x64.msi"
$ZipPath = Join-Path $DistDir "$OutputArtifactPrefix-$AppVersion-windows-x64.zip"

function Require-Command($Name, $Message) {
    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw $Message
    }
}

Require-Command "java" "Java 21 is required but java was not found on PATH."
Require-Command "javac" "A full Java 21 JDK is required, but javac was not found on PATH."
Require-Command "jpackage" "jpackage was not found. Install a full Java 21 JDK and ensure jpackage is on PATH."

$JavaVersionOutput = & java -version 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "java -version failed: $JavaVersionOutput"
}
$JavaVersionLine = ($JavaVersionOutput | Select-Object -First 1).ToString()

if ($JavaVersionLine -notmatch '"21\.') {
    throw "Java 21 is required. Found: $JavaVersionLine"
}

$JavacVersionOutput = & javac -version 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "javac -version failed: $JavacVersionOutput"
}

$JpackageVersionOutput = & jpackage --version 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "jpackage --version failed: $JpackageVersionOutput"
}

if (-not (Test-Path ".\mvnw.cmd")) {
    throw "Maven wrapper mvnw.cmd is missing."
}

Write-Host "Packaging $AppName $AppVersion for Windows x64"
Write-Host "Building Maven artifacts..."
& .\mvnw.cmd -pl $AppModule -am clean install -DskipTests

Remove-Item $InputDir, $DistDir -Recurse -Force -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $InputDir, $DistDir | Out-Null

Write-Host "Copying runtime dependencies..."
& .\mvnw.cmd -pl $AppModule dependency:copy-dependencies "-DincludeScope=runtime" "-DoutputDirectory=$InputDir"

$MainJar = Get-ChildItem "$AppModule\target\$AppArtifactPrefix-*.jar" |
    Where-Object { $_.Name -notmatch '-sources|-javadoc' } |
    Sort-Object Name |
    Select-Object -First 1

if (-not $MainJar) {
    throw "Could not find built app jar under $AppModule\target."
}

Copy-Item $MainJar.FullName $InputDir

$CommonArgs = @(
    "--name", $AppName,
    "--app-version", $AppVersion,
    "--vendor", $Vendor,
    "--input", $InputDir,
    "--main-jar", $MainJar.Name,
    "--main-class", $MainClass,
    "--java-options", "-Dfile.encoding=UTF-8",
    "--win-menu",
    "--win-shortcut",
    "--win-dir-chooser",
    "--win-upgrade-uuid", "8f3682d0-71aa-4c80-8c10-4a1ad0f8b901"
)

if (Test-Path $IconPath) {
    Write-Host "Using Windows icon: $IconPath"
    $CommonArgs += @("--icon", $IconPath)
} else {
    Write-Host "No Windows icon found at packaging\icons\app.ico; packaging without a custom icon."
}

Write-Host "Creating Windows EXE installer..."
& jpackage --type exe @CommonArgs --dest $DistDir

$CreatedExe = Get-ChildItem $DistDir -Filter "*.exe" | Sort-Object Name | Select-Object -First 1
if (-not $CreatedExe -or $CreatedExe.Length -eq 0) {
    throw "jpackage did not create a non-empty EXE in $DistDir. WiX Toolset may be missing or misconfigured."
}
if ($CreatedExe.FullName -ne $ExePath) {
    Move-Item -Force $CreatedExe.FullName $ExePath
}

Write-Host "Creating portable ZIP..."
& jpackage --type app-image @CommonArgs --dest $DistDir

$AppImageDir = Get-ChildItem $DistDir -Directory | Where-Object { $_.Name -match [regex]::Escape($AppName) } | Select-Object -First 1
if ($AppImageDir) {
    Compress-Archive -Path $AppImageDir.FullName -DestinationPath $ZipPath -Force
    Write-Host "ZIP: $ZipPath"
} else {
    Write-Warning "App image directory not found for ZIP creation."
}

if ($env:CREATE_MSI -eq "1") {
    Write-Host "CREATE_MSI=1 set. Attempting MSI installer..."
    try {
        & jpackage --type msi @CommonArgs --dest $DistDir
        $CreatedMsi = Get-ChildItem $DistDir -Filter "*.msi" | Sort-Object Name | Select-Object -First 1
        if ($CreatedMsi -and $CreatedMsi.Length -gt 0) {
            if ($CreatedMsi.FullName -ne $MsiPath) {
                Move-Item -Force $CreatedMsi.FullName $MsiPath
            }
        } else {
            Write-Warning "MSI packaging completed without a non-empty MSI artifact."
        }
    } catch {
        Write-Warning "MSI packaging failed. EXE is still available. Reason: $($_.Exception.Message)"
    }
}

Write-Host ""
Write-Host "Windows packaging complete."
Write-Host "EXE: $ExePath"
if (Test-Path $MsiPath) {
    Write-Host "MSI: $MsiPath"
}
if (Test-Path $ZipPath) {
    Write-Host "ZIP: $ZipPath"
}

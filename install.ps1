param([string]$TvHost = $env:TV_HOST)
$ErrorActionPreference = 'Stop'
if (-not $TvHost) { throw 'Pass -TvHost <TV address> or set TV_HOST.' }
$sourceRoot = Join-Path $PSScriptRoot 'cleanyourtv-main'
$py = Join-Path $sourceRoot 'tools\venv\Scripts\python.exe'
$env:TV_HOST = $TvHost
if (-not $env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME = Join-Path $PSScriptRoot 'gradle-home' }
$localSdk = Join-Path $PSScriptRoot 'android-sdk'
if (Test-Path -LiteralPath $localSdk) { $env:ANDROID_HOME = $localSdk }
$env:PYTHONUTF8 = '1'
if (-not (Test-Path -LiteralPath $py)) { throw 'Python tools are missing.' }
# Preserve the original Home and package states before the official installer touches them.
& $py -I (Join-Path $sourceRoot 'tools\recovery.py') --save (Join-Path $PSScriptRoot 'debloat.txt')
if ($LASTEXITCODE -ne 0) { throw 'Recovery recording failed; installation was stopped.' }
$oldLocation = Get-Location
try {
    # This firmware keeps stock Home at priority 2 until it is disabled. The official
    # installer starts the replacement first and restores stock if handover fails.
    & (Join-Path $sourceRoot 'launcher\build-and-install.ps1') -TvHost $TvHost -StockLauncher com.google.android.tvlauncher
    & $py -I (Join-Path $sourceRoot 'tools\tv.py') --screenshot (Join-Path $PSScriptRoot 'shots\installed-home.png') HOME --wait 2
    if ($LASTEXITCODE -ne 0) { throw 'Could not capture Home. Check the TV and restore.ps1 before continuing.' }
    $activityState = (& $py -I (Join-Path $sourceRoot 'tools\tv.py') 'dumpsys activity activities') -join "`n"
    if ($LASTEXITCODE -ne 0 -or $activityState -notmatch 'mFocusedApp[:=].*com\.gathin\.tvhome/|topResumedActivity=.*com\.gathin\.tvhome/') {
        & (Join-Path $PSScriptRoot 'restore.ps1') -TvHost $TvHost
        throw 'Replacement Home was not focused; the original state was restored.'
    }
} finally {
    Set-Location $oldLocation
}

param([string]$TvHost = $env:TV_HOST)
$ErrorActionPreference = 'Stop'
if (-not $TvHost) { throw 'Pass -TvHost <TV address> or set TV_HOST.' }
$sourceRoot = Join-Path $PSScriptRoot 'cleanyourtv-main'
$py = Join-Path $sourceRoot 'tools\venv\Scripts\python.exe'
$list = Join-Path $PSScriptRoot 'debloat.txt'
if (-not (Test-Path -LiteralPath $py)) { throw 'Python tools are missing.' }
$env:TV_HOST = $TvHost
$env:PYTHONUTF8 = '1'
$OutputEncoding = New-Object System.Text.UTF8Encoding $false
# A relocated checkout has another recovery-record key. Never fall back to guessed states.
$recordLookup = @'
import sys
from pathlib import Path
sys.path.insert(0, str(Path(sys.argv[1]) / 'tools'))
import recovery
print(recovery.record_path(sys.argv[2], sys.argv[3]))
'@
$recordPath = ($recordLookup | & $py -I - $sourceRoot $list $TvHost | Select-Object -Last 1)
if ($LASTEXITCODE -ne 0 -or -not $recordPath -or -not (Test-Path -LiteralPath $recordPath)) {
    throw 'Original recovery record is missing for this TV and checkout. Restore the matching local record before continuing; nothing was sent to the TV.'
}
& (Join-Path $sourceRoot 'tools\restore.ps1') -TvHost $TvHost -List $list -Stock com.google.android.tvlauncher
if ($LASTEXITCODE -ne 0) { throw 'Restore failed. Review the output before continuing.' }

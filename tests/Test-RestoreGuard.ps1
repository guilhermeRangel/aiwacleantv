param(
    [string]$PythonPath = (Join-Path (Split-Path -Parent $PSScriptRoot) 'cleanyourtv-main\tools\venv\Scripts\python.exe')
)
$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$tempParent = [System.IO.Path]::GetFullPath((Join-Path $repoRoot '.tmp'))
$testRoot = Join-Path $tempParent ('restore-guard-' + [guid]::NewGuid().ToString('N'))
$fixtureTools = Join-Path $testRoot 'cleanyourtv-main\tools'
$fixtureScripts = Join-Path $fixtureTools 'venv\Scripts'
$utf8 = New-Object System.Text.UTF8Encoding $false
try {
    New-Item -ItemType Directory -Path $fixtureScripts -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $repoRoot 'restore.ps1') -Destination $testRoot
    Copy-Item -LiteralPath (Join-Path $repoRoot 'debloat.txt') -Destination $testRoot
    Copy-Item -LiteralPath $PythonPath -Destination (Join-Path $fixtureScripts 'python.exe')
    $venvRoot = Split-Path -Parent (Split-Path -Parent $PythonPath)
    Copy-Item -LiteralPath (Join-Path $venvRoot 'pyvenv.cfg') -Destination (Join-Path $fixtureTools 'venv\pyvenv.cfg')
    [System.IO.File]::WriteAllText((Join-Path $fixtureTools 'recovery.py'), @'
from pathlib import Path
def record_path(list_path, host):
    return Path(list_path).parent / 'fixture-record.json'
'@, $utf8)
    [System.IO.File]::WriteAllText((Join-Path $fixtureTools 'restore.ps1'), @'
param([string]$TvHost, [string]$List, [string]$Stock)
[System.IO.File]::WriteAllText((Join-Path (Split-Path -Parent $List) 'restore-called.txt'), $TvHost)
$global:LASTEXITCODE = 0
'@, $utf8)

    $blocked = $false
    try { & (Join-Path $testRoot 'restore.ps1') -TvHost 'fixture-tv.invalid' }
    catch {
        if ($_.Exception.Message -notlike 'Original recovery record is missing*') { throw }
        $blocked = $true
    }
    if (-not $blocked -or (Test-Path -LiteralPath (Join-Path $testRoot 'restore-called.txt'))) {
        throw 'Missing recovery record did not stop the restore before delegation.'
    }
    Write-Output 'PASS: a missing record blocks the restore without contacting a TV.'

    [System.IO.File]::WriteAllText((Join-Path $testRoot 'fixture-record.json'), '{}', $utf8)
    & (Join-Path $testRoot 'restore.ps1') -TvHost 'fixture-tv.invalid'
    $marker = Join-Path $testRoot 'restore-called.txt'
    if (-not (Test-Path -LiteralPath $marker) -or (Get-Content -LiteralPath $marker -Raw) -ne 'fixture-tv.invalid') {
        throw 'An existing record did not delegate to the upstream restore script.'
    }
    Write-Output 'PASS: an existing record delegates to the upstream restore script.'
} finally {
    # The fixture contains only copied files, with no links to the real runtime.
    $resolvedTestRoot = [System.IO.Path]::GetFullPath($testRoot)
    if (-not $resolvedTestRoot.StartsWith($tempParent + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing cleanup outside the isolated test directory.'
    }
    if (Test-Path -LiteralPath $resolvedTestRoot) {
        Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force
    }
}

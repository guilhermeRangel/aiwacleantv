$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$tempParent = [System.IO.Path]::GetFullPath((Join-Path $repoRoot '.tmp'))
$testRoot = Join-Path $tempParent ('wallpaper-deploy-test-' + [guid]::NewGuid().ToString('N'))
$utf8 = New-Object System.Text.UTF8Encoding $false
try {
    foreach ($directory in @('images', 'themes\aiwa-wallpapers', 'cleanyourtv-main\tools')) {
        New-Item -ItemType Directory -Path (Join-Path $testRoot $directory) -Force | Out-Null
    }
    Copy-Item -LiteralPath (Join-Path $repoRoot 'apply-wallpapers.ps1') -Destination $testRoot
    Copy-Item -LiteralPath (Join-Path $repoRoot 'themes\aiwa-wallpapers\config.json') -Destination (Join-Path $testRoot 'themes\aiwa-wallpapers')
    [System.IO.File]::WriteAllText((Join-Path $testRoot 'cleanyourtv-main\tools\theme.ps1'), @'
param([string]$TvHost, [string]$Theme)
$fixture = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$images = @(Get-ChildItem -LiteralPath (Join-Path $Theme 'wallpapers') -File)
$result = @{ Host = $TvHost; Theme = $Theme; Names = @($images.Name); Bytes = @($images | ForEach-Object { [System.IO.File]::ReadAllBytes($_.FullName)[0] }); Config = (Get-Content -LiteralPath (Join-Path $Theme 'config.json') -Raw | ConvertFrom-Json) }
[System.IO.File]::WriteAllText((Join-Path $fixture 'deployment.json'), ($result | ConvertTo-Json -Depth 5))
$global:LASTEXITCODE = 0
'@, $utf8)
    foreach ($number in 1..35) {
        [System.IO.File]::WriteAllBytes((Join-Path $testRoot "images\$number.jpg"), [byte[]]@($number))
    }
    & (Join-Path $testRoot 'apply-wallpapers.ps1') -TvHost 'fixture-tv.invalid'
    $result = Get-Content -LiteralPath (Join-Path $testRoot 'deployment.json') -Raw | ConvertFrom-Json
    if ($result.Names.Count -ne 34 -or $result.Names -contains '35.jpg' -or $result.Host -ne 'fixture-tv.invalid') {
        throw 'Deployment did not select exactly the requested 34 images.'
    }
    if (($result.Bytes | Sort-Object) -join ',' -ne (1..34 -join ',')) { throw 'Original image bytes changed.' }
    if (-not $result.Config.wallpaper_change_on_home -or $result.Config.wallpaper_rotation_minutes -ne 0) {
        throw 'The deployed profile does not use event-driven wallpaper changes.'
    }
    if (Test-Path -LiteralPath $result.Theme) { throw 'The temporary theme was not removed.' }
    if (@(Get-ChildItem -LiteralPath (Join-Path $testRoot 'images') -File).Count -ne 35) { throw 'Original images were removed.' }
    Write-Output 'PASS: exactly images 1-34 are staged unchanged; originals survive and temporary files are removed.'

    Remove-Item -LiteralPath (Join-Path $testRoot 'deployment.json')
    Remove-Item -LiteralPath (Join-Path $testRoot 'images\7.jpg')
    $blocked = $false
    try { & (Join-Path $testRoot 'apply-wallpapers.ps1') -TvHost 'fixture-tv.invalid' }
    catch {
        if ($_.Exception.Message -notlike 'Missing wallpaper:*') { throw }
        $blocked = $true
    }
    if (-not $blocked -or (Test-Path -LiteralPath (Join-Path $testRoot 'deployment.json'))) {
        throw 'An incomplete collection reached the deployment tool.'
    }
    Write-Output 'PASS: missing images stop deployment before the TV can be changed.'
} finally {
    $resolvedTestRoot = [System.IO.Path]::GetFullPath($testRoot)
    if (-not $resolvedTestRoot.StartsWith($tempParent + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing cleanup outside the isolated test directory.'
    }
    if (Test-Path -LiteralPath $resolvedTestRoot) { Remove-Item -LiteralPath $resolvedTestRoot -Recurse -Force }
}

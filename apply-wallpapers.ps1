param(
    [string]$TvHost = $env:TV_HOST,
    [string]$ImagesDirectory = (Join-Path $PSScriptRoot 'images')
)
$ErrorActionPreference = 'Stop'
if (-not $TvHost) { throw 'Pass -TvHost <TV address> or set TV_HOST.' }

# Validate the complete collection before the upstream tool can replace anything on the TV.
$pictures = @(1..34 | ForEach-Object {
    $path = Join-Path $ImagesDirectory ("$_.jpg")
    if (-not (Test-Path -LiteralPath $path -PathType Leaf)) { throw "Missing wallpaper: $path" }
    $picture = Get-Item -LiteralPath $path
    if ($picture.Length -eq 0 -or $picture.Length -gt 10MB) { throw "Wallpaper must be between 1 byte and 10 MB: $path" }
    $picture
})
$tempParent = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '.tmp'))
$stage = Join-Path $tempParent ('wallpaper-theme-' + [guid]::NewGuid().ToString('N'))
$oldLocation = Get-Location
try {
    New-Item -ItemType Directory -Path (Join-Path $stage 'wallpapers') -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'themes\aiwa-wallpapers\config.json') -Destination $stage
    foreach ($picture in $pictures) {
        Copy-Item -LiteralPath $picture.FullName -Destination (Join-Path $stage 'wallpapers')
    }
    & (Join-Path $PSScriptRoot 'cleanyourtv-main\tools\theme.ps1') -TvHost $TvHost -Theme $stage
    if ($LASTEXITCODE -ne 0) { throw 'Wallpaper deployment failed; review the upstream tool output.' }
} finally {
    Set-Location $oldLocation
    $resolvedStage = [System.IO.Path]::GetFullPath($stage)
    if (-not $resolvedStage.StartsWith($tempParent + [System.IO.Path]::DirectorySeparatorChar, [System.StringComparison]::OrdinalIgnoreCase)) {
        throw 'Refusing cleanup outside the isolated staging directory.'
    }
    if (Test-Path -LiteralPath $resolvedStage) { Remove-Item -LiteralPath $resolvedStage -Recurse -Force }
}

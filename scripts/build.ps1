$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$mainSourceDir = Join-Path $projectRoot "src\\main\\java"
$outputDir = Join-Path $projectRoot "out\\main"

if (-not (Test-Path $mainSourceDir)) {
    throw "Main source directory not found: $mainSourceDir"
}

New-Item -ItemType Directory -Path $outputDir -Force | Out-Null

$sourceList = Join-Path $projectRoot "out\\main-sources.txt"
$sourceFiles = Get-ChildItem -Path $mainSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

if (-not $sourceFiles) {
    throw "No Java source files found under $mainSourceDir"
}

$sourceFiles | Set-Content -Path $sourceList -Encoding Ascii

javac -encoding UTF-8 -d $outputDir "@$sourceList"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed when compiling main sources."
}

Write-Host "Build completed. Classes written to $outputDir"

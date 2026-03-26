$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$testSourceDir = Join-Path $projectRoot "src\\test\\java"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$testOutputDir = Join-Path $projectRoot "out\\test"

& $buildScript

if (-not (Test-Path $testSourceDir)) {
    throw "Test source directory not found: $testSourceDir"
}

New-Item -ItemType Directory -Path $testOutputDir -Force | Out-Null

$sourceList = Join-Path $projectRoot "out\\test-sources.txt"
$sourceFiles = Get-ChildItem -Path $testSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

if (-not $sourceFiles) {
    throw "No Java test files found under $testSourceDir"
}

$sourceFiles | Set-Content -Path $sourceList -Encoding Ascii

javac -encoding UTF-8 -cp $mainOutputDir -d $testOutputDir "@$sourceList"
if ($LASTEXITCODE -ne 0) {
    throw "javac failed when compiling test sources."
}

java -cp "$mainOutputDir;$testOutputDir" com.bupt.tarecruitment.US0204SmokeTest
if ($LASTEXITCODE -ne 0) {
    throw "US0204SmokeTest failed."
}

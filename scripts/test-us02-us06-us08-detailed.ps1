. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$testSourceDir = Join-Path $projectRoot "src\\test\\java"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$testOutputDir = Join-Path $projectRoot "out\\test"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-us020608-detailed-test"

try {
    & $buildScript

    if (-not (Test-Path $testSourceDir)) {
        throw "Test source directory not found: $testSourceDir"
    }

    $argFile = Join-Path $tempWorkspace "test-sources.txt"
    $sourceFiles = Get-ChildItem -Path $testSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

    if (-not $sourceFiles) {
        throw "No Java test files found under $testSourceDir"
    }

    $sourceFiles | Set-Content -Path $argFile -Encoding Ascii

    Reset-Directory -Path $testOutputDir
    & $javac -encoding UTF-8 -cp $mainOutputDir -d $testOutputDir "@$argFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling test sources."
    }

    & $java -cp "$mainOutputDir;$testOutputDir" com.bupt.tarecruitment.US020608DetailedIntegrationTest
    if ($LASTEXITCODE -ne 0) {
        throw "US020608DetailedIntegrationTest failed."
    }
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$testSourceDir = Join-Path $projectRoot "src\\test\\java"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$testOutputDir = Join-Path $projectRoot "out\\test"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-us10-lookup-test"

try {
    & $buildScript

    if (-not (Test-Path $testSourceDir)) {
        throw "Test source directory not found: $testSourceDir"
    }

    $stagedTestSourceDir = Join-Path $tempWorkspace "src\\test\\java"
    $stagedMainOutputDir = Join-Path $tempWorkspace "out\\main"
    $stagedTestOutputDir = Join-Path $tempWorkspace "out\\test"
    $argFile = Join-Path $tempWorkspace "test-sources.txt"

    Copy-DirectoryContents -Source $testSourceDir -Destination $stagedTestSourceDir
    Copy-DirectoryContents -Source $mainOutputDir -Destination $stagedMainOutputDir
    New-Item -ItemType Directory -Path $stagedTestOutputDir -Force | Out-Null

    $sourceFiles = Get-ChildItem -Path $stagedTestSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName

    if (-not $sourceFiles) {
        throw "No Java test files found under $testSourceDir"
    }

    $sourceFiles | Set-Content -Path $argFile -Encoding Ascii

    & $javac -encoding UTF-8 -cp $stagedMainOutputDir -d $stagedTestOutputDir "@$argFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling test sources."
    }

    Reset-Directory -Path $testOutputDir
    Copy-DirectoryContents -Source $stagedTestOutputDir -Destination $testOutputDir

    & $java -cp "$stagedMainOutputDir;$stagedTestOutputDir" com.bupt.tarecruitment.recommendation.MissingSkillsFeedbackServiceLookupTest
    if ($LASTEXITCODE -ne 0) {
        throw "MissingSkillsFeedbackServiceLookupTest failed."
    }
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

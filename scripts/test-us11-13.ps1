. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$testSourceDir = Join-Path $projectRoot "src\\test\\java\\com\\bupt\\tarecruitment"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$testOutputDir = Join-Path $projectRoot "out\\test"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-us11-13-test"

try {
    & $buildScript

    $stagedTestSourceDir = Join-Path $tempWorkspace "src\\test\\java\\com\\bupt\\tarecruitment"
    $stagedMainOutputDir = Join-Path $tempWorkspace "out\\main"
    $stagedTestOutputDir = Join-Path $tempWorkspace "out\\test"
    New-Item -ItemType Directory -Path $stagedTestSourceDir -Force | Out-Null
    New-Item -ItemType Directory -Path $stagedTestOutputDir -Force | Out-Null

    Copy-Item -Path (Join-Path $testSourceDir "US11SmokeTest.java") -Destination (Join-Path $stagedTestSourceDir "US11SmokeTest.java") -Force
    Copy-Item -Path (Join-Path $testSourceDir "US12SmokeTest.java") -Destination (Join-Path $stagedTestSourceDir "US12SmokeTest.java") -Force
    Copy-Item -Path (Join-Path $testSourceDir "US13SmokeTest.java") -Destination (Join-Path $stagedTestSourceDir "US13SmokeTest.java") -Force
    Copy-DirectoryContents -Source $mainOutputDir -Destination $stagedMainOutputDir

    & $javac -encoding UTF-8 -cp $stagedMainOutputDir -d $stagedTestOutputDir (Join-Path $stagedTestSourceDir "US11SmokeTest.java") (Join-Path $stagedTestSourceDir "US12SmokeTest.java") (Join-Path $stagedTestSourceDir "US13SmokeTest.java")
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling US11-US13 smoke tests."
    }

    Reset-Directory -Path $testOutputDir
    Copy-DirectoryContents -Source $stagedTestOutputDir -Destination $testOutputDir

    & $java -cp "$stagedMainOutputDir;$stagedTestOutputDir" com.bupt.tarecruitment.US11SmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "US11SmokeTest failed."
    }

    & $java -cp "$stagedMainOutputDir;$stagedTestOutputDir" com.bupt.tarecruitment.US12SmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "US12SmokeTest failed."
    }

    & $java -cp "$stagedMainOutputDir;$stagedTestOutputDir" com.bupt.tarecruitment.US13SmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "US13SmokeTest failed."
    }

    Write-Host "US11-US13 smoke tests passed."
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

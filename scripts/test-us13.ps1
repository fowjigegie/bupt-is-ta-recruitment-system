. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$testSourceDir = Join-Path $projectRoot "src\\test\\java\\com\\bupt\\tarecruitment"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$testOutputDir = Join-Path $projectRoot "out\\test"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-us13-test"

try {
    & $buildScript

    if (-not (Test-Path $testSourceDir)) {
        throw "Test source directory not found: $testSourceDir"
    }

    $stagedTestSourceDir = Join-Path $tempWorkspace "src\\test\\java\\com\\bupt\\tarecruitment"
    $stagedMainOutputDir = Join-Path $tempWorkspace "out\\main"
    $stagedTestOutputDir = Join-Path $tempWorkspace "out\\test"
    $targetSourceFile = Join-Path $stagedTestSourceDir "US13SmokeTest.java"

    New-Item -ItemType Directory -Path $stagedTestSourceDir -Force | Out-Null
    Copy-Item -Path (Join-Path $testSourceDir "US13SmokeTest.java") -Destination $targetSourceFile -Force
    Copy-DirectoryContents -Source $mainOutputDir -Destination $stagedMainOutputDir
    New-Item -ItemType Directory -Path $stagedTestOutputDir -Force | Out-Null
    & $javac -encoding UTF-8 -cp $stagedMainOutputDir -d $stagedTestOutputDir $targetSourceFile
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling US13SmokeTest."
    }

    Reset-Directory -Path $testOutputDir
    Copy-DirectoryContents -Source $stagedTestOutputDir -Destination $testOutputDir

    & $java -cp "$stagedMainOutputDir;$stagedTestOutputDir" com.bupt.tarecruitment.US13SmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "US13SmokeTest failed."
    }
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

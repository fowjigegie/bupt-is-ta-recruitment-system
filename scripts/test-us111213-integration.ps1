. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$testSourceDir = Join-Path $projectRoot "src\\test\\java\\com\\bupt\\tarecruitment"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$testOutputDir = Join-Path $projectRoot "out\\test"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-us111213-integration"

try {
    & $buildScript

    $stagedTestSourceDir = Join-Path $tempWorkspace "src\\test\\java\\com\\bupt\\tarecruitment"
    $stagedMainOutputDir = Join-Path $tempWorkspace "out\\main"
    $stagedTestOutputDir = Join-Path $tempWorkspace "out\\test"
    $targetSourceFile = Join-Path $stagedTestSourceDir "US111213DetailedIntegrationTest.java"

    New-Item -ItemType Directory -Path $stagedTestSourceDir -Force | Out-Null
    Copy-Item -Path (Join-Path $testSourceDir "US111213DetailedIntegrationTest.java") -Destination $targetSourceFile -Force
    Copy-DirectoryContents -Source $mainOutputDir -Destination $stagedMainOutputDir
    New-Item -ItemType Directory -Path $stagedTestOutputDir -Force | Out-Null

    & $javac -encoding UTF-8 -cp $stagedMainOutputDir -d $stagedTestOutputDir $targetSourceFile
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling US111213DetailedIntegrationTest."
    }

    Reset-Directory -Path $testOutputDir
    Copy-DirectoryContents -Source $stagedTestOutputDir -Destination $testOutputDir

    & $java -cp "$stagedMainOutputDir;$stagedTestOutputDir" com.bupt.tarecruitment.US111213DetailedIntegrationTest
    if ($LASTEXITCODE -ne 0) {
        throw "US111213DetailedIntegrationTest failed."
    }
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

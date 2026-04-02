. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$mainSourceDir = Join-Path $projectRoot "src\\main\\java\\com\\bupt\\tarecruitment"
$testSourceFile = Join-Path $projectRoot "src\\test\\java\\com\\bupt\\tarecruitment\\US14SmokeTest.java"
$mainOutputDir = Join-Path $projectRoot "out\\us14-main"
$testOutputDir = Join-Path $projectRoot "out\\us14-test"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-us14-test"

try {
    if (-not (Test-Path $mainSourceDir)) {
        throw "Main source directory not found: $mainSourceDir"
    }

    if (-not (Test-Path $testSourceFile)) {
        throw "US14 test source file not found: $testSourceFile"
    }

    $stagedMainSourceDir = Join-Path $tempWorkspace "src\\main\\java\\com\\bupt\\tarecruitment"
    $stagedTestSourceDir = Join-Path $tempWorkspace "src\\test\\java\\com\\bupt\\tarecruitment"
    $stagedClassOutputDir = Join-Path $tempWorkspace "out\\classes"
    $allArgFile = Join-Path $tempWorkspace "all-sources.txt"

    Copy-DirectoryContents -Source $mainSourceDir -Destination $stagedMainSourceDir
    New-Item -ItemType Directory -Path $stagedTestSourceDir -Force | Out-Null
    Copy-Item -Path $testSourceFile -Destination $stagedTestSourceDir -Force
    New-Item -ItemType Directory -Path $stagedClassOutputDir -Force | Out-Null

    $mainSourceFiles = Get-ChildItem -Path $stagedMainSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
    if (-not $mainSourceFiles) {
        throw "No Java source files found under $mainSourceDir"
    }

    $testSourceFiles = Get-ChildItem -Path $stagedTestSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
    ($mainSourceFiles + $testSourceFiles) | Set-Content -Path $allArgFile -Encoding Ascii

    & $javac -encoding UTF-8 -d $stagedClassOutputDir "@$allArgFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling US14 sources."
    }

    Reset-Directory -Path $mainOutputDir
    Reset-Directory -Path $testOutputDir
    Copy-DirectoryContents -Source $stagedClassOutputDir -Destination $mainOutputDir
    Copy-DirectoryContents -Source $stagedClassOutputDir -Destination $testOutputDir

    & $java -cp $stagedClassOutputDir com.bupt.tarecruitment.US14SmokeTest
    if ($LASTEXITCODE -ne 0) {
        throw "US14SmokeTest failed."
    }
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

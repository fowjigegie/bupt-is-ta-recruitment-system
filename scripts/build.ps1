. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$mainSourceDir = Join-Path $projectRoot "src\\main\\java"
$outputDir = Join-Path $projectRoot "out\\main"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-main-build"

try {
    if (-not (Test-Path $mainSourceDir)) {
        throw "Main source directory not found: $mainSourceDir"
    }

    $stagedSourceDir = Join-Path $tempWorkspace "src\\main\\java"
    $stagedOutputDir = Join-Path $tempWorkspace "out\\main"
    $argFile = Join-Path $tempWorkspace "main-sources.txt"

    Copy-DirectoryContents -Source $mainSourceDir -Destination $stagedSourceDir
    New-Item -ItemType Directory -Path $stagedOutputDir -Force | Out-Null

    $sourceFiles = Get-ChildItem -Path $stagedSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
    if (-not $sourceFiles) {
        throw "No Java source files found under $mainSourceDir"
    }

    $sourceFiles | Set-Content -Path $argFile -Encoding Ascii
    $javaFxArgs = Get-JavaFxJavacArgs -SourceFiles $sourceFiles

    & $javac @javaFxArgs -encoding UTF-8 -d $stagedOutputDir "@$argFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling main sources."
    }

    Reset-Directory -Path $outputDir
    Copy-DirectoryContents -Source $stagedOutputDir -Destination $outputDir

    $resourcesDir = Join-Path $projectRoot "src\\main\\resources"
    if (Test-Path $resourcesDir) {
        Copy-Item -Path (Join-Path $resourcesDir "*") -Destination $outputDir -Recurse -Force
    }

    Write-Host "Build completed. Classes written to $outputDir"
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

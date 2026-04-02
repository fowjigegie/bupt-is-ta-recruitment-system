. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$mainSourceDir = Join-Path $projectRoot "src\\main\\java"
$webappSourceDir = Join-Path $projectRoot "src\\main\\webapp"
$webRootDir = Join-Path $projectRoot "out\\web\\ROOT"
$webClassesDir = Join-Path $webRootDir "WEB-INF\\classes"
$javac = Get-JavaToolPath -ToolName "javac" -MinimumVersion 21
$tempWorkspace = New-TempWorkspace -Prefix "ta-web-build"

try {
    if (-not (Test-Path $mainSourceDir)) {
        throw "Main source directory not found: $mainSourceDir"
    }

    if (-not (Test-Path $webappSourceDir)) {
        throw "Webapp source directory not found: $webappSourceDir"
    }

    $stagedSourceDir = Join-Path $tempWorkspace "src\\main\\java"
    $stagedClassesDir = Join-Path $tempWorkspace "classes"
    $argFile = Join-Path $tempWorkspace "web-sources.txt"

    Copy-DirectoryContents -Source $mainSourceDir -Destination $stagedSourceDir
    New-Item -ItemType Directory -Path $stagedClassesDir -Force | Out-Null

    $sourceFiles = Get-ChildItem -Path $stagedSourceDir -Recurse -Filter *.java | Select-Object -ExpandProperty FullName
    if (-not $sourceFiles) {
        throw "No Java source files found under $mainSourceDir"
    }

    $sourceFiles | Set-Content -Path $argFile -Encoding Ascii
    $javaFxArgs = Get-JavaFxJavacArgs -SourceFiles $sourceFiles

    & $javac @javaFxArgs -encoding UTF-8 -d $stagedClassesDir "@$argFile"
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed when compiling web classes."
    }

    Reset-Directory -Path $webRootDir
    Copy-DirectoryContents -Source $webappSourceDir -Destination $webRootDir
    New-Item -ItemType Directory -Path $webClassesDir -Force | Out-Null
    Copy-DirectoryContents -Source $stagedClassesDir -Destination $webClassesDir

    Write-Host "Web package prepared at $webRootDir"
} finally {
    if (Test-Path $tempWorkspace) {
        Remove-Item -Recurse -Force $tempWorkspace
    }
}

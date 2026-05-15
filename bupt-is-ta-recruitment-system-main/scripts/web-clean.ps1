param(
    [switch]$RemoveRuntimeData
)

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$containerName = "bupt-ta-recruitment-web-local"
$imageName = "bupt-ta-recruitment-web:local"
$runtimeDataDir = Join-Path $projectRoot "out\\web-runtime-data"
$webBuildDir = Join-Path $projectRoot "out\\web"

cmd /c "docker rm -f $containerName >nul 2>&1"
cmd /c "docker image rm -f $imageName >nul 2>&1"

if ($RemoveRuntimeData -and (Test-Path $runtimeDataDir)) {
    Remove-Item -Recurse -Force $runtimeDataDir
}

if (Test-Path $webBuildDir) {
    Remove-Item -Recurse -Force $webBuildDir
}

Write-Host "Web Docker resources cleaned."

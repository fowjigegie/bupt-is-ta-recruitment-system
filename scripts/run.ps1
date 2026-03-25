$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$mainOutputDir = Join-Path $projectRoot "out\\main"

& $buildScript
java -cp $mainOutputDir com.bupt.tarecruitment.App

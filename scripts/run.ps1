param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AppArgs
)

$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$mainOutputDir = Join-Path $projectRoot "out\\main"

& $buildScript
if ($AppArgs) {
    java -cp $mainOutputDir com.bupt.tarecruitment.App @AppArgs
} else {
    java -cp $mainOutputDir com.bupt.tarecruitment.App
}

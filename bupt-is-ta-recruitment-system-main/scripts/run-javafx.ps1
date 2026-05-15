param(
    [string]$MainClass = "com.bupt.tarecruitment.ui.LoginPage",

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$AppArgs
)

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$buildScript = Join-Path $PSScriptRoot "build.ps1"
$mainOutputDir = Join-Path $projectRoot "out\\main"
$java = Get-JavaToolPath -ToolName "java" -MinimumVersion 21
$javaFxArgs = Get-JavaFxJavaArgs

& $buildScript
Push-Location $projectRoot
try {
    & $java @javaFxArgs -cp $mainOutputDir $MainClass @AppArgs
} finally {
    Pop-Location
}

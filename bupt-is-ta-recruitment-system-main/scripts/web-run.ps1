param(
    [int]$Port = 8080
)

. (Join-Path $PSScriptRoot "common.ps1")

$projectRoot = Split-Path -Parent $PSScriptRoot
$webBuildScript = Join-Path $PSScriptRoot "web-build.ps1"
$webRootDir = Join-Path $projectRoot "out\\web\\ROOT"
$runtimeDataDir = Join-Path $projectRoot "out\\web-runtime-data"
$seedDataDir = Join-Path $projectRoot "data"
$containerName = "bupt-ta-recruitment-web-local"
$imageName = "bupt-ta-recruitment-web:local"
$dockerContext = New-TempWorkspace -Prefix "ta-web-docker"

try {
    docker version | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not available."
    }

    & $webBuildScript

    if (-not (Test-Path $webRootDir)) {
        throw "Web package was not generated: $webRootDir"
    }

    if (-not (Test-Path $runtimeDataDir)) {
        Copy-DirectoryContents -Source $seedDataDir -Destination $runtimeDataDir
    }

    $contextRootDir = Join-Path $dockerContext "ROOT"
    Copy-DirectoryContents -Source $webRootDir -Destination $contextRootDir

    $dockerfilePath = Join-Path $dockerContext "Dockerfile"
    @'
FROM tomcat:9.0-jdk21-temurin
RUN rm -rf /usr/local/tomcat/webapps/ROOT
COPY ROOT /usr/local/tomcat/webapps/ROOT
EXPOSE 8080
'@ | Set-Content -Path $dockerfilePath -Encoding Ascii

    cmd /c "docker rm -f $containerName >nul 2>&1"
    cmd /c "docker image rm -f $imageName >nul 2>&1"

    docker build -t $imageName $dockerContext
    if ($LASTEXITCODE -ne 0) {
        throw "Docker image build failed."
    }

    $runtimeDataMount = (Resolve-Path $runtimeDataDir).Path
    docker run -d `
        --name $containerName `
        -p "${Port}:8080" `
        -v "${runtimeDataMount}:/opt/ta-data" `
        $imageName | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker container failed to start."
    }

    $ready = $false
    for ($attempt = 0; $attempt -lt 30; $attempt++) {
        Start-Sleep -Seconds 2
        try {
            $response = Invoke-WebRequest -Uri "http://localhost:$Port/" -UseBasicParsing -TimeoutSec 5
            if ($response.StatusCode -eq 200) {
                $ready = $true
                break
            }
        } catch {
        }
    }

    if (-not $ready) {
        docker logs $containerName
        throw "Tomcat did not become ready on http://localhost:$Port/"
    }

    Write-Host "Web container is running."
    Write-Host "URL: http://localhost:$Port/"
    Write-Host "Container: $containerName"
    Write-Host "Image: $imageName"
    Write-Host "Runtime data: $runtimeDataDir"
} finally {
    if (Test-Path $dockerContext) {
        Remove-Item -Recurse -Force $dockerContext
    }
}

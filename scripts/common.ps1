$ErrorActionPreference = "Stop"

function Get-JavaVersionNumber {
    param(
        [Parameter(Mandatory = $true)]
        [string]$CommandPath
    )

    $versionOutput = cmd /c "`"$CommandPath`" -version 2>&1" | Select-Object -First 1
    if (-not $versionOutput) {
        return $null
    }

    if ($versionOutput -match 'version "(\d+)(?:\.(\d+))?.*"') {
        if ($Matches[1] -eq "1" -and $Matches[2]) {
            return [int]$Matches[2]
        }
        return [int]$Matches[1]
    }

    if ($versionOutput -match 'javac (\d+)(?:\.(\d+))?') {
        if ($Matches[1] -eq "1" -and $Matches[2]) {
            return [int]$Matches[2]
        }
        return [int]$Matches[1]
    }

    return $null
}

function Get-JavaToolPath {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet("java", "javac")]
        [string]$ToolName,

        [int]$MinimumVersion = 17
    )

    $candidates = New-Object System.Collections.Generic.List[string]

    if ($env:JAVA_HOME) {
        $candidates.Add((Join-Path $env:JAVA_HOME "bin\$ToolName.exe"))
    }

    try {
        $command = Get-Command $ToolName -ErrorAction Stop
        if ($command.Path) {
            $candidates.Add($command.Path)
        }
    } catch {
    }

    $fallbacks = @(
        "C:\Users\fengxu\.vscode\extensions\redhat.java-1.53.0-win32-x64\jre\21.0.10-win32-x86_64\bin\$ToolName.exe",
        "C:\Program Files\Java\jdk-21\bin\$ToolName.exe",
        "C:\Program Files\Java\jdk-17\bin\$ToolName.exe",
        "C:\Program Files\Microsoft\jdk-21*\bin\$ToolName.exe",
        "C:\Program Files\Microsoft\jdk-17*\bin\$ToolName.exe"
    )

    foreach ($fallback in $fallbacks) {
        if ($fallback.Contains("*")) {
            $resolved = Get-ChildItem -Path $fallback -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName
            foreach ($item in $resolved) {
                $candidates.Add($item)
            }
        } else {
            $candidates.Add($fallback)
        }
    }

    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (-not (Test-Path $candidate)) {
            continue
        }

        $version = Get-JavaVersionNumber -CommandPath $candidate
        if ($version -and $version -ge $MinimumVersion) {
            return $candidate
        }
    }

    throw "No $ToolName executable with version $MinimumVersion or newer was found. Please install or configure JDK $MinimumVersion+."
}

function New-TempWorkspace {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Prefix
    )

    $tempRoot = Join-Path $env:TEMP ($Prefix + "-" + [Guid]::NewGuid().ToString("N"))
    New-Item -ItemType Directory -Path $tempRoot -Force | Out-Null
    return $tempRoot
}

function Copy-DirectoryContents {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Source,

        [Parameter(Mandatory = $true)]
        [string]$Destination
    )

    New-Item -ItemType Directory -Path $Destination -Force | Out-Null

    if (-not (Test-Path $Source)) {
        throw "Source directory does not exist: $Source"
    }

    Get-ChildItem -Path $Source -Force | ForEach-Object {
        Copy-Item -Path $_.FullName -Destination $Destination -Recurse -Force
    }
}

function Reset-Directory {
    param(
        [Parameter(Mandatory = $true)]
        [string]$Path
    )

    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        return
    }

    Get-ChildItem -Path $Path -Force -ErrorAction SilentlyContinue | ForEach-Object {
        Remove-Item -Recurse -Force $_.FullName -ErrorAction Stop
    }
}

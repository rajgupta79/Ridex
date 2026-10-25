# Convenience build script: pins the build to Java 21 regardless of the
# machine's default JAVA_HOME. Usage:
#   .\build.ps1                 -> compile (skip tests)
#   .\build.ps1 test            -> run tests
#   .\build.ps1 spring-boot:run -> run the app
#   .\build.ps1 clean package   -> full package

$ErrorActionPreference = "Stop"

$jdk21 = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
if (-not (Test-Path $jdk21)) {
    Write-Error "JDK 21 not found at $jdk21. Install with: winget install --id Microsoft.OpenJDK.21 -e"
    exit 1
}

$env:JAVA_HOME = $jdk21
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

$mvnArgs = if ($args.Count -gt 0) { $args } else { @("-DskipTests", "compile") }

Write-Host "Using JAVA_HOME = $env:JAVA_HOME" -ForegroundColor Cyan
& "$PSScriptRoot\mvnw.cmd" @mvnArgs

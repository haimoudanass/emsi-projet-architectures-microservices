# Microservices project — local session toolchain bootstrap
# Scope: current PowerShell process (+ child processes) only.
# Does NOT modify User/Machine environment variables, registry, or global PATH.
#
# Usage (dot-source — required so vars stay in the current session):
#   . "C:\Users\Administrator\Desktop\microservices-project\scripts\init-env.ps1"

$ErrorActionPreference = 'Stop'

$JdkHome = 'C:\Program Files\Eclipse Adoptium\jdk-21.0.13.10002-hotspot'
$MavenHome = 'C:\Users\Administrator\Desktop\springboot-tools\apache-maven-3.9.6'

function Test-RequiredPath {
    param([string]$Path, [string]$Label)
    if (-not (Test-Path -LiteralPath $Path)) {
        throw "Missing $Label : $Path"
    }
}

Test-RequiredPath -Path (Join-Path $JdkHome 'bin\java.exe') -Label 'JDK 21 java.exe'
Test-RequiredPath -Path (Join-Path $JdkHome 'bin\javac.exe') -Label 'JDK 21 javac.exe'
Test-RequiredPath -Path (Join-Path $MavenHome 'bin\mvn.cmd') -Label 'Maven mvn.cmd'

$env:JAVA_HOME = $JdkHome
$env:MAVEN_HOME = $MavenHome

# Prefix PATH without accumulating duplicate entries for these homes.
$pathParts = $env:PATH -split ';' | Where-Object {
    $_ -and
    ($_ -ne (Join-Path $JdkHome 'bin')) -and
    ($_ -ne (Join-Path $MavenHome 'bin'))
}
$env:PATH = (@(
    (Join-Path $JdkHome 'bin'),
    (Join-Path $MavenHome 'bin')
) + $pathParts) -join ';'

Write-Host '=== Microservices toolchain (session-only) ===' -ForegroundColor Cyan
Write-Host "JAVA_HOME  = $env:JAVA_HOME"
Write-Host "MAVEN_HOME = $env:MAVEN_HOME"
Write-Host ''

# java/javac write version info to stderr; keep Stop for real failures only.
$previousEap = $ErrorActionPreference
$ErrorActionPreference = 'Continue'
try {
    Write-Host '--- java -version ---'
    & java -version 2>&1 | ForEach-Object { "$_" }
    Write-Host '--- javac -version ---'
    & javac -version 2>&1 | ForEach-Object { "$_" }
    Write-Host '--- mvn -version ---'
    & mvn -version
}
finally {
    $ErrorActionPreference = $previousEap
}

Write-Host ''
Write-Host 'First resolvable executables:' -ForegroundColor Cyan
Write-Host ("java  -> " + ((Get-Command java).Source))
Write-Host ("javac -> " + ((Get-Command javac).Source))
Write-Host ("mvn   -> " + ((Get-Command mvn).Source))
Write-Host ''
Write-Host 'Session ready. No User/Machine env vars were changed.' -ForegroundColor Green

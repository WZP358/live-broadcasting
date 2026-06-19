param(
    [switch]$SkipBackend,
    [switch]$SkipFrontend,
    [switch]$SkipAgent,
    [switch]$SeedData,
    [int]$Rooms = 16
)

$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$runLogDir = Join-Path $root ".runlogs"
New-Item -ItemType Directory -Force -Path $runLogDir | Out-Null

function Start-DemoProcess {
    param(
        [Parameter(Mandatory = $true)][string]$Name,
        [Parameter(Mandatory = $true)][string]$FilePath,
        [Parameter(Mandatory = $true)][string]$WorkingDirectory,
        [string[]]$Arguments = @()
    )

    $logPath = Join-Path $runLogDir "$Name.log"
    $errPath = Join-Path $runLogDir "$Name.err.log"
    $process = Start-Process `
        -FilePath $FilePath `
        -ArgumentList $Arguments `
        -WorkingDirectory $WorkingDirectory `
        -RedirectStandardOutput $logPath `
        -RedirectStandardError $errPath `
        -PassThru `
        -WindowStyle Hidden

    Write-Host "[demo] $Name started, pid=$($process.Id), log=$logPath" -ForegroundColor Green
}

function Find-Python {
    $venvPython = Join-Path $root "ai-services\live-agent\.venv\Scripts\python.exe"
    if (Test-Path $venvPython) {
        return $venvPython
    }
    return "python"
}

if ($SeedData) {
    $seedScript = Join-Path $root "scripts\demo-live.ps1"
    if (Test-Path $seedScript) {
        Write-Host "[demo] seeding demo data..." -ForegroundColor Cyan
        & powershell -NoProfile -ExecutionPolicy Bypass -File $seedScript -Rooms $Rooms -Once
    } else {
        Write-Host "[demo] demo-live.ps1 not found, skip data seeding." -ForegroundColor Yellow
    }
}

if (!$SkipAgent) {
    Start-DemoProcess `
        -Name "ai-agent" `
        -FilePath (Find-Python) `
        -WorkingDirectory (Join-Path $root "ai-services\live-agent") `
        -Arguments @("server.py")
}

if (!$SkipBackend) {
    Start-DemoProcess `
        -Name "backend" `
        -FilePath "mvn" `
        -WorkingDirectory (Join-Path $root "backend") `
        -Arguments @("-s", ".\settings.xml", "spring-boot:run")
}

if (!$SkipFrontend) {
    Start-DemoProcess `
        -Name "frontend" `
        -FilePath "npm" `
        -WorkingDirectory (Join-Path $root "frontend") `
        -Arguments @("run", "dev")
}

Write-Host ""
Write-Host "[demo] startup commands sent." -ForegroundColor Cyan
Write-Host "[demo] frontend: http://localhost:5173/ or Vite output port" -ForegroundColor Cyan
Write-Host "[demo] backend:  http://localhost:8088/" -ForegroundColor Cyan
Write-Host "[demo] agent:    http://localhost:8100/api/agent/health" -ForegroundColor Cyan
Write-Host "[demo] logs:     $runLogDir" -ForegroundColor Cyan


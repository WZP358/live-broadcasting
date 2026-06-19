$ErrorActionPreference = "Continue"

$ports = @(5173, 5174, 8088, 8100)

foreach ($port in $ports) {
    $lines = netstat -ano | Select-String ":$port\s+.*LISTENING"
    foreach ($line in $lines) {
        $parts = ($line.Line -replace "\s+", " ").Trim().Split(" ")
        $pid = $parts[-1]
        if ($pid -and $pid -ne "0") {
            Write-Host "[demo] stopping pid=$pid on port $port" -ForegroundColor Yellow
            Stop-Process -Id ([int]$pid) -Force -ErrorAction SilentlyContinue
        }
    }
}

Write-Host "[demo] demo services stopped." -ForegroundColor Green


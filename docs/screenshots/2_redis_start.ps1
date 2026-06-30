$host.ui.RawUI.WindowTitle = "Redis - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Redis Server Startup (Port 6379)" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1] Starting Redis..." -ForegroundColor Green
Write-Host ""

redis-server.exe redis.windows.conf

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

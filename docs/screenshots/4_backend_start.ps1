$host.ui.RawUI.WindowTitle = "Backend - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  PulseLive Spring Boot Backend" -ForegroundColor Yellow
Write-Host "  API: :8088  |  WebSocket: :10022" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location D:\code\live\backend
mvn -s .\settings.xml spring-boot:run

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

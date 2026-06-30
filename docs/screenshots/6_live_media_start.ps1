$host.ui.RawUI.WindowTitle = "Live Media Server - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Local Live Media Server (Node.js)" -ForegroundColor Yellow
Write-Host "  RTMP Push : rtmp://127.0.0.1:1935/live/" -ForegroundColor Green
Write-Host "  HTTP-FL V : http://127.0.0.1:8080/live/" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location D:\code\live\local-services\live-server
npm start

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

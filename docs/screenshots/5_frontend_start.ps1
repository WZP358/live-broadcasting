$host.ui.RawUI.WindowTitle = "Frontend - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  PulseLive Vue3 + Vite Frontend" -ForegroundColor Yellow
Write-Host "  Local: http://localhost:5173/" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location D:\code\live\frontend
npm run dev

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

$host.ui.RawUI.WindowTitle = "Live Guard Vision - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Live Guard YOLOv8 Vision Moderation" -ForegroundColor Yellow
Write-Host "  API : http://127.0.0.1:8300/check" -ForegroundColor Green
Write-Host "  Docs: http://127.0.0.1:8300/docs" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location D:\code\live
& "ai-services\vision-guard\server\venv\Scripts\python.exe" "ai-services\vision-guard\server\vision_guard.py"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

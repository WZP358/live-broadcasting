$host.ui.RawUI.WindowTitle = "Live Agent AI - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Live Agent AI Assistant (LLM)" -ForegroundColor Yellow
Write-Host "  Model: Qwen1.5-1.8B" -ForegroundColor Green
Write-Host "  API : http://127.0.0.1:8100" -ForegroundColor Green
Write-Host "  Health: /api/agent/health" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location D:\code\live
& "ai-services\vision-guard\server\venv\Scripts\python.exe" "ai-services\live-agent\server.py"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

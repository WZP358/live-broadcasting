$host.ui.RawUI.WindowTitle = "DeepFilterNet3 Denoise - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  DeepFilterNet3 Audio Denoising Service" -ForegroundColor Yellow
Write-Host "  WebSocket: ws://127.0.0.1:18765/ws" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Set-Location D:\code\live
& "ai-services\vision-guard\server\venv\Scripts\python.exe" "ai-services\deepfilternet3\server\server.py"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

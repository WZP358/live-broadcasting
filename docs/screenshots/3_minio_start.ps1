$host.ui.RawUI.WindowTitle = "MinIO - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MinIO Object Storage Startup" -ForegroundColor Yellow
Write-Host "  API: :9000  |  Console: :9001" -ForegroundColor Green
Write-Host "  User: minioadmin  |  Pass: minioadmin" -ForegroundColor Green
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

minio server D:\minio\data --address ":9000" --console-address ":9001"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Close this window or Ctrl+C" -ForegroundColor Yellow
Read-Host

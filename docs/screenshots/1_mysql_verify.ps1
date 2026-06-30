$host.ui.RawUI.WindowTitle = "MySQL Verify - close after screenshot"
Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  MySQL Startup Verification" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "[1] Starting MySQL service..." -ForegroundColor Green
net start mysql

Write-Host ""
Write-Host "[2] Show all databases..." -ForegroundColor Green
mysql -u root -pwzp -e "SHOW DATABASES;"

Write-Host ""
Write-Host "[3] Show tables in ant-live..." -ForegroundColor Green
mysql -u root -pwzp ant-live -e "SHOW TABLES;"

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "  Screenshot done? Press Enter to close." -ForegroundColor Yellow
Read-Host

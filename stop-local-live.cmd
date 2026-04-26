@echo off
setlocal enabledelayedexpansion

set "LAST_PID="
for %%P in (1935 8080) do (
  for /f "tokens=5" %%A in ('netstat -ano ^| findstr /R /C:":%%P .*LISTENING"') do (
    if not "%%A"=="0" if not "%%A"=="!LAST_PID!" (
      set "LAST_PID=%%A"
      echo Stopping local live process on port %%P, pid %%A
      taskkill /PID %%A /F >nul 2>nul
    )
  )
)

exit /b 0

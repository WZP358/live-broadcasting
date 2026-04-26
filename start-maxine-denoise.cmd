@echo off
setlocal
cd /d "%~dp0"
if exist "models\live_check\venv\Scripts\python.exe" (
  "models\live_check\venv\Scripts\python.exe" tools\deepfilternet_denoise\server.py
) else (
  python tools\deepfilternet_denoise\server.py
)

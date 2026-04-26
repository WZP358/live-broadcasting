@echo off
setlocal
cd /d "%~dp0models\live_check"
set "YOLO_CONFIG_DIR=%CD%\.ultralytics"
if exist "venv\Scripts\python.exe" (
  "venv\Scripts\python.exe" vision_guard.py
) else (
  python vision_guard.py
)

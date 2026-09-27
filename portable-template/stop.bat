@echo off
setlocal
chcp 65001 >nul

REM ============================================================
REM  Stop Quizandar (portable)
REM ============================================================

echo Stopping Quizandar...

REM --- Find process listening on port 8765 ---
set "FOUND=0"
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8765" ^| findstr "LISTENING"') do (
    echo Killing process with PID %%P...
    taskkill /PID %%P /F >nul 2>&1
    if not errorlevel 1 set "FOUND=1"
)

if "%FOUND%"=="1" (
    echo Quizandar stopped.
) else (
    echo Quizandar is not running (no process found on port 8765).
)

pause
endlocal

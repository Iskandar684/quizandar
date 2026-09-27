@echo off
setlocal
set DIR=%~dp0
set JAVA=%DIR%jre\bin\java.exe
set JAR=%DIR%quizandar.jar
set QUESTIONS_DIR=%DIR%questions

REM --- Detect local IPv4 address of active network interface ---
set IP=localhost
for /f "usebackq delims=" %%i in (`powershell -NoProfile -Command "(Get-NetIPConfiguration | Where-Object { $_.IPv4DefaultGateway -ne $null -and $_.NetAdapter.Status -eq 'Up' } | Select-Object -First 1).IPv4Address.IPAddress"`) do set IP=%%i

echo ============================================
echo  Quizandar
echo ============================================
echo JRE:          %JAVA%
echo JAR:          %JAR%
echo Questions:    %QUESTIONS_DIR%
echo.
echo Host page:    http://%IP%:8080/
echo Player page:  http://%IP%:8080/#/player
echo ============================================
echo.

REM --- Open host page in browser after 15s (in background) ---
start "" powershell -NoProfile -WindowStyle Hidden -Command "Start-Sleep -Seconds 15; Start-Process 'http://%IP%:8080/'"

REM --- Start server (foreground) ---
"%JAVA%" -Dquizandar.questions.directory="%QUESTIONS_DIR%" -jar "%JAR%" --server.port=8080

pause

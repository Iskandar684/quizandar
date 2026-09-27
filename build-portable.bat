@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

REM ============================================================
REM  Build portable Quizandar for Windows (no Docker required)
REM ============================================================

REM --- Settings ---
set "PORTABLE_DIR=quizandar-portable"
set "APP_DIR=%PORTABLE_DIR%\app"
set "JRE_URL=https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.5%%2B11/OpenJDK21U-jre_x64_windows_hotspot_21.0.5_11.zip"
set "JRE_ZIP=jre21.zip"
set "JAR_SOURCE=backend\target\quizandar-backend-0.0.1-SNAPSHOT.jar"
set "JAR_DEST=quizandar.jar"
set "QUESTIONS_SOURCE=backend\src\main\resources\questions"
set "QUESTIONS_DEST=questions"
set "FRONTEND_DIST=frontend\dist"
set "BACKEND_STATIC=backend\src\main\resources\static"
set "TEMPLATE_DIR=portable-template"

REM --- Check template files ---
if not exist "%TEMPLATE_DIR%\start.bat"           ( echo ERROR: %TEMPLATE_DIR%\start.bat not found. & exit /b 1 )
if not exist "%TEMPLATE_DIR%\stop.bat"            ( echo ERROR: %TEMPLATE_DIR%\stop.bat not found. & exit /b 1 )
if not exist "%TEMPLATE_DIR%\make-shortcuts.bat"  ( echo ERROR: %TEMPLATE_DIR%\make-shortcuts.bat not found. & exit /b 1 )
if not exist "%TEMPLATE_DIR%\README.txt"          ( echo ERROR: %TEMPLATE_DIR%\README.txt not found. & exit /b 1 )
if not exist "%TEMPLATE_DIR%\icons\play.ico"      ( echo ERROR: %TEMPLATE_DIR%\icons\play.ico not found. & exit /b 1 )
if not exist "%TEMPLATE_DIR%\icons\stop.ico"      ( echo ERROR: %TEMPLATE_DIR%\icons\stop.ico not found. & exit /b 1 )

REM --- Clean previous build ---
if exist "%PORTABLE_DIR%" (
    echo Removing old portable build...
    rmdir /s /q "%PORTABLE_DIR%"
)
mkdir "%PORTABLE_DIR%"
mkdir "%APP_DIR%"
mkdir "%APP_DIR%\icons"

REM --- Build frontend ---
echo.
echo [1/6] Building frontend (npm install + build)...
pushd frontend
call npm install
if errorlevel 1 ( echo ERROR: npm install failed. & popd & exit /b 1 )
call npm run build
if errorlevel 1 ( echo ERROR: npm run build failed. & popd & exit /b 1 )
popd

if not exist "%FRONTEND_DIST%\index.html" (
    echo ERROR: frontend build output not found: %FRONTEND_DIST%\index.html
    exit /b 1
)

REM --- Copy frontend dist into backend resources ---
echo.
echo [2/6] Copying frontend build into backend resources...
if exist "%BACKEND_STATIC%" rmdir /s /q "%BACKEND_STATIC%"
mkdir "%BACKEND_STATIC%"
xcopy "%FRONTEND_DIST%" "%BACKEND_STATIC%\" /E /I /Y >nul

REM --- Build backend ---
echo.
echo [3/6] Building backend with Maven...
pushd backend
call mvn clean package -DskipTests
if errorlevel 1 ( echo ERROR: Maven build failed. & popd & exit /b 1 )
popd

if exist "%BACKEND_STATIC%" rmdir /s /q "%BACKEND_STATIC%"

if not exist "%JAR_SOURCE%" (
    echo ERROR: JAR not found after build: %JAR_SOURCE%
    exit /b 1
)

echo Copying JAR to app/...
copy "%JAR_SOURCE%" "%APP_DIR%\%JAR_DEST%" >nul

REM --- Copy questions to root ---
echo.
echo [4/6] Copying questions...
if exist "%QUESTIONS_SOURCE%" (
    xcopy "%QUESTIONS_SOURCE%" "%PORTABLE_DIR%\%QUESTIONS_DEST%\" /E /I /Y >nul
) else (
    mkdir "%PORTABLE_DIR%\%QUESTIONS_DEST%"
)

REM --- Download JRE to app/ ---
echo.
echo [5/6] Downloading JRE (Java 21)...
curl -L -o "%JRE_ZIP%" "%JRE_URL%"
if errorlevel 1 ( echo ERROR: Failed to download JRE. & exit /b 1 )

echo Extracting JRE into app/...
tar -xf "%JRE_ZIP%" -C "%APP_DIR%"
del "%JRE_ZIP%"

for /d %%D in ("%APP_DIR%\jdk-*" "%APP_DIR%\OpenJDK*") do (
    if exist "%%D" ren "%%D" "jre"
)
if not exist "%APP_DIR%\jre\bin\java.exe" (
    echo WARNING: Could not find app\jre\bin\java.exe. Check extracted folder name.
)

REM --- Copy launcher files to app/ ---
echo.
echo [6/6] Copying launcher files to app/...
copy /Y "%TEMPLATE_DIR%\start.bat"          "%APP_DIR%\start.bat"          >nul
copy /Y "%TEMPLATE_DIR%\stop.bat"           "%APP_DIR%\stop.bat"           >nul
copy /Y "%TEMPLATE_DIR%\make-shortcuts.bat" "%APP_DIR%\make-shortcuts.bat" >nul
copy /Y "%TEMPLATE_DIR%\icons\play.ico"     "%APP_DIR%\icons\play.ico"     >nul
copy /Y "%TEMPLATE_DIR%\icons\stop.ico"     "%APP_DIR%\icons\stop.ico"     >nul
copy /Y "%TEMPLATE_DIR%\README.txt"         "%PORTABLE_DIR%\README.txt"    >nul

REM --- Create shortcuts in root ---
echo Creating shortcuts in root...
call "%APP_DIR%\make-shortcuts.bat"

REM --- Rename shortcuts to Russian names (via PowerShell, using .NET which handles Unicode well) ---
powershell -NoProfile -ExecutionPolicy Bypass -Command "if (Test-Path '%PORTABLE_DIR%\start.lnk') { Rename-Item -LiteralPath '%PORTABLE_DIR%\start.lnk' -NewName 'Запустить Quizandar.lnk' }; if (Test-Path '%PORTABLE_DIR%\stop.lnk') { Rename-Item -LiteralPath '%PORTABLE_DIR%\stop.lnk' -NewName 'Остановить Quizandar.lnk' }"

echo.
echo ============================================================
echo  Done! Portable build is in folder: %PORTABLE_DIR%
echo  In root: questions/, 2 shortcuts, app/
echo ============================================================

endlocal

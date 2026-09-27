@echo off
setlocal enabledelayedexpansion
chcp 65001 >nul

REM ============================================================
REM  Build portable Quizandar for Windows (no Docker required)
REM ============================================================

REM --- Settings ---
set "PORTABLE_DIR=quizandar-portable"
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
if not exist "%TEMPLATE_DIR%\start.bat" (
    echo ERROR: %TEMPLATE_DIR%\start.bat not found.
    exit /b 1
)
if not exist "%TEMPLATE_DIR%\README.txt" (
    echo ERROR: %TEMPLATE_DIR%\README.txt not found.
    exit /b 1
)

REM --- Clean previous build ---
if exist "%PORTABLE_DIR%" (
    echo Removing old portable build...
    rmdir /s /q "%PORTABLE_DIR%"
)
mkdir "%PORTABLE_DIR%"

REM --- Build frontend ---
echo.
echo [1/6] Building frontend (npm install + build)...
pushd frontend
call npm install
if errorlevel 1 (
    echo ERROR: npm install failed.
    popd
    exit /b 1
)
call npm run build
if errorlevel 1 (
    echo ERROR: npm run build failed.
    popd
    exit /b 1
)
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

REM --- Build backend (jar with embedded frontend) ---
echo.
echo [3/6] Building backend with Maven...
pushd backend
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed.
    popd
    exit /b 1
)
popd

REM Cleanup: remove static so it doesn't pollute the repo
if exist "%BACKEND_STATIC%" rmdir /s /q "%BACKEND_STATIC%"

if not exist "%JAR_SOURCE%" (
    echo ERROR: JAR not found after build: %JAR_SOURCE%
    exit /b 1
)

echo.
echo Copying JAR...
copy "%JAR_SOURCE%" "%PORTABLE_DIR%\%JAR_DEST%" >nul

REM --- Copy questions ---
echo.
echo [4/6] Copying questions...
if exist "%QUESTIONS_SOURCE%" (
    xcopy "%QUESTIONS_SOURCE%" "%PORTABLE_DIR%\%QUESTIONS_DEST%\" /E /I /Y >nul
) else (
    mkdir "%PORTABLE_DIR%\%QUESTIONS_DEST%"
)

REM --- Download JRE ---
echo.
echo [5/6] Downloading JRE (Java 21)...
curl -L -o "%JRE_ZIP%" "%JRE_URL%"
if errorlevel 1 (
    echo ERROR: Failed to download JRE.
    exit /b 1
)

echo Extracting JRE...
tar -xf "%JRE_ZIP%" -C "%PORTABLE_DIR%"
del "%JRE_ZIP%"

REM Rename extracted folder to "jre"
for /d %%D in ("%PORTABLE_DIR%\jdk-*" "%PORTABLE_DIR%\OpenJDK*") do (
    if exist "%%D" ren "%%D" "jre"
)
if not exist "%PORTABLE_DIR%\jre\bin\java.exe" (
    echo WARNING: Could not find jre\bin\java.exe. Check extracted folder name.
)
if not exist "%TEMPLATE_DIR%\stop.bat" (
    echo ERROR: %TEMPLATE_DIR%\stop.bat not found.
    exit /b 1
)

REM --- Copy portable launcher files ---
echo.
echo [6/6] Copying portable launcher files...
copy /Y "%TEMPLATE_DIR%\start.bat" "%PORTABLE_DIR%\start.bat" >nul
copy /Y "%TEMPLATE_DIR%\README.txt" "%PORTABLE_DIR%\README.txt" >nul
copy /Y "%TEMPLATE_DIR%\stop.bat" "%PORTABLE_DIR%\stop.bat" >nul

echo.
echo ============================================================
echo  Done! Portable build is in folder: %PORTABLE_DIR%
echo  Run: %PORTABLE_DIR%\start.bat
echo ============================================================

endlocal
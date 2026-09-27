@echo off
setlocal
set "APPDIR=%~dp0"
for %%I in ("%APPDIR%..") do set "ROOT=%%~fI"

powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=New-Object -ComObject WScript.Shell; $l=$s.CreateShortcut('%ROOT%\start.lnk'); $l.TargetPath='%APPDIR%start.bat'; $l.WorkingDirectory='%APPDIR%'; $l.IconLocation='%APPDIR%icons\play.ico'; $l.Save()"

powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=New-Object -ComObject WScript.Shell; $l=$s.CreateShortcut('%ROOT%\stop.lnk'); $l.TargetPath='%APPDIR%stop.bat'; $l.WorkingDirectory='%APPDIR%'; $l.IconLocation='%APPDIR%icons\stop.ico'; $l.Save()"

echo Shortcuts created in %ROOT%
endlocal

@echo off
setlocal

set "ROOT=%~dp0"

REM --- Shortcut: Start ---
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=New-Object -ComObject WScript.Shell; $l=$s.CreateShortcut('%ROOT%start.lnk'); $l.TargetPath='%ROOT%start.bat'; $l.WorkingDirectory='%ROOT%'; $l.IconLocation='%ROOT%icons\play.ico'; $l.Description='Start Quizandar'; $l.Save()"

REM --- Shortcut: Stop ---
powershell -NoProfile -ExecutionPolicy Bypass -Command "$s=New-Object -ComObject WScript.Shell; $l=$s.CreateShortcut('%ROOT%stop.lnk'); $l.TargetPath='%ROOT%stop.bat'; $l.WorkingDirectory='%ROOT%'; $l.IconLocation='%ROOT%icons\stop.ico'; $l.Description='Stop Quizandar'; $l.Save()"

echo Shortcuts created.
pause
endlocal

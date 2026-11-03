@echo off
REM Checks that Logging, Core and UI can start and talk to each other on this machine.
cd /d "%~dp0.."
if not exist out mkdir out
javac -encoding UTF-8 -d out -cp lib\jna-5.15.0.jar src\cpu_core\*.java src\memory\*.java src\logging\*.java src\gui\*.java src\app\*.java || exit /b 1
java -cp out;lib\jna-5.15.0.jar app.Diagnose %*
pause

@echo off
REM Compiles the simulator and starts the 3 processes (Logging, Core, UI) via app.Launcher.
REM   run_all.bat             Swing GUI
REM   run_all.bat --console   text-mode UI (no window), prints the FCFS run
REM Ports: add -Dsim.corePort=... -Dsim.logPort=... to the java command if 6060/6061 are taken.
cd /d "%~dp0.."
if not exist out mkdir out
javac -encoding UTF-8 -d out -cp lib\jna-5.15.0.jar src\cpu_core\*.java src\memory\*.java src\logging\*.java src\gui\*.java src\app\*.java || exit /b 1
java -cp out;lib\jna-5.15.0.jar app.Launcher %*

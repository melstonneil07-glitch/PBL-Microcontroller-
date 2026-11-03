#!/bin/sh
# Compiles the simulator and starts the 3 processes (Logging, Core, UI) via app.Launcher.
#   scripts/run_all.sh             Swing GUI
#   scripts/run_all.sh --console   text-mode UI (no window), prints the FCFS run
# Ports: JAVA_OPTS="-Dsim.corePort=7000 -Dsim.logPort=7001" scripts/run_all.sh
cd "$(dirname "$0")/.." || exit 1
mkdir -p out
javac -encoding UTF-8 -d out -cp lib/jna-5.15.0.jar src/cpu_core/*.java src/memory/*.java src/logging/*.java src/gui/*.java src/app/*.java || exit 1
exec java $JAVA_OPTS -cp out:lib/jna-5.15.0.jar app.Launcher "$@"

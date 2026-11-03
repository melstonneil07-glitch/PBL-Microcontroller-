#!/bin/sh
# Checks that Logging, Core and UI can start and talk to each other on this machine.
cd "$(dirname "$0")/.." || exit 1
mkdir -p out
javac -encoding UTF-8 -d out -cp lib/jna-5.15.0.jar src/cpu_core/*.java src/memory/*.java src/logging/*.java src/gui/*.java src/app/*.java || exit 1
exec java $JAVA_OPTS -cp out:lib/jna-5.15.0.jar app.Diagnose "$@"

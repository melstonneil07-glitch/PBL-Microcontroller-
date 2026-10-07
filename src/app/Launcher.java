package app;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Starts the simulator as three separate OS processes (three JVMs) and waits for the UI:
 *
 *     Logging process  <--TCP--  Core process  <--TCP--  UI process
 *                          ^------------TCP-------------------|
 *
 * Usage: java -cp out[;lib/jna-5.15.0.jar] app.Launcher [--console] [--log file]
 *   --console   run the text-mode UI (gui.ConsoleUI) instead of the Swing window
 * Ports can be changed with -Dsim.corePort=... -Dsim.logPort=... (passed on to all three).
 * Start order is Logging, Core, UI; each process's output is shown with a [TAG] prefix.
 */
public class Launcher {

    public static void main(String[] args) throws Exception {
        boolean console = false;
        String logFile = "logs.txt";
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--console")) console = true;
            else if (args[i].equals("--log") && i + 1 < args.length) logFile = args[++i];
        }
        String corePort = System.getProperty("sim.corePort", String.valueOf(cpu_core.CoreProcess.DEFAULT_CORE_PORT));
        String logPort = System.getProperty("sim.logPort", String.valueOf(logging.LoggingServer.DEFAULT_PORT));

        List<Process> started = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> stopAll(started), "Launcher-Shutdown"));

        // 1. Logging process: must be listening before the others start sending.
        Process logger = start("LOGGER", "logging.LoggingServer", "Listening on", 15, started,
                new String[]{}, logPort, logFile);
        // 2. Core process
        Process core = start("CORE", "cpu_core.CoreProcess", "Core is READY", 15, started,
                new String[]{"-Dsim.corePort=" + corePort, "-Dsim.logPort=" + logPort});
        // 3. UI process
        Process ui = start("UI", console ? "gui.ConsoleUI" : "gui.MainWindow", null, 0, started,
                new String[]{"-Dsim.corePort=" + corePort, "-Dsim.logPort=" + logPort});

        System.out.println("[LAUNCHER] pids logging=" + logger.pid() + " core=" + core.pid() + " ui=" + ui.pid());
        int code = ui.waitFor();
        System.out.println("[LAUNCHER] UI exited with code " + code + "; stopping Core and Logging");
        stopAll(started);
        System.exit(code);
    }

    // Starts one child JVM, forwards its output with a tag, and (optionally) waits for a ready marker.
    private static Process start(String tag, String mainClass, String readyMarker, int timeoutSeconds,
                                 List<Process> started, String[] jvmArgs, String... programArgs) throws Exception {
        List<String> cmd = new ArrayList<>();
        cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-cp");
        cmd.add(ServiceSpawner.classPath());
        for (String a : jvmArgs) cmd.add(a);
        cmd.add(mainClass);
        for (String a : programArgs) cmd.add(a);

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        started.add(p);
        CountDownLatch ready = new CountDownLatch(1);
        Thread pump = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[" + tag + "] " + line);
                    if (readyMarker != null && line.contains(readyMarker)) ready.countDown();
                }
            } catch (IOException ignored) {
            }
        }, "pump-" + tag);
        pump.setDaemon(true);
        pump.start();

        if (readyMarker != null && !ready.await(timeoutSeconds, TimeUnit.SECONDS)) {
            System.err.println("[LAUNCHER] " + tag + " did not become ready within " + timeoutSeconds
                    + "s (is its port already in use?)");
            stopAll(started);
            System.exit(3);
        }
        return p;
    }

    private static void stopAll(List<Process> processes) {
        for (Process p : processes) p.destroy();                 // SIGTERM: Core flushes its log queue
        for (Process p : processes) {
            try {
                if (!p.waitFor(3, TimeUnit.SECONDS)) p.destroyForcibly();
            } catch (InterruptedException ignored) {
            }
        }
    }
}

package app;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Lets the UI process start the Core and Logging processes itself when it finds them not running
 * (for example when MainWindow is started straight from an IDE). Each is a separate JVM with the
 * same classpath, output shown in the UI's console, and is stopped again when the UI closes.
 */
public final class ServiceSpawner {

    private final List<Process> children = new ArrayList<>();
    private Process core;
    private Process logger;
    private final int corePort;
    private final int logPort;
    private String logFile = System.getProperty("sim.logFile", "simulator.log");

    public ServiceSpawner(int corePort, int logPort) {
        this.corePort = corePort;
        this.logPort = logPort;
    }

    public void setLogFile(String path) { this.logFile = path; }

    public synchronized boolean startLogger() throws IOException {
        if (logger != null) return false;
        logger = spawn("logging.LoggingServer", new String[0], String.valueOf(logPort), logFile);
        return true;
    }

    public synchronized boolean startCore() throws IOException {
        if (core != null) return false;
        core = spawn("cpu_core.CoreProcess",
                new String[]{"-Dsim.corePort=" + corePort, "-Dsim.logPort=" + logPort});
        return true;
    }

    /** True if we started the Core and it has already died (e.g. its port is taken by something else). */
    public synchronized boolean coreDied() { return core != null && !core.isAlive(); }

    public synchronized int coreExitCode() { return core == null || core.isAlive() ? -1 : core.exitValue(); }

    public synchronized void stopAll() {
        for (Process p : children) p.destroy();            // Core's shutdown hook flushes its log queue
        for (Process p : children) {
            try {
                if (!p.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) p.destroyForcibly();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        children.clear();
    }

    private Process spawn(String mainClass, String[] jvmArgs, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-cp");
        cmd.add(System.getProperty("java.class.path"));
        for (String a : jvmArgs) cmd.add(a);
        cmd.add(mainClass);
        for (String a : args) cmd.add(a);
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        children.add(p);
        return p;
    }
}

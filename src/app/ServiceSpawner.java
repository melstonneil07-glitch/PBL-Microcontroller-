package app;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
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
    private final ArrayDeque<String> coreTail = new ArrayDeque<>();   // last lines the Core process printed
    private Process core;
    private Process logger;
    private final int corePort;
    private final int logPort;
    private String logFile = System.getProperty("sim.logFile", "logs.txt");

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
                new String[]{"-Dsim.corePort=" + corePort, "-Dsim.logPort=" + logPort}, coreTail);
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

    /** Last few lines the Core process printed (shown in the UI log when it dies). */
    public synchronized String coreOutputTail() {
        return String.join(" / ", coreTail);
    }

    private Process spawn(String mainClass, String[] jvmArgs, String... args) throws IOException {
        return spawn(mainClass, jvmArgs, null, args);
    }

    private Process spawn(String mainClass, String[] jvmArgs, ArrayDeque<String> tail, String... args) throws IOException {
        List<String> cmd = new ArrayList<>();
        cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
        cmd.add("-cp");
        cmd.add(classPath());
        for (String a : jvmArgs) cmd.add(a);
        cmd.add(mainClass);
        for (String a : args) cmd.add(a);
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        children.add(p);
        String tag = mainClass.substring(mainClass.lastIndexOf('.') + 1);
        Thread pump = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    System.out.println("[" + tag + "] " + line);
                    if (tail != null) synchronized (this) {
                        tail.addLast(line);
                        while (tail.size() > 6) tail.removeFirst();
                    }
                }
            } catch (IOException ignored) {
            }
        }, "pump-" + tag);
        pump.setDaemon(true);
        pump.start();
        return p;
    }

    /**
     * Classpath for child JVMs. java.class.path alone is unreliable (IDEs may use a temporary
     * manifest jar, relative entries or the module path), so first add the absolute location each
     * simulator class was actually loaded from, then every entry the UI itself was started with.
     */
    public static String classPath() {
        LinkedHashSet<String> parts = new LinkedHashSet<>();
        Class<?>[] anchors = {ServiceSpawner.class, cpu_core.CoreProcess.class,
                logging.LoggingServer.class, gui.TCPClient.class};
        for (Class<?> c : anchors) {
            try {
                parts.add(new File(c.getProtectionDomain().getCodeSource().getLocation().toURI()).getAbsolutePath());
            } catch (Exception ignored) {
                // no code source (unusual loader): fall back to the property below
            }
        }
        for (String property : new String[]{"java.class.path", "jdk.module.path"}) {
            String value = System.getProperty(property, "");
            for (String entry : value.split(File.pathSeparator)) {
                if (!entry.isEmpty()) parts.add(new File(entry).getAbsolutePath());
            }
        }
        return String.join(File.pathSeparator, parts);
    }
}

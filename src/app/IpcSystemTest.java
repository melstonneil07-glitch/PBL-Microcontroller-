package app;

import gui.TCPClient;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;

/**
 * IPC test cases for the three-process simulator. Every case starts REAL separate JVM processes
 * (Logging, Core, UI) and talks to them over TCP.
 *   IPC01  full system via Launcher: three distinct PIDs, FCFS results, log file written
 *   IPC02  start-order independence: UI first, then Core, then Logging (retry + queued logs)
 *   IPC03  UI can disconnect and reconnect to the same Core (state reset, no restart needed)
 *   IPC04  Core serves pipelined UI requests first-come-first-served
 * Run: java -cp out app.IpcSystemTest
 */
public class IpcSystemTest {

    private static final String CP = System.getProperty("java.class.path");
    private static int total = 0, passed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("Test    Description                                         Status");
        System.out.println("---------------------------------------------------------------------");
        Path dir = Files.createTempDirectory("ipctest");
        try {
            testFullSystem(dir);
            testStartOrder(dir);
            testReconnectAndFifo(dir);
            testAutoStart();
        } finally {
            for (Proc p : ALL) p.stop();
        }
        System.out.println("---------------------------------------------------------------------");
        System.out.println(passed + " / " + total + " IPC system test cases passed.");
        System.exit(passed == total ? 0 : 1);
    }

    // ---------------- scenarios ----------------

    private static void testFullSystem(Path dir) throws Exception {
        Path log = dir.resolve("full.log");
        Proc launcher = Proc.start("launcher", "app.Launcher", new String[]{"-Dsim.corePort=6260", "-Dsim.logPort=6261"},
                "--console", "--log", log.toString());
        boolean exited = launcher.waitFor(90);
        String out = launcher.output();
        check("IPC01a", "Launcher run finishes with exit code 0", exited && launcher.exitCode() == 0);

        Matcher m = Pattern.compile("pids logging=(\\d+) core=(\\d+) ui=(\\d+)").matcher(out);
        boolean distinct = m.find() && !m.group(1).equals(m.group(2)) && !m.group(2).equals(m.group(3))
                && !m.group(1).equals(m.group(3));
        check("IPC01b", "Logging, Core and UI are 3 distinct OS processes", distinct);

        check("IPC01c", "FCFS results arrive at the UI (finish 16/23/31)",
                out.contains("RESULT P1 state=TERMINATED arrival=0 start=0 finish=16")
                && out.contains("RESULT P2 state=TERMINATED arrival=2 start=16 finish=23")
                && out.contains("RESULT P3 state=TERMINATED arrival=4 start=23 finish=31"));

        String logText = Files.exists(log) ? new String(Files.readAllBytes(log), StandardCharsets.UTF_8) : "";
        check("IPC01d", "Log file has entries from both CORE and UI",
                logText.contains("[CORE]") && logText.contains("[UI]"));
        check("IPC01e", "Log file records 3 TERMINATE and 2 CONTEXT_SWITCH events",
                count(logText, "SCHED TERMINATE") == 3 && count(logText, "SCHED CONTEXT_SWITCH") == 2);
    }

    private static void testStartOrder(Path dir) throws Exception {
        Path log = dir.resolve("order.log");
        String[] ports = {"-Dsim.corePort=6262", "-Dsim.logPort=6263"};
        Proc ui = Proc.start("ui-first", "gui.ConsoleUI", ports);           // nothing is running yet
        Thread.sleep(2000);
        Proc core = Proc.start("core", "cpu_core.CoreProcess", ports);
        Thread.sleep(2500);
        Proc logger = Proc.start("logger", "logging.LoggingServer", new String[0], "6263", log.toString());
        boolean exited = ui.waitFor(60);
        check("IPC02a", "UI started first still connects and completes", exited && ui.exitCode() == 0);
        Thread.sleep(1500);   // let the Core's forwarder deliver
        String logText = Files.exists(log) ? new String(Files.readAllBytes(log), StandardCharsets.UTF_8) : "";
        check("IPC02b", "Core logs queued before the Logger existed are delivered",
                logText.contains("Loaded 3 process(es)") && logText.contains("SCHED TERMINATE PID=3"));
        core.stop();
        logger.stop();
    }

    private static void testReconnectAndFifo(Path dir) throws Exception {
        String[] ports = {"-Dsim.corePort=6264", "-Dsim.logPort=6265"};
        Proc logger = Proc.start("logger2", "logging.LoggingServer", new String[0], "6265", dir.resolve("r.log").toString());
        logger.awaitOutput("Listening on", 15);
        Proc core = Proc.start("core2", "cpu_core.CoreProcess", ports);
        core.awaitOutput("Core is READY", 15);

        // session 1
        Client c1 = Client.connect(6264);
        check("IPC03a", "Core sends CORE_READY on connect", c1.next(5) != null && c1.last.startsWith("CORE_READY"));
        c1.send("STEP");
        String reply = c1.next(5);
        check("IPC03b", "STEP before LOAD returns a clear ERROR", reply != null && reply.startsWith("ERROR|No processes loaded"));
        c1.send(gui.DemoWorkload.loadCommand());
        check("IPC03c", "LOADPROCS acknowledged with LOADED", c1.nextStartingWith("LOADED", 5) != null);
        c1.close();

        // session 2 on the same Core process
        Client c2 = Client.connect(6264);
        check("IPC03d", "UI can reconnect to the same Core (CORE_READY again)",
                c2.next(5) != null && c2.last.startsWith("CORE_READY"));
        c2.send("STEP");
        reply = c2.next(5);
        check("IPC03e", "Core state was reset for the new session", reply != null && reply.startsWith("ERROR|No processes loaded"));

        // FIFO: fire 6 STEPs without waiting for replies
        c2.send(gui.DemoWorkload.loadCommand());
        c2.nextStartingWith("PROCS", 5);
        for (int i = 0; i < 6; i++) c2.send("STEP");
        List<String> clocks = new ArrayList<>();
        List<String> stepReplies = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10000;
        while (clocks.size() < 6 && System.currentTimeMillis() < deadline) {
            String m = c2.next(2);
            if (m != null && m.startsWith("STEP|")) { clocks.add(field(m, "CLOCK")); stepReplies.add(m); }
        }
        check("IPC04", "6 pipelined STEPs are served in order (CLOCK 1..6)",
                clocks.equals(Arrays.asList("1", "2", "3", "4", "5", "6")));
        check("IPC04b", "Data memory reaches the UI (MOV 30H,A -> MEMORY=48:13)",
                stepReplies.size() == 6 && "".equals(field(stepReplies.get(2), "MEMORY"))
                        && "48:13".equals(field(stepReplies.get(3), "MEMORY")));
        c2.send("SHUTDOWN");
        c2.close();
        core.waitFor(10);
        logger.stop();
    }

    private static void testAutoStart() throws Exception {
        TCPClient probe = new TCPClient("127.0.0.1", 6266, 6267);
        check("IPC05a", "Nothing running: connect fails with a reason", !probe.connectCore() && !probe.getLastError().isEmpty());
        ServiceSpawner spawner = new ServiceSpawner(6266, 6267);
        spawner.setLogFile(Files.createTempFile("spawn", ".log").toString());
        spawner.startLogger();
        spawner.startCore();
        Client c = Client.connect(6266);
        check("IPC05b", "Spawned Core accepts the UI (CORE_READY)", c.next(10) != null && c.last.startsWith("CORE_READY"));
        c.send("PING");
        check("IPC05c", "Spawned Core answers PING", "PONG".equals(c.next(5)));
        c.close();
        spawner.stopAll();
        Thread.sleep(500);
        TCPClient after = new TCPClient("127.0.0.1", 6266, 6267);
        check("IPC05d", "stopAll() terminates the spawned Core", !after.connectCore());
    }

    // ---------------- helpers ----------------

    private static void check(String id, String description, boolean ok) {
        total++;
        if (ok) passed++;
        System.out.printf("%-7s %-51s %s%n", id, description, ok ? "PASS" : "FAIL");
    }

    private static int count(String text, String needle) {
        int n = 0, i = 0;
        while ((i = text.indexOf(needle, i)) >= 0) { n++; i += needle.length(); }
        return n;
    }

    private static String field(String message, String key) {
        for (String part : message.split("\\|")) if (part.startsWith(key + "=")) return part.substring(key.length() + 1);
        return null;
    }

    private static final List<Proc> ALL = new ArrayList<>();

    /** A child JVM with its output captured. */
    private static final class Proc {
        final Process process;
        final StringBuffer out = new StringBuffer();

        private Proc(Process p) { this.process = p; }

        static Proc start(String name, String mainClass, String[] jvmArgs, String... args) throws IOException {
            List<String> cmd = new ArrayList<>();
            cmd.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
            cmd.add("-cp"); cmd.add(ServiceSpawner.classPath());
            cmd.addAll(Arrays.asList(jvmArgs));
            cmd.add(mainClass);
            cmd.addAll(Arrays.asList(args));
            Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            Proc proc = new Proc(p);
            Thread t = new Thread(() -> {
                try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = r.readLine()) != null) proc.out.append(line).append('\n');
                } catch (IOException ignored) { }
            }, "capture-" + name);
            t.setDaemon(true);
            t.start();
            ALL.add(proc);
            return proc;
        }

        boolean waitFor(int seconds) throws InterruptedException { return process.waitFor(seconds, TimeUnit.SECONDS); }
        int exitCode() { return process.exitValue(); }
        String output() { return out.toString(); }

        boolean awaitOutput(String text, int seconds) throws InterruptedException {
            long end = System.currentTimeMillis() + seconds * 1000L;
            while (System.currentTimeMillis() < end) {
                if (out.indexOf(text) >= 0) return true;
                Thread.sleep(100);
            }
            return false;
        }

        void stop() {
            process.destroy();
            try { if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly(); } catch (InterruptedException ignored) { }
        }
    }

    /** UI-side client using the same TCPClient as the real UI. */
    private static final class Client {
        final TCPClient tcp;
        final BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        String last;

        private Client(int port) {
            tcp = new TCPClient("127.0.0.1", port, 1);
            tcp.setMessageListener(new TCPClient.MessageListener() {
                @Override public void onCoreMessage(String m) { inbox.add(m); }
                @Override public void onConnectionChanged(boolean c) { }
            });
        }

        static Client connect(int port) throws InterruptedException {
            Client c = new Client(port);
            for (int i = 0; i < 40 && !c.tcp.connectCore(); i++) Thread.sleep(250);
            return c;
        }

        void send(String m) { tcp.sendToCore(m); }

        String next(int seconds) throws InterruptedException {
            last = inbox.poll(seconds, TimeUnit.SECONDS);
            return last;
        }

        String nextStartingWith(String prefix, int seconds) throws InterruptedException {
            long end = System.currentTimeMillis() + seconds * 1000L;
            while (System.currentTimeMillis() < end) {
                String m = next(1);
                if (m != null && m.startsWith(prefix)) return m;
            }
            return null;
        }

        void close() { tcp.close(); }
    }
}

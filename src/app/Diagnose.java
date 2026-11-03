package app;

import gui.DemoWorkload;
import gui.TCPClient;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * One-command connectivity check for the three simulator processes. It looks at the environment and
 * the ports, then starts the Logging and Core processes itself (their console output is shown, so a
 * startup crash is visible), connects to them as the UI would, runs a few steps and prints PASS/FAIL.
 * Run: java -cp out app.Diagnose      (or scripts/diagnose.bat / scripts/diagnose.sh)
 */
public class Diagnose {

    private static boolean allOk = true;
    private static java.nio.file.Path LOG_PATH;

    public static void main(String[] args) throws Exception {
        int corePort = Integer.getInteger("sim.corePort", cpu_core.CoreProcess.DEFAULT_CORE_PORT);
        int logPort = Integer.getInteger("sim.logPort", logging.LoggingServer.DEFAULT_PORT);

        System.out.println("== Environment");
        System.out.println("java.version = " + System.getProperty("java.version") + "   os = " + System.getProperty("os.name"));
        System.out.println("working dir  = " + new File(".").getCanonicalPath());
        System.out.println("classpath    = " + System.getProperty("java.class.path"));
        System.out.println("ports        = Core " + corePort + ", Logging " + logPort);

        System.out.println("\n== 1. Classes");
        for (String c : new String[]{"cpu_core.CoreProcess", "logging.LoggingServer", "gui.TCPClient", "app.ServiceSpawner"}) {
            try { Class.forName(c, false, Diagnose.class.getClassLoader()); report(true, c + " found"); }
            catch (ClassNotFoundException e) { report(false, c + " MISSING - recompile the whole project"); }
        }

        System.out.println("\n== 2. Ports before starting anything");
        boolean coreBusy = probe("Core", corePort);
        boolean logBusy = probe("Logging", logPort);
        if (probe("OLD Core build", 5000)) {
            System.out.println("   !! something listens on 5000: an OLD Core build is probably still running. Stop it.");
        }
        if (coreBusy || logBusy) {
            System.out.println("   Ports already in use: stop those processes (or use -Dsim.corePort/-Dsim.logPort) and run this again.");
            System.exit(2);
        }

        System.out.println("\n== 3. Start Logging + Core as separate processes (their output follows)");
        ServiceSpawner spawner = new ServiceSpawner(corePort, logPort);
        java.nio.file.Path logPath = Files.createTempFile("diagnose", ".log");
        spawner.setLogFile(logPath.toString());
        LOG_PATH = logPath;
        try {
            spawner.startLogger();
            spawner.startCore();
            selfTest(spawner, corePort, logPort);
        } finally {
            spawner.stopAll();
        }
        System.out.println("\n== RESULT: " + (allOk
                ? "ALL CHECKS PASSED - UI, Core and Logging connect and work on this machine."
                : "SOME CHECKS FAILED - read the first FAIL line above and any error text printed by the Core."));
        System.exit(allOk ? 0 : 1);
    }

    private static void selfTest(ServiceSpawner spawner, int corePort, int logPort) throws Exception {
        BlockingQueue<String> inbox = new LinkedBlockingQueue<>();
        TCPClient ui = new TCPClient("127.0.0.1", corePort, logPort);
        ui.setMessageListener(new TCPClient.MessageListener() {
            @Override public void onCoreMessage(String m) { inbox.add(m); }
            @Override public void onConnectionChanged(boolean c) { }
        });
        System.out.println("\n== 4. UI -> Core");
        boolean connected = false;
        for (int i = 0; i < 40 && !connected; i++) {
            connected = ui.connectCore();
            if (!connected) {
                if (spawner.coreDied()) break;
                Thread.sleep(500);
            }
        }
        if (!connected) {
            report(false, "UI could not connect to Core on 127.0.0.1:" + corePort + " (" + ui.getLastError() + ")"
                    + (spawner.coreDied() ? "; the Core process exited with code " + spawner.coreExitCode() + " - see its output above" : ""));
            return;
        }
        report(true, "TCP connection to Core established");
        String m = next(inbox, "CORE_READY", 5);
        report(m != null, m != null ? "Core said hello: " + m : "Core did not send CORE_READY");
        ui.sendToCore(DemoWorkload.loadCommand());
        report(next(inbox, "LOADED", 5) != null, "LOAD acknowledged (program loaded)");
        String step = null;
        for (int i = 0; i < 3; i++) { ui.sendToCore("STEP"); step = next(inbox, "STEP", 5); }
        report(step != null, step != null ? "STEP executed by the CPU: " + step.replaceAll("\\|STACK=.*", "") : "no reply to STEP");

        System.out.println("\n== 5. Core -> Logging");
        Thread.sleep(1500);
        report(ui.connectLogger(), "UI can connect to the Logging process on port " + logPort);
        ui.sendLog("diagnose: UI log entry");
        Thread.sleep(500);
        ui.sendToCore("SHUTDOWN");
        Thread.sleep(1500);
        String logText = new String(Files.readAllBytes(LOG_PATH), StandardCharsets.UTF_8);
        report(logText.contains("[CORE]") && logText.contains("SCHED DISPATCH"),
                "Core -> Logging: Core entries reached the log file");
        report(logText.contains("[UI]") && logText.contains("diagnose: UI log entry"),
                "UI -> Logging: UI entry reached the log file");
        ui.close();
    }

    private static String next(BlockingQueue<String> inbox, String prefix, int seconds) throws InterruptedException {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            String m = inbox.poll(300, TimeUnit.MILLISECONDS);
            if (m != null && m.startsWith(prefix)) return m;
        }
        return null;
    }

    // true if something accepts connections on the port
    private static boolean probe(String name, int port) {
        try (Socket s = new Socket()) {
            s.connect(new InetSocketAddress("127.0.0.1", port), 800);
            s.setSoTimeout(700);
            String hello = "";
            try { hello = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8)).readLine(); }
            catch (IOException ignored) { }
            System.out.println("   port " + port + " (" + name + "): IN USE" + (hello != null && !hello.isEmpty() ? " - replies: " + hello : ""));
            return true;
        } catch (IOException e) {
            System.out.println("   port " + port + " (" + name + "): free");
            return false;
        }
    }

    private static void report(boolean ok, String text) {
        if (!ok) allOk = false;
        System.out.println("   " + (ok ? "PASS  " : "FAIL  ") + text);
    }
}

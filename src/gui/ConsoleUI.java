package gui;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Text-mode UI process. It talks to the Core and Logging processes through exactly the same
 * TCPClient the Swing window uses, so it exercises the real IPC path without needing a display.
 * Runs the demo FCFS workload to completion and exits 0 only if every process terminated.
 */
public class ConsoleUI {

    private static final BlockingQueue<String> INBOX = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws Exception {
        String host = "127.0.0.1";
        int corePort = Integer.getInteger("sim.corePort", cpu_core.CoreProcess.DEFAULT_CORE_PORT);
        int logPort = Integer.getInteger("sim.logPort", logging.LoggingServer.DEFAULT_PORT);

        TCPClient client = new TCPClient(host, corePort, logPort);
        client.setMessageListener(new TCPClient.MessageListener() {
            @Override public void onCoreMessage(String message) { INBOX.add(message); }
            @Override public void onConnectionChanged(boolean connected) { }
        });

        if (!retry(() -> client.connectCore(), 20)) fail("Core not reachable on port " + corePort);
        retry(() -> client.connectLogger(), 10);
        System.out.println("[UI] connected to Core:" + corePort + " (logger " + (client.isLoggerConnected() ? "connected" : "unavailable") + ")");
        if (await("CORE_READY") == null) fail("Core did not report ready");

        client.sendLog("ConsoleUI started, loading " + DemoWorkload.PROCESSES.length + " processes");
        client.sendToCore(DemoWorkload.loadCommand());
        if (await("LOADED") == null) fail("Core did not acknowledge LOAD");
        String procs = await("PROCS");

        int ticks = 0;
        boolean halted = false;
        while (!halted && ticks < 1000) {
            client.sendToCore("STEP");
            ticks++;
            while (true) {                                   // one tick = events, STEP/IDLE, then PROCS
                String m = INBOX.poll(5, TimeUnit.SECONDS);
                if (m == null) fail("Timed out waiting for reply to STEP " + ticks);
                if (m.startsWith("ERROR")) fail(m);
                if (m.startsWith("HALTED")) { halted = true; break; }
                if (m.startsWith("PROCS")) { procs = m; break; }
                print(m);
            }
        }

        // Summary
        int terminated = 0;
        Map<String, String> f = fields(procs);
        for (String row : f.getOrDefault("TABLE", "").split(";")) {
            String[] c = row.split(":");
            if (c.length < 9) continue;
            if (c[2].equals("TERMINATED")) terminated++;
            System.out.println("[UI] RESULT " + c[1] + " state=" + c[2] + " arrival=" + c[3] + " start=" + c[5]
                    + " finish=" + c[6] + " wait=" + c[7] + " tat=" + c[8]);
        }
        System.out.println("[UI] SUMMARY terminated=" + terminated + " ctx=" + f.get("CTX")
                + " avg_wait=" + f.get("AVG_WAIT") + " avg_tat=" + f.get("AVG_TAT") + " gantt=" + f.get("GANTT"));
        client.sendLog("ConsoleUI finished: " + terminated + " processes terminated");
        client.sendToCore("SHUTDOWN");                       // lets the Core flush its log queue and exit
        await("CORE_SHUTDOWN");
        Thread.sleep(300);
        client.close();
        System.exit(terminated == DemoWorkload.PROCESSES.length ? 0 : 1);
    }

    private static void print(String m) {
        Map<String, String> f = fields(m);
        String kind = m.substring(0, m.indexOf('|') < 0 ? m.length() : m.indexOf('|'));
        switch (kind) {
            case "STEP":
                System.out.println("[UI] t=" + f.get("CLOCK") + " " + f.get("PNAME") + " " + f.get("INSTRUCTION")
                        + "  A=" + f.get("A") + " CY=" + f.get("CY") + " OV=" + f.get("OV") + " SP=" + f.get("SP"));
                break;
            case "IDLE":
                System.out.println("[UI] t=" + f.get("CLOCK") + " CPU idle");
                break;
            default:
                System.out.println("[UI] " + m);
        }
    }

    private static String await(String prefix) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 10000;
        while (System.currentTimeMillis() < deadline) {
            String m = INBOX.poll(500, TimeUnit.MILLISECONDS);
            if (m != null && m.startsWith(prefix)) return m;
        }
        return null;
    }

    private static boolean retry(java.util.function.BooleanSupplier attempt, int seconds) throws InterruptedException {
        for (int i = 0; i < seconds * 2; i++) {
            if (attempt.getAsBoolean()) return true;
            Thread.sleep(500);
        }
        return false;
    }

    private static Map<String, String> fields(String message) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        String[] parts = message.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            int eq = parts[i].indexOf('=');
            if (eq > 0) map.put(parts[i].substring(0, eq), parts[i].substring(eq + 1));
        }
        return map;
    }

    private static void fail(String why) {
        System.err.println("[UI] FAILED: " + why);
        System.exit(2);
    }
}

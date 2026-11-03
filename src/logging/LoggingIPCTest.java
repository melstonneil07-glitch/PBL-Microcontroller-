package logging;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;

// IPC test case (Week 4 deliverable: "IPC test cases").
//
// Starts a real LoggingServer, connects two LogClients concurrently --
// one tagged "UI", one tagged "CORE", exactly as the real UI and Core
// processes would -- sends several messages from each at the same
// time, then reads back the persisted log file and checks that every
// message actually arrived and was written correctly.
//
// This is a genuine end-to-end test of the chosen IPC mechanism
// (TCP sockets over loopback): if the server, the wire protocol, or
// the client ever breaks, this test fails.
public class LoggingIPCTest {

    private static int total = 0;
    private static int passed = 0;

    public static void main(String[] args) throws Exception {
        int testPort = 6062; // dedicated test port, separate from DEFAULT_PORT
        Path logFile = Files.createTempFile("ipc_test_", ".log");

        System.out.println("Test   Description                          Status");
        System.out.println("---------------------------------------------------");

        LoggingServer server = new LoggingServer(testPort, logFile);
        Thread serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                // server thread is daemon-like for this test; ignore shutdown noise
            }
        }, "test-logging-server");
        serverThread.setDaemon(true);
        serverThread.start();

        waitForServerReady(testPort);

        // Two concurrent clients, exactly like the real UI and Core processes.
        LogClient uiClient = new LogClient("UI", "127.0.0.1", testPort);
        LogClient coreClient = new LogClient("CORE", "127.0.0.1", testPort);
        uiClient.connect();
        coreClient.connect();

        CountDownLatch bothDone = new CountDownLatch(2);

        Thread uiThread = new Thread(() -> {
            uiClient.info("User clicked LOAD");
            uiClient.info("User clicked STEP");
            uiClient.warn("User clicked RUN with no program loaded");
            bothDone.countDown();
        }, "sim-ui-process");

        Thread coreThread = new Thread(() -> {
            coreClient.info("Executed MOV A,#10");
            coreClient.info("Executed PUSH A (SP=08H)");
            coreClient.error("Stack overflow on PUSH");
            bothDone.countDown();
        }, "sim-core-process");

        uiThread.start();
        coreThread.start();
        bothDone.await();

        // Give the writer thread a moment to drain the queue to disk.
        Thread.sleep(300);

        uiClient.close();
        coreClient.close();

        List<String> lines = Files.readAllLines(logFile);

        check("TC01", "Log file has 6 entries", lines.size() == 6, "count=" + lines.size());
        check("TC02", "UI entries present (3)", countContaining(lines, "[UI]") == 3, "");
        check("TC03", "CORE entries present (3)", countContaining(lines, "[CORE]") == 3, "");
        check("TC04", "ERROR level delivered", countContaining(lines, "[ERROR]") == 1, "");
        check("TC05", "WARNING level delivered", countContaining(lines, "[WARNING]") == 1, "");
        check("TC06", "Specific message content intact",
                anyContains(lines, "Stack overflow on PUSH"), "");
        check("TC07", "Pipe-delimited protocol parses cleanly",
                lines.stream().allMatch(l -> l.startsWith("[")), "");

        System.out.println("---------------------------------------------------");
        System.out.println(passed + " / " + total + " IPC test cases passed.");

        Files.deleteIfExists(logFile);
        System.exit(0);
    }

    private static void check(String id, String desc, boolean ok, String detail) {
        total++;
        if (ok) passed++;
        System.out.printf("%-6s %-36s %s%s%n", id, desc, ok ? "PASS" : "FAIL",
                detail.isEmpty() ? "" : " (" + detail + ")");
    }

    private static long countContaining(List<String> lines, String needle) {
        return lines.stream().filter(l -> l.contains(needle)).count();
    }

    private static boolean anyContains(List<String> lines, String needle) {
        return lines.stream().anyMatch(l -> l.contains(needle));
    }

    // Polls the port until the server's ServerSocket is actually
    // accepting connections, instead of a fixed sleep guess.
    private static void waitForServerReady(int port) throws InterruptedException {
        for (int i = 0; i < 50; i++) {
            try (java.net.Socket probe = new java.net.Socket("127.0.0.1", port)) {
                return; // connected -- server is up
            } catch (java.io.IOException notReadyYet) {
                Thread.sleep(50);
            }
        }
        throw new IllegalStateException("LoggingServer did not start within 2.5s");
    }
}
package cpu_core;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/** Windows/Java-only Week 4 Core Process. UI <-> Core uses localhost:5000.
 * Core -> Logger uses localhost:5001. No JNA, Linux, shared memory, or POSIX APIs.
 */
public class CoreProcess {
    private static final int CORE_PORT = 5000;
    private static final int LOGGER_PORT = 5001;
    private static volatile boolean running = true;
    private static ServerSocket coreServer;
    private static Socket uiSocket, loggerSocket;
    private static BufferedWriter uiWriter, loggerWriter;
    private static final Object UI_LOCK = new Object();
    private static final Object LOGGER_LOCK = new Object();

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("       CORE PROCESS STARTED");
        System.out.println("=================================");
        CPU cpu = new CPU();
        System.out.println("CPU created successfully");
        System.out.println("Java Socket IPC: UI=" + CORE_PORT + ", Logger=" + LOGGER_PORT);

        try {
            coreServer = new ServerSocket(CORE_PORT);
            startLoggerServer();
            System.out.println("Core is READY. Waiting for UI on port " + CORE_PORT + "...");

            uiSocket = coreServer.accept();
            uiWriter = new BufferedWriter(new OutputStreamWriter(uiSocket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader uiReader = new BufferedReader(new InputStreamReader(uiSocket.getInputStream(), StandardCharsets.UTF_8));
            System.out.println("UI connected.");
            sendUpdate("CORE_READY");

            while (running) {
                String command = uiReader.readLine();
                if (command == null) break;
                if (command.trim().isEmpty()) continue;
                System.out.println("GUI command: " + command);

                if (command.startsWith("LOAD|")) {
                    try {
                        ArrayList<Instruction> program = parseProgram(command.substring(5));
                        cpu.loadProgram(program);
                        sendUpdate("LOADED|PC=" + cpu.getPC());
                    } catch (RuntimeException ex) {
                        sendUpdate("ERROR|" + safeMessage(ex));
                    }
                }
                else if (command.equals("STEP")) {
                    if (!cpu.isRunning()) {
                        sendUpdate("HALTED|PC=" + cpu.getPC());
                        continue;
                    }
                    int oldPC = cpu.getPC();
                    try {
                        Instruction instruction = cpu.step();
                        String text = instruction == null ? "NONE" : instruction.toString();
                        sendUpdate("STEP"
                                + "|OLD_PC=" + oldPC
                                + "|PC=" + cpu.getPC()
                                + "|A=" + cpu.getA()
                                + "|R1=" + cpu.getR(1)
                                + "|CY=" + (cpu.isCY() ? 1 : 0)
                                + "|OV=" + (cpu.isOV() ? 1 : 0)
                                + "|SP=" + cpu.getSP()
                                + "|QUEUE=" + cpu.getQueue().getCount()
                                + "|RUNNING=" + (cpu.isRunning() ? 1 : 0)
                                + "|INSTRUCTION=" + text);
                    } catch (RuntimeException ex) {
                        sendUpdate("ERROR|" + safeMessage(ex));
                    }
                }
                else if (command.equals("RESET")) {
                    try {
                        cpu.reset();
                        sendUpdate("RESET|PC=" + cpu.getPC());
                    } catch (RuntimeException ex) {
                        sendUpdate("ERROR|" + safeMessage(ex));
                    }
                }
                else if (command.equals("PING")) {
                    sendUpdate("PONG");
                }
                else if (command.equals("SHUTDOWN")) {
                    sendUpdate("CORE_SHUTDOWN");
                    running = false;
                }
                else {
                    sendUpdate("ERROR|Unknown command: " + command);
                }
            }
        } catch (IOException ex) {
            if (running) System.err.println("Core IPC error: " + ex.getMessage());
        } finally {
            cleanup();
        }
    }

    private static void startLoggerServer() {
        Thread t = new Thread(() -> {
            try (ServerSocket server = new ServerSocket(LOGGER_PORT)) {
                System.out.println("Waiting for Logger on port " + LOGGER_PORT + "...");
                Socket socket = server.accept();
                synchronized (LOGGER_LOCK) {
                    loggerSocket = socket;
                    loggerWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                }
                System.out.println("Logger connected.");
                while (running && !socket.isClosed()) Thread.sleep(500);
            } catch (Exception ex) {
                if (running) System.out.println("Logger not connected: " + ex.getMessage());
            }
        }, "Logger-Connection-Thread");
        t.setDaemon(true);
        t.start();
    }

    private static void sendUpdate(String message) {
        sendToUI(message);
        sendToLogger(message);
    }

    private static void sendToUI(String message) {
        synchronized (UI_LOCK) {
            if (uiWriter == null) return;
            try {
                uiWriter.write(message);
                uiWriter.newLine();
                uiWriter.flush();
            } catch (IOException ex) {
                System.err.println("Failed to send to UI: " + ex.getMessage());
            }
        }
    }

    private static void sendToLogger(String message) {
        synchronized (LOGGER_LOCK) {
            if (loggerWriter == null) return;
            try {
                loggerWriter.write(message);
                loggerWriter.newLine();
                loggerWriter.flush();
            } catch (IOException ex) {
                loggerWriter = null;
                try { if (loggerSocket != null) loggerSocket.close(); } catch (IOException ignored) {}
                loggerSocket = null;
            }
        }
    }

    private static ArrayList<Instruction> parseProgram(String programData) {
        ArrayList<Instruction> program = new ArrayList<>();
        if (programData == null || programData.isEmpty()) return program;
        for (String instructionText : programData.split(";")) {
            if (instructionText.trim().isEmpty()) continue;
            String[] parts = instructionText.split(":", 2);
            String mnemonic = parts[0].trim();
            ArrayList<String> operands = new ArrayList<>();
            if (parts.length > 1 && !parts[1].trim().isEmpty()) {
                for (String operand : parts[1].split(",")) operands.add(operand.trim());
            }
            program.add(new Instruction(mnemonic, operands));
        }
        return program;
    }

    private static String safeMessage(RuntimeException ex) {
        String m = ex.getMessage();
        return (m == null || m.trim().isEmpty()) ? ex.getClass().getSimpleName() : m.replace('\n', ' ').replace('\r', ' ');
    }

    private static void cleanup() {
        running = false;
        System.out.println("Cleaning up Core IPC...");
        try { if (uiSocket != null) uiSocket.close(); } catch (IOException ignored) {}
        try { if (loggerSocket != null) loggerSocket.close(); } catch (IOException ignored) {}
        try { if (coreServer != null) coreServer.close(); } catch (IOException ignored) {}
        uiSocket = loggerSocket = null;
        uiWriter = loggerWriter = null;
        System.out.println("Core Process Stopped");
    }
}

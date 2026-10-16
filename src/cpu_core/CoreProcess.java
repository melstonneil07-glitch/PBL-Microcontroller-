package cpu_core;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import logging.LogClient;
import logging.LogLevel;
import logging.LoggingServer;

/**
 * Week 4 Core Process: CPU + Memory/Stack/Queue + FCFS scheduler, served over TCP.
 *
 *   UI process --(TCP, 127.0.0.1:sim.corePort, line protocol)--> CORE process
 *   CORE process --(TCP, 127.0.0.1:sim.logPort, LogMessage protocol)--> LOGGING process
 *
 * Threads inside this process:
 *   main / worker      accepts a UI connection, then executes its commands one at a time
 *   Core-UI-Reader     reads command lines off the socket into a FIFO queue
 *   Core-Log-Forwarder drains a FIFO queue of log entries to the Logging process
 * Commands are served strictly first-come-first-served from the FIFO queue, so a slow
 * command never lets a later one overtake it, and the CPU/scheduler state is only ever
 * touched by the worker thread (no locking needed).
 *
 * Commands from the UI:
 *   LOADPROCS|NAME@ARRIVAL@PROGRAM|NAME@ARRIVAL@PROGRAM|...   replace the process table
 *   LOAD|PROGRAM                                              single process "P1" (compat)
 *   STEP      advance the system clock by one tick
 *   RESET     return every process to NEW, clock to 0
 *   PING / SHUTDOWN
 * Replies are single lines "KIND|KEY=VALUE|KEY=VALUE...".
 * Scheduler events (ADMIT, DISPATCH, CONTEXT_SWITCH, TERMINATE) are sent before the STEP
 * reply of the tick that caused them; a PROCS line with the process table, ready queue and
 * Gantt data follows every change.
 */
public class CoreProcess {
    public static final int DEFAULT_CORE_PORT = 6060;
    private static final String LOCALHOST = "127.0.0.1";
    private static final String EOF = new String("<EOF>");   // identity-compared sentinel

    private static final int CORE_PORT = Integer.getInteger("sim.corePort", DEFAULT_CORE_PORT);
    private static final int LOG_PORT = Integer.getInteger("sim.logPort", LoggingServer.DEFAULT_PORT);

    private static volatile boolean running = true;
    private static ServerSocket coreServer;
    private static Socket uiSocket;
    private static BufferedWriter uiWriter;
    private static final Object UI_LOCK = new Object();
    private static final LogForwarder LOG = new LogForwarder();

    public static void main(String[] args) {
        System.out.println("=================================");
        System.out.println("       CORE PROCESS STARTED");
        System.out.println("=================================");
        CPU cpu = new CPU();
        ProcessManager manager = new ProcessManager(cpu, new FCFSScheduler());
        System.out.println("CPU created, scheduler: " + manager.getScheduler().name());
        System.out.println("TCP IPC: UI port=" + CORE_PORT + ", Logging port=" + LOG_PORT);
        LOG.start();
        // Launcher stops the Core with SIGTERM: flush queued log entries before the JVM exits.
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { running = false; LOG.stop(); }, "Core-Shutdown"));
        try {
            coreServer = new ServerSocket();
            coreServer.setReuseAddress(true);
            coreServer.bind(new InetSocketAddress(InetAddress.getByName(LOCALHOST), CORE_PORT));
        } catch (IOException ex) {
            System.err.println("Core cannot listen on port " + CORE_PORT + ": " + ex.getMessage()
                    + " (is another Core already running, or is the port used by another program? "
                    + "Use -Dsim.corePort=<port> on both Core and UI to change it.)");
            LOG.stop();
            System.exit(1);
            return;
        }
        System.out.println("Core is READY. Waiting for UI on port " + CORE_PORT + "...");
        info("Core listening on port " + CORE_PORT + ", scheduler = " + manager.getScheduler().name());

        // One UI session at a time; when it ends the Core goes back to waiting, so the UI
        // can be closed and reopened without restarting the Core.
        while (running) {
            try {
                Socket socket = coreServer.accept();
                runSession(socket, manager, cpu);
            } catch (IOException ex) {
                if (running) {
                    System.err.println("Core IPC error: " + ex.getMessage());
                    error("Core IPC error: " + ex.getMessage());
                }
            }
        }
        cleanup();
    }

    // One UI connection: reader thread -> FIFO queue -> this (worker) thread.
    private static void runSession(Socket socket, ProcessManager manager, CPU cpu) throws IOException {
        uiSocket = socket;
        BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        synchronized (UI_LOCK) {
            uiWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        }
        System.out.println("UI connected.");
        info("UI connected from " + socket.getRemoteSocketAddress());
        manager.clear();

        BlockingQueue<String> commands = new LinkedBlockingQueue<>();
        Thread readerThread = new Thread(() -> {
            try {
                String line;
                while ((line = reader.readLine()) != null) commands.put(line);
            } catch (IOException | InterruptedException ignored) {
                // connection closed
            } finally {
                commands.offer(EOF);
            }
        }, "Core-UI-Reader");
        readerThread.setDaemon(true);
        readerThread.start();

        send("CORE_READY|ALGO=" + manager.getScheduler().name() + "|PID=" + processId());

        try {
            while (running) {
                String command = commands.take();          // FIFO: first request in is first served
                if (command == EOF) break;
                if (command.trim().isEmpty()) continue;
                System.out.println("GUI command: " + shorten(command));
                debug("Command from UI: " + shorten(command));
                handleCommand(command, manager, cpu);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            synchronized (UI_LOCK) { uiWriter = null; }
            try { socket.close(); } catch (IOException ignored) { }
            uiSocket = null;
            System.out.println("UI disconnected.");
            info("UI disconnected");
        }
    }

    // ---------------- command handling ----------------

    private static void handleCommand(String command, ProcessManager manager, CPU cpu) {
        try {
            if (command.startsWith("LOADPROCS|")) {
                manager.clear();
                for (String token : command.substring(10).split("\\|")) {
                    if (token.trim().isEmpty()) continue;
                    String[] parts = token.split("@", 3);
                    if (parts.length < 3) throw new IllegalArgumentException("Bad process definition: " + token);
                    int arrival = Integer.parseInt(parts[1].trim());
                    manager.create(cleanName(parts[0]), arrival, parseProgram(parts[2]));
                }
                afterLoad(manager);
            }
            else if (command.startsWith("LOAD|")) {
                manager.clear();
                manager.create("P1", 0, parseProgram(command.substring(5)));
                afterLoad(manager);
            }
            else if (command.equals("STEP")) {
                step(manager, cpu);
            }
            else if (command.equals("RESET")) {
                manager.reset();
                info("RESET: all processes returned to NEW, clock = 0");
                send("RESET|PC=0|CLOCK=0|ALGO=" + manager.getScheduler().name());
                send(procsMessage(manager));
            }
            else if (command.equals("PING")) {
                send("PONG");
            }
            else if (command.equals("SHUTDOWN")) {
                info("SHUTDOWN requested by UI");
                send("CORE_SHUTDOWN");
                running = false;
                try { if (coreServer != null) coreServer.close(); } catch (IOException ignored) { }
            }
            else {
                warn("Unknown command: " + shorten(command));
                send("ERROR|Unknown command: " + command);
            }
        } catch (RuntimeException ex) {
            error(command.split("\\|")[0] + " failed: " + safeMessage(ex));
            send("ERROR|" + safeMessage(ex));
        }
    }

    private static void afterLoad(ProcessManager manager) {
        info("Loaded " + manager.getProcesses().size() + " process(es), scheduler = "
                + manager.getScheduler().name());
        for (PCB p : manager.getProcesses()) {
            info("  PID " + p.pid + " " + p.name + " arrival=" + p.arrivalTime
                    + " instructions=" + p.program.size());
        }
        send("LOADED|PC=0|PROCS=" + manager.getProcesses().size()
                + "|ALGO=" + manager.getScheduler().name());
        send(procsMessage(manager));
    }

    private static void step(ProcessManager manager, CPU cpu) {
        if (manager.isEmpty()) {
            send("ERROR|No processes loaded. Press LOAD first.");
            return;
        }
        if (manager.allTerminated()) {
            send("HALTED|PC=" + cpu.getPC() + "|CLOCK=" + manager.getClock());
            return;
        }
        ProcessManager.TickResult tick = manager.tick();

        for (String event : tick.events) {
            send(event);
            info("SCHED " + event.replace('|', ' '));
        }

        if (tick.kind == ProcessManager.TickResult.Kind.IDLE) {
            send("IDLE|CLOCK=" + manager.getClock() + "|RUNNING=1");
            debug("t=" + (manager.getClock() - 1) + " CPU idle (no process has arrived)");
        }
        else if (tick.kind == ProcessManager.TickResult.Kind.EXECUTED) {
            PCB p = tick.process;
            String text = tick.instruction == null ? "NONE" : tick.instruction.toString();
            if (p.fault != null) {
                error("PID " + p.pid + " " + p.name + " faulted: " + p.fault);
                send("ERROR|" + p.name + " faulted: " + p.fault);
            }
            send("STEP"
                    + "|PID=" + p.pid
                    + "|PNAME=" + p.name
                    + "|CLOCK=" + manager.getClock()
                    + "|OLD_PC=" + tick.oldPc
                    + "|PC=" + cpu.getPC()
                    + "|RUNNING=" + (manager.allTerminated() ? 0 : 1)
                    + stateFields(cpu.getSnapshot())
                    + "|INSTRUCTION=" + text);
            debug("t=" + (manager.getClock() - 1) + " " + p.name + " PC " + tick.oldPc + "->" + cpu.getPC()
                    + " " + text + " | A=" + cpu.getA() + " CY=" + (cpu.isCY() ? 1 : 0)
                    + " OV=" + (cpu.isOV() ? 1 : 0) + " SP=" + cpu.getSP());
        }

        send(procsMessage(manager));

        if (manager.allTerminated()) {
            info(String.format("All processes finished at t=%d. Avg waiting=%.2f, avg turnaround=%.2f, context switches=%d",
                    manager.getClock(), manager.averageWaitingTime(), manager.averageTurnaroundTime(),
                    manager.getContextSwitches()));
        }
    }

    // ---------------- message builders ----------------

    // CPU registers, flags, stack, queue and data memory of the running process.
    private static String stateFields(CpuSnapshot s) {
        StringBuilder sb = new StringBuilder();
        sb.append("|A=").append(s.a);
        for (int i = 0; i < 8; i++) sb.append("|R").append(i).append('=').append(s.registers[i]);
        sb.append("|CY=").append(s.cy ? 1 : 0);
        sb.append("|OV=").append(s.ov ? 1 : 0);
        sb.append("|SP=").append(s.sp);
        sb.append("|STACK=").append(join(s.stackContents));
        sb.append("|STACK_EMPTY=").append(s.stackEmpty ? 1 : 0);
        sb.append("|STACK_FULL=").append(s.stackFull ? 1 : 0);
        sb.append("|QUEUE=").append(s.queueCount);
        sb.append("|QUEUE_CAPACITY=").append(s.queueCapacity);
        sb.append("|QUEUE_VALUES=").append(join(s.queueContents));
        sb.append("|QUEUE_EMPTY=").append(s.queueEmpty ? 1 : 0);
        sb.append("|QUEUE_FULL=").append(s.queueFull ? 1 : 0);
        StringBuilder mem = new StringBuilder();
        for (int[] pair : s.nonZeroMemory) {
            if (mem.length() > 0) mem.append(',');
            mem.append(pair[0]).append(':').append(pair[1]);
        }
        sb.append("|MEMORY=").append(mem);
        return sb.toString();
    }

    // Process table, ready queue, Gantt chart and averages.
    //   TABLE rows: pid:name:state:arrival:executed:start:finish:wait:turnaround  ('-' = not yet known)
    //   GANTT rows: pid:start:end   (pid 0 = CPU idle)
    private static String procsMessage(ProcessManager m) {
        StringBuilder table = new StringBuilder();
        for (PCB p : m.getProcesses()) {
            if (table.length() > 0) table.append(';');
            table.append(p.pid).append(':').append(p.name).append(':').append(p.state)
                 .append(':').append(p.arrivalTime)
                 .append(':').append(p.executed)
                 .append(':').append(p.startTime < 0 ? "-" : String.valueOf(p.startTime))
                 .append(':').append(p.finishTime < 0 ? "-" : String.valueOf(p.finishTime))
                 .append(':').append(p.waitingTime(m.getClock()))
                 .append(':').append(p.turnaroundTime() < 0 ? "-" : String.valueOf(p.turnaroundTime()));
        }
        StringBuilder ready = new StringBuilder();
        for (PCB p : m.getScheduler().readyList()) {
            if (ready.length() > 0) ready.append(',');
            ready.append(p.name);
        }
        StringBuilder gantt = new StringBuilder();
        for (ProcessManager.Segment s : m.getGantt()) {
            if (gantt.length() > 0) gantt.append(';');
            gantt.append(s.pid).append(':').append(s.start).append(':').append(s.end);
        }
        PCB run = m.getRunning();
        return "PROCS|ALGO=" + m.getScheduler().name()
                + "|CLOCK=" + m.getClock()
                + "|RUNNING_PNAME=" + (run == null ? "-" : run.name)
                + "|READY=" + ready
                + "|CTX=" + m.getContextSwitches()
                + "|AVG_WAIT=" + String.format("%.2f", m.averageWaitingTime())
                + "|AVG_TAT=" + String.format("%.2f", m.averageTurnaroundTime())
                + "|TABLE=" + table
                + "|GANTT=" + gantt;
    }

    private static String join(List<Integer> values) {
        StringBuilder sb = new StringBuilder();
        for (Integer v : values) {
            if (sb.length() > 0) sb.append(',');
            sb.append(v);
        }
        return sb.toString();
    }

    // ---------------- IPC helpers ----------------

    // Logging is asynchronous and best-effort: entries are queued and forwarded by the
    // Core-Log-Forwarder thread, so the CPU never waits on the Logging process.
    private static void debug(String m) { LOG.log(LogLevel.DEBUG, m); }
    private static void info(String m)  { LOG.log(LogLevel.INFO, m); }
    private static void warn(String m)  { LOG.log(LogLevel.WARNING, m); }
    private static void error(String m) { LOG.log(LogLevel.ERROR, m); }

    private static void send(String message) {
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

    // "pid@host" from the JVM runtime name; works on every Java version.
    private static String processId() {
        String name = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        int at = name.indexOf('@');
        return at > 0 ? name.substring(0, at) : name;
    }

    private static String cleanName(String raw) {
        String name = raw.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        return name.isEmpty() ? "P" : name;
    }

    private static String shorten(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }

    private static String safeMessage(RuntimeException ex) {
        String m = ex.getMessage();
        return (m == null || m.trim().isEmpty()) ? ex.getClass().getSimpleName() : m.replace('\n', ' ').replace('\r', ' ');
    }

    private static void cleanup() {
        running = false;
        System.out.println("Cleaning up Core IPC...");
        info("Core process stopping");
        LOG.stop();
        try { if (uiSocket != null) uiSocket.close(); } catch (IOException ignored) {}
        try { if (coreServer != null) coreServer.close(); } catch (IOException ignored) {}
        System.out.println("Core Process Stopped");
    }

    /** Core -> Logging IPC: FIFO queue drained by its own thread, reconnecting as needed. */
    private static final class LogForwarder implements Runnable {
        private static final int MAX_QUEUED = 5000;
        private final BlockingQueue<Object[]> queue = new LinkedBlockingQueue<>();
        private final LogClient client = new LogClient("CORE", LOCALHOST, LOG_PORT);
        private volatile boolean active = true;
        private Thread thread;

        void start() {
            thread = new Thread(this, "Core-Log-Forwarder");
            thread.setDaemon(true);
            thread.start();
        }

        void log(LogLevel level, String message) {
            if (queue.size() >= MAX_QUEUED) queue.poll();     // never grow without bound
            queue.offer(new Object[]{level, message});
        }

        void stop() {
            active = false;
            try { if (thread != null) thread.join(2000); } catch (InterruptedException ignored) { }
            client.close();
        }

        @Override public void run() {
            boolean announced = false;
            while (active || (!queue.isEmpty() && client.isConnected())) {
                if (!client.isConnected()) {
                    try {
                        client.connect();
                        if (!announced) System.out.println("Logging process connected on port " + LOG_PORT + ".");
                        announced = true;
                    } catch (IOException ex) {
                        if (!active) return;                   // shutting down and no logger: give up
                        sleep(1000);
                        continue;
                    }
                }
                try {
                    Object[] entry = queue.poll(300, TimeUnit.MILLISECONDS);
                    if (entry != null && !client.send((LogLevel) entry[0], (String) entry[1])) {
                        queue.offer(entry);                    // logger went away: retry after reconnecting
                    }
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }

        private void sleep(long ms) {
            try { Thread.sleep(ms); } catch (InterruptedException ignored) { }
        }
    }
}

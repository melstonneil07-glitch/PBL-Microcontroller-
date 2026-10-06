package gui;

import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import logging.LoggingServer;

/** Displays the demonstration program and only state reported by the Core. */
public final class MainWindow extends JFrame implements TCPClient.MessageListener {
    private static final String HOST = "127.0.0.1";
    private static final int CORE_PORT = Integer.getInteger("sim.corePort", cpu_core.CoreProcess.DEFAULT_CORE_PORT);
    private static final int LOGGER_PORT = Integer.getInteger("sim.logPort", LoggingServer.DEFAULT_PORT);

    private static final Color NAVY = new Color(20, 39, 63);
    private static final Color CANVAS = new Color(239, 244, 249);
    private static final Color INK = new Color(37, 55, 75);
    private static final Color MUTED = new Color(105, 123, 143);
    private static final Color TEAL = new Color(16, 139, 151);

    private static final String[][] DEMO_PROCESSES = DemoWorkload.PROCESSES;

    private final DefaultListModel<String> programModel = new DefaultListModel<>();
    private final JList<String> programList = new JList<>(programModel);
    private final Chip3DPanel chip = new Chip3DPanel();
    private final Map<String, JLabel> stateValues = new LinkedHashMap<>();
    private final JLabel coreStatus = new JLabel("Core: Connecting…");
    private final JLabel loggerStatus = new JLabel("Logger: Connecting…");
    private final JLabel currentInstruction = new JLabel("Waiting for Core update");
    private final JLabel instructionCategory = new JLabel("Category: —");
    private final JLabel statusBar = new JLabel("  Connecting to simulator services…");
    private final JLabel stackPointer = new JLabel("—");
    private final JLabel stackStatus = new JLabel("—");
    private final JTextArea stackArea = new JTextArea();
    private final JLabel queueCount = new JLabel("—");
    private final JLabel queueStatus = new JLabel("—");
    private final JTextArea queueArea = new JTextArea();
    private final JTextArea memoryArea = new JTextArea();
    private final JTextArea traceArea = new JTextArea();
    private final JTextArea logArea = new JTextArea();
    private final SchedulerPanel schedulerPanel = new SchedulerPanel();
    // Display text of the program the CPU is currently running (changes on each dispatch).
    private String[] displayProgram = new String[0];
    private final TCPClient tcpClient = new TCPClient(HOST, CORE_PORT, LOGGER_PORT);
    private final TCPClient.MessageListener tcpListener = new TCPClient.MessageListener() {
        @Override public void onCoreMessage(String message) {
            MainWindow.this.onCoreMessage(message);
        }

        @Override public void onConnectionChanged(boolean connected) {
            MainWindow.this.onConnectionChanged(connected);
        }
    };
    private final Timer runTimer = new Timer(450, e -> sendCore("STEP"));
    private boolean coreReady;
    private boolean programLoaded;

    public MainWindow() {
        super("Nuvoton MS51FB9AE Microcontroller Simulator");
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(1120, 760));
        setSize(1440, 900);
        setLocationRelativeTo(null);
        setContentPane(createRoot());
        tcpClient.setMessageListener(tcpListener);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dispose(); }
        });
        connectInBackground();
    }

    private JPanel createRoot() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBackground(CANVAS);
        root.setBorder(new EmptyBorder(12, 14, 12, 14));
        root.add(createHeader(), BorderLayout.NORTH);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setOpaque(false);
        main.add(createProgramAndControls(), BorderLayout.WEST);
        main.add(createChipPanel(), BorderLayout.CENTER);
        main.add(createCorePanel(), BorderLayout.EAST);
        root.add(main, BorderLayout.CENTER);
        root.add(createFooter(), BorderLayout.SOUTH);
        return root;
    }

    private JPanel createHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setBackground(NAVY);
        header.setBorder(new EmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("NUVOTON MS51FB9AE MICROCONTROLLER SIMULATOR");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        header.add(title, BorderLayout.WEST);

        JPanel connections = new JPanel(new GridLayout(2, 1, 0, 4));
        connections.setOpaque(false);
        styleConnection(coreStatus);
        styleConnection(loggerStatus);
        connections.add(coreStatus);
        connections.add(loggerStatus);
        header.add(connections, BorderLayout.EAST);
        return header;
    }

    private void styleConnection(JLabel label) {
        label.setForeground(new Color(210, 225, 238));
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    private JPanel createProgramAndControls() {
        JPanel column = new JPanel(new BorderLayout(8, 8));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(300, 0));

        JPanel controls = card("SIMULATOR CONTROLS");
        controls.setLayout(new GridLayout(2, 2, 8, 8));
        controls.add(actionButton("LOAD", this::loadProgram, TEAL));
        controls.add(actionButton("STEP", () -> sendCore("STEP"), NAVY));
        controls.add(actionButton("RUN", this::runProgram, new Color(41, 135, 91)));
        controls.add(actionButton("RESET", this::resetProgram, new Color(157, 83, 69)));

        JPanel memory = card("PROGRAM MEMORY");
        memory.setLayout(new BorderLayout(5, 8));
        programList.setFont(new Font("Consolas", Font.PLAIN, 13));
        programList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        programList.setBackground(new Color(250, 252, 254));
        memory.add(new JScrollPane(programList), BorderLayout.CENTER);

        JPanel current = new JPanel(new GridLayout(0, 1, 0, 5));
        current.setOpaque(false);
        JLabel currentTitle = new JLabel("CURRENT INSTRUCTION");
        currentTitle.setFont(new Font("Segoe UI", Font.BOLD, 10));
        currentTitle.setForeground(MUTED);
        currentInstruction.setFont(new Font("Consolas", Font.BOLD, 14));
        currentInstruction.setForeground(INK);
        instructionCategory.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        instructionCategory.setForeground(MUTED);
        current.add(currentTitle);
        current.add(currentInstruction);
        current.add(instructionCategory);
        memory.add(current, BorderLayout.SOUTH);

        column.add(controls, BorderLayout.NORTH);
        column.add(memory, BorderLayout.CENTER);
        return column;
    }

    private JPanel createChipPanel() {
        JPanel panel = card("MICROCONTROLLER VIEW");
        panel.setLayout(new BorderLayout());
        // Keep the visualization's width (and therefore the chip's apparent
        // scale) fixed, while allowing its dark canvas to use the full panel height.
        chip.setPreferredSize(new Dimension(430, 500));
        chip.setMinimumSize(new Dimension(430, 320));
        chip.setMaximumSize(new Dimension(430, Integer.MAX_VALUE));
        JPanel viewport = new JPanel(new GridBagLayout());
        viewport.setBackground(Color.WHITE);
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.fill = GridBagConstraints.VERTICAL;
        constraints.weighty = 1.0;
        viewport.add(chip, constraints);
        panel.add(viewport, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createCorePanel() {
        JPanel column = new JPanel(new BorderLayout(8, 8));
        column.setOpaque(false);
        column.setPreferredSize(new Dimension(620, 0));

        JPanel panels = new JPanel(new GridLayout(2, 2, 8, 8));
        panels.setOpaque(false);
        panels.add(createRegisterPanel());
        panels.add(createStackPanel());
        panels.add(createQueuePanel());
        panels.add(createMemoryPanel());
        column.add(panels, BorderLayout.CENTER);
        return column;
    }

    private JPanel createRegisterPanel() {
        JPanel panel = card("CPU REGISTERS");
        panel.setLayout(new BorderLayout());
        JPanel registerFields = new JPanel(new GridLayout(0, 2, 5, 7));
        registerFields.setBackground(Color.WHITE);
        String[] keys = {"A", "B", "PSW", "PC", "SP", "CY", "OV", "RUNNING",
                "R0", "R1", "R2", "R3", "R4", "R5", "R6", "R7", "RESPONSE"};
        for (String key : keys) addStateField(registerFields, key);
        JScrollPane registerScroll = new JScrollPane(registerFields,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        registerScroll.setBorder(null);
        registerScroll.getVerticalScrollBar().setUnitIncrement(16);
        panel.add(registerScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStackPanel() {
        JPanel panel = card("STACK");
        panel.setLayout(new BorderLayout());
        JPanel contents = new JPanel(new BorderLayout(4, 5));
        contents.setBackground(Color.WHITE);
        JPanel summary = new JPanel(new GridLayout(0, 1, 0, 3));
        summary.setOpaque(false);
        JPanel spRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        spRow.setOpaque(false);
        spRow.add(new JLabel("SP: "));
        spRow.add(stackPointer);
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusRow.setOpaque(false);
        statusRow.add(new JLabel("Status: "));
        statusRow.add(stackStatus);
        summary.add(spRow);
        summary.add(statusRow);
        configureTextArea(stackArea, false);
        stackArea.setText("Stack contents not reported by Core.");
        stackArea.setRows(8);
        contents.add(summary, BorderLayout.NORTH);
        contents.add(createVerticalContentScroll(stackArea), BorderLayout.CENTER);
        panel.add(createVerticalContentScroll(contents), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createQueuePanel() {
        JPanel panel = card("FIFO QUEUE");
        panel.setLayout(new BorderLayout());
        JPanel contents = new JPanel(new BorderLayout(4, 5));
        contents.setBackground(Color.WHITE);
        JPanel summary = new JPanel(new GridLayout(0, 1, 0, 3));
        summary.setOpaque(false);
        JPanel countRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        countRow.setOpaque(false);
        countRow.add(new JLabel("Count: "));
        countRow.add(queueCount);
        JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        statusRow.setOpaque(false);
        statusRow.add(new JLabel("Status: "));
        statusRow.add(queueStatus);
        summary.add(countRow);
        summary.add(statusRow);
        configureTextArea(queueArea, false);
        queueArea.setText("Queue contents not reported by Core.");
        queueArea.setRows(8);
        contents.add(summary, BorderLayout.NORTH);
        contents.add(createVerticalContentScroll(queueArea), BorderLayout.CENTER);
        panel.add(createVerticalContentScroll(contents), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMemoryPanel() {
        JPanel panel = card("DATA MEMORY");
        panel.setLayout(new BorderLayout());
        configureTextArea(memoryArea, false);
        memoryArea.setText("Data memory not reported by Core.");
        panel.add(new JScrollPane(memoryArea), BorderLayout.CENTER);
        return panel;
    }

    private JScrollPane createVerticalContentScroll(JComponent contents) {
        JScrollPane scroll = new JScrollPane(contents,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private void addStateField(JPanel panel, String key) {
        JLabel name = new JLabel(key);
        name.setForeground(MUTED);
        name.setFont(new Font("Segoe UI", Font.BOLD, 12));
        JLabel value = new JLabel("—");
        value.setForeground(INK);
        value.setFont(new Font("Consolas", Font.BOLD, 12));
        stateValues.put(key, value);
        panel.add(name);
        panel.add(value);
    }

    private JPanel createFooter() {
        JPanel footer = new JPanel(new BorderLayout(8, 7));
        footer.setOpaque(false);
        JPanel lower = new JPanel(new GridLayout(1, 3, 8, 0));
        lower.setOpaque(false);
        lower.setPreferredSize(new Dimension(0, 230));
        JPanel trace = card("FETCH -> DECODE -> EXECUTE");
        trace.setLayout(new BorderLayout());
        configureTextArea(traceArea, true);
        JScrollPane traceScroll = new JScrollPane(traceArea,
                ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        traceScroll.getVerticalScrollBar().setUnitIncrement(16);
        trace.add(traceScroll, BorderLayout.CENTER);
        JPanel logs = card("SYSTEM LOG");
        logs.setLayout(new BorderLayout());
        configureTextArea(logArea, true);
        logs.add(new JScrollPane(logArea), BorderLayout.CENTER);
        JPanel scheduler = card("PROCESS SCHEDULER (FCFS)");
        scheduler.setLayout(new BorderLayout());
        scheduler.add(schedulerPanel, BorderLayout.CENTER);
        lower.add(trace);
        lower.add(scheduler);
        lower.add(logs);
        statusBar.setOpaque(true);
        statusBar.setBackground(new Color(225, 235, 244));
        statusBar.setForeground(NAVY);
        statusBar.setBorder(new EmptyBorder(8, 10, 8, 10));
        statusBar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        footer.add(lower, BorderLayout.CENTER);
        footer.add(statusBar, BorderLayout.SOUTH);
        return footer;
    }

    private JPanel card(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(new Color(211, 221, 231)), title),
                new EmptyBorder(9, 9, 9, 9)));
        return panel;
    }

    private JButton actionButton(String title, Runnable action, Color color) {
        JButton button = new JButton(title);
        button.setFocusPainted(false);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.addActionListener(event -> action.run());
        return button;
    }

    private void configureTextArea(JTextArea area, boolean wrap) {
        area.setEditable(false);
        area.setLineWrap(wrap);
        area.setWrapStyleWord(true);
        area.setFont(new Font("Consolas", Font.PLAIN, 12));
        area.setBackground(new Color(249, 251, 253));
        area.setForeground(INK);
    }

    // Shows the given Core-format program (e.g. "MOV_A_DATA:10;HALT") in the program memory list.
    private void showProgram(String coreProgram) {
        String[] instructions = coreProgram.split(";");
        displayProgram = new String[instructions.length];
        programModel.clear();
        for (int i = 0; i < instructions.length; i++) {
            displayProgram[i] = toDisplay(instructions[i]);
            programModel.addElement(String.format("%04X   %s", i, displayProgram[i]));
        }
    }

    private String programOf(String processName) {
        for (String[] p : DEMO_PROCESSES) if (p[0].equals(processName)) return p[2];
        return null;
    }

    // "MOV_RN_DATA:R1,3" -> "MOV R1,#3"
    private String toDisplay(String core) {
        String[] parts = core.trim().split(":", 2);
        String ops = parts.length > 1 ? parts[1].trim() : "";
        switch (parts[0]) {
            case "MOV_A_DATA":  return "MOV A,#" + ops;
            case "MOV_RN_DATA": return "MOV " + ops.replace(",", ",#");
            case "ADD": case "SUBB": case "ANL": return parts[0] + " A," + ops;
            default: return ops.isEmpty() ? parts[0] : parts[0] + " " + ops;
        }
    }

    // The Core and Logging processes may start before, after, or be restarted independently
    // of the UI, so each connection is retried in the background until it is established.
    private final app.ServiceSpawner spawner = new app.ServiceSpawner(CORE_PORT, LOGGER_PORT);
    private final java.util.concurrent.atomic.AtomicBoolean coreConnecting = new java.util.concurrent.atomic.AtomicBoolean();
    private final java.util.concurrent.atomic.AtomicBoolean loggerConnecting = new java.util.concurrent.atomic.AtomicBoolean();

    private void connectInBackground() {
        tcpClient.setMessageListener(tcpListener);
        connectCoreWithRetry();
        connectLoggerWithRetry();
    }

    private void connectCoreWithRetry() {
        if (!coreConnecting.compareAndSet(false, true)) return;
        Thread connector = new Thread(() -> {
            int attempt = 0;
            while (!tcpClient.connectCore()) {
                attempt++;
                final int n = attempt;
                final String error = tcpClient.getLastError();
                final String spawnNote = autoStartServices(n);
                SwingUtilities.invokeLater(() -> {
                    coreStatus.setText("Core: Disconnected");
                    setStatus("Core not reachable on 127.0.0.1:" + CORE_PORT + " (attempt " + n + "). Retrying…");
                    if (n == 1 || n % 5 == 0) logLine("UI: Core connect failed: " + error);
                    if (spawnNote != null) logLine("UI: " + spawnNote);
                });
                try { Thread.sleep(1500); } catch (InterruptedException e) { return; }
            }
            coreConnecting.set(false);
            SwingUtilities.invokeLater(() -> {
                coreStatus.setText("Core: Connected");
                setStatus("Connected to Core on port " + CORE_PORT + "; waiting for it to report ready.");
            });
        }, "simulator-core-connect");
        connector.setDaemon(true);
        connector.start();
    }

    private void connectLoggerWithRetry() {
        if (!loggerConnecting.compareAndSet(false, true)) return;
        Thread connector = new Thread(() -> {
            while (!tcpClient.connectLogger()) {
                SwingUtilities.invokeLater(() -> loggerStatus.setText("Logger: Disconnected"));
                try { Thread.sleep(1500); } catch (InterruptedException e) { return; }
            }
            loggerConnecting.set(false);
            SwingUtilities.invokeLater(() -> {
                loggerStatus.setText("Logger: Connected");
                logLine("Logger connection established.");
            });
            // Keep watching: if the Logging process goes away, reconnect when it returns.
            while (tcpClient.isLoggerConnected()) {
                try { Thread.sleep(1000); } catch (InterruptedException e) { return; }
            }
            loggerConnecting.set(false);
            SwingUtilities.invokeLater(() -> {
                loggerStatus.setText("Logger: Disconnected");
                connectLoggerWithRetry();
            });
        }, "simulator-logger-connect");
        connector.setDaemon(true);
        connector.start();
    }

    // No Core answering after a few seconds: start the Core (and Logger) as separate processes
    // ourselves, once. Returns a note for the system log, or null.
    private String autoStartServices(int attempt) {
        try {
            if (attempt == 2 && !tcpClient.isLoggerConnected() && spawner.startLogger()) {
                return "Logging process not running - started it automatically.";
            }
            if (attempt == 3 && spawner.startCore()) {
                return "Core process not running - started it automatically on port " + CORE_PORT + ".";
            }
            if (spawner.coreDied()) {
                return "The Core process I started exited with code " + spawner.coreExitCode()
                        + " (port " + CORE_PORT + " in use? see console output).";
            }
        } catch (java.io.IOException ex) {
            return "Could not start service process: " + ex.getMessage();
        }
        return null;
    }

    // Every button goes through this so a click is never silently ignored.
    private boolean requireCore(String action) {
        if (isCoreConnected()) return true;
        String why = !tcpClient.isCoreConnected()
                ? "Core process is not connected (port " + CORE_PORT + ")"
                : "Core has not reported ready yet";
        setStatus("Cannot " + action + ": " + why + ".");
        logLine("UI: cannot " + action + " - " + why);
        return false;
    }

    private void loadProgram() {
        if (!requireCore("load")) return;
        clearReportedValues();
        tcpClient.sendToCore(DemoWorkload.loadCommand());
        sendLog("LOAD requested: " + DEMO_PROCESSES.length + " processes (FCFS)");
        setStatus("Process load requested.");
    }

    private void runProgram() {
        if (!requireCore("run")) return;
        if (!programLoaded) {
            setStatus("Nothing to run: press LOAD first.");
            return;
        }
        if ("false".equalsIgnoreCase(stateText("RUNNING")) || "0".equals(stateText("RUNNING"))) {
            setStatus("Core has not reported that the program is running.");
            return;
        }
        runTimer.start();
        sendLog("RUN requested");
        setStatus("Run requested.");
    }

    private void resetProgram() {
        runTimer.stop();
        if (!requireCore("reset")) return;
        tcpClient.sendToCore("RESET");
        sendLog("RESET requested");
        setStatus("Reset requested.");
    }

    private void sendCore(String command) {
        if (!requireCore(command.toLowerCase())) {
            runTimer.stop();
            return;
        }
        tcpClient.sendToCore(command);
        if ("STEP".equals(command)) sendLog("STEP requested");
    }

    private boolean isCoreConnected() {
        return tcpClient.isCoreConnected() && coreReady;
    }

    @Override public void onCoreMessage(String message) {
        SwingUtilities.invokeLater(() -> processCoreMessage(message));
    }

    @Override public void onConnectionChanged(boolean connected) {
        SwingUtilities.invokeLater(() -> {
            coreReady = false;
            coreStatus.setText(connected ? "Core: Connected" : "Core: Disconnected");
            if (!connected) {
                runTimer.stop();
                programLoaded = false;
                setStatus("Core connection lost. Reconnecting…");
                connectCoreWithRetry();
            }
        });
    }

    private void processCoreMessage(String message) {
        if (message.startsWith("PROCS|")) {
            schedulerPanel.update(parseFields(message));
            return;
        }
        logLine("Core: " + message);
        sendLog("CORE -> UI: " + message);
        if (message.startsWith("CORE_READY")) {
            coreReady = true;
            Map<String, String> ready = parseFields(message);
            coreStatus.setText("Core: Ready" + (ready.containsKey("PID") ? " (pid " + ready.get("PID") + ")" : ""));
            setStatus("Core is ready. Load the program to begin.");
            return;
        }
        if (message.startsWith("LOADED")) {
            programLoaded = true;
            showProgram(DEMO_PROCESSES[0][2]);
            Map<String, String> fields = parseFields(message);
            updateFields(fields);
            selectInstruction(parseInteger(fields.get("PC"), -1), null);
            setResponse("LOADED");
            setStatus("Core reported that the program loaded.");
            return;
        }
        if (message.startsWith("RESET")) {
            runTimer.stop();
            Map<String, String> fields = parseFields(message);
            clearReportedValues();
            updateFields(fields);
            if (programLoaded) selectInstruction(parseInteger(fields.get("PC"), -1), null);
            chip.reset();
            schedulerPanel.clear();
            if (programLoaded) showProgram(DEMO_PROCESSES[0][2]);
            setResponse("RESET");
            setStatus("Core reported reset.");
            return;
        }
        if (message.startsWith("HALTED")) {
            runTimer.stop();
            Map<String, String> fields = parseFields(message);
            updateFields(fields);
            stateValues.get("RUNNING").setText("false");
            setResponse("HALTED");
            chip.setInstruction("HALTED");
            setStatus("All processes finished. Press RESET to run them again.");
            return;
        }
        if (message.startsWith("ERROR")) {
            runTimer.stop();
            setResponse("ERROR");
            setStatus(message);
            return;
        }
        if (message.startsWith("DISPATCH")) {
            Map<String, String> fields = parseFields(message);
            String program = programOf(fields.get("PNAME"));
            if (program != null) showProgram(program);
            programList.clearSelection();
            setStatus("Dispatched " + fields.get("PNAME") + " at t=" + fields.get("CLOCK")
                    + " (waited " + fields.get("WAITED") + ").");
            appendTrace("DISPATCH: " + fields.get("PNAME") + " gets the CPU");
            return;
        }
        if (message.startsWith("CONTEXT_SWITCH")) {
            Map<String, String> fields = parseFields(message);
            appendTrace("CONTEXT SWITCH: " + fields.get("FROM") + " -> " + fields.get("TO"));
            setResponse("CONTEXT SWITCH");
            return;
        }
        if (message.startsWith("ADMIT")) {
            appendTrace("ADMIT: " + parseFields(message).get("PNAME") + " joins the ready queue");
            return;
        }
        if (message.startsWith("TERMINATE")) {
            appendTrace("TERMINATE: " + parseFields(message).get("PNAME") + " finished");
            return;
        }
        if (message.startsWith("IDLE")) {
            setResponse("IDLE");
            setStatus("CPU idle: no process has arrived yet.");
            return;
        }
        if (message.startsWith("STEP")) processStep(message);
    }

    private void processStep(String message) {
        Map<String, String> fields = parseFields(message);
        updateFields(fields);
        int oldPC = parseInteger(fields.get("OLD_PC"), -1);
        String instruction = fields.get("INSTRUCTION");
        selectInstruction(oldPC, instruction);
        if (instruction != null) {
            chip.setInstruction(instruction);
            chip.pulse();
            instructionCategory.setText("Category: " + categoryFor(instruction));
        }
        setResponse("STEP");
        if (fields.containsKey("OLD_PC")) appendTrace("FETCH   ✓ : " + fields.getOrDefault("PNAME", "") + " PC = " + fields.get("OLD_PC"));
        if (instruction != null) appendTrace("DECODE  ✓ : " + instruction);
        if (instruction != null) appendTrace("EXECUTE ✓ : " + instruction);
        appendReportedTraceFields(fields);
        if ("0".equals(fields.get("RUNNING"))) {
            runTimer.stop();
            setStatus("All processes finished. Press RESET to run them again.");
        } else {
            setStatus("Core step response received.");
        }
    }

    private void updateFields(Map<String, String> fields) {
        for (Map.Entry<String, String> field : fields.entrySet()) {
            JLabel label = stateValues.get(field.getKey());
            if (label != null) label.setText(field.getValue());
        }

        if (fields.containsKey("SP")) stackPointer.setText(fields.get("SP"));
        if (fields.containsKey("STACK_EMPTY")) stackStatus.setText("1".equals(fields.get("STACK_EMPTY")) ? "Empty" : "Available");
        if (fields.containsKey("STACK_FULL") && "1".equals(fields.get("STACK_FULL"))) stackStatus.setText("Full");
        if (fields.containsKey("STACK")) stackArea.setText(formatValues(fields.get("STACK")));

        if (fields.containsKey("QUEUE")) {
            queueCount.setText(fields.get("QUEUE") + (fields.containsKey("QUEUE_CAPACITY") ? " / " + fields.get("QUEUE_CAPACITY") : ""));
        }
        if (fields.containsKey("QUEUE_EMPTY")) queueStatus.setText("1".equals(fields.get("QUEUE_EMPTY")) ? "Empty" : "Available");
        if (fields.containsKey("QUEUE_FULL") && "1".equals(fields.get("QUEUE_FULL"))) queueStatus.setText("Full");
        if (fields.containsKey("QUEUE_VALUES")) queueArea.setText(formatValues(fields.get("QUEUE_VALUES")));
        if (fields.containsKey("MEMORY")) memoryArea.setText(formatMemory(fields.get("MEMORY")));
    }

    private void clearReportedValues() {
        for (JLabel value : stateValues.values()) value.setText("—");
        stackPointer.setText("—");
        stackStatus.setText("—");
        stackArea.setText("Stack contents not reported by Core.");
        queueCount.setText("—");
        queueStatus.setText("—");
        queueArea.setText("Queue contents not reported by Core.");
        memoryArea.setText("Data memory not reported by Core.");
        currentInstruction.setText("Waiting for Core update");
        instructionCategory.setText("Category: —");
    }

    private void setResponse(String response) {
        JLabel label = stateValues.get("RESPONSE");
        if (label != null) label.setText(response);
    }

    private void appendReportedTraceFields(Map<String, String> fields) {
        String[] keys = {"PC", "A", "R1", "CY", "OV", "SP", "QUEUE", "RUNNING"};
        for (String key : keys) {
            if (fields.containsKey(key)) appendTrace(String.format("%-8s: %s", key, fields.get(key)));
        }
        appendTrace("--------------------------------");
    }

    private String formatValues(String values) {
        if (values == null || values.isEmpty()) return "(empty)";
        StringBuilder text = new StringBuilder();
        String[] entries = values.split(",");
        for (int i = 0; i < entries.length; i++) {
            if (i > 0) text.append('\n');
            text.append('[').append(i).append("] ").append(entries[i]);
        }
        return text.toString();
    }

    private String formatMemory(String memory) {
        if (memory == null || memory.isEmpty()) return "(all zero)";
        StringBuilder text = new StringBuilder();
        for (String entry : memory.split(",")) {
            String[] pair = entry.split(":", 2);
            if (text.length() > 0) text.append('\n');
            if (pair.length == 2) text.append('[').append(pair[0]).append("] = ").append(pair[1]);
            else text.append(entry);
        }
        return text.toString();
    }

    private void selectInstruction(int index, String coreInstruction) {
        if (index >= 0 && index < displayProgram.length) {
            programList.setSelectedIndex(index);
            programList.ensureIndexIsVisible(index);
            String text = coreInstruction == null ? displayProgram[index] : coreInstruction;
            currentInstruction.setText(text);
            instructionCategory.setText("Category: " + categoryFor(text));
            if (coreInstruction == null) chip.setInstruction(text);
        } else if (coreInstruction != null) {
            currentInstruction.setText(coreInstruction);
            instructionCategory.setText("Category: " + categoryFor(coreInstruction));
        }
    }

    private String categoryFor(String text) {
        String upper = text == null ? "" : text.trim().toUpperCase();
        if (upper.startsWith("MOV") || upper.startsWith("MOV_")) return "Data Transfer";
        if (upper.startsWith("ADD") || upper.startsWith("SUBB") || upper.startsWith("INC")) return "Arithmetic";
        if (upper.startsWith("ANL")) return "Logical";
        if (upper.startsWith("PUSH") || upper.startsWith("POP")) return "Stack Operation";
        if (upper.startsWith("ENQUEUE") || upper.startsWith("DEQUEUE")) return "FIFO Operation";
        if (upper.startsWith("SJMP")) return "Control Flow";
        if (upper.startsWith("HALT")) return "Program Termination";
        return "—";
    }

    private Map<String, String> parseFields(String message) {
        Map<String, String> fields = new LinkedHashMap<>();
        String[] parts = message.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            int equals = parts[i].indexOf('=');
            if (equals > 0) fields.put(parts[i].substring(0, equals), parts[i].substring(equals + 1));
        }
        return fields;
    }

    private int parseInteger(String value, int fallback) {
        if (value == null) return fallback;
        try { return Integer.parseInt(value.trim()); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private String stateText(String key) { return stateValues.get(key).getText(); }
    private void sendLog(String message) { if (tcpClient.isLoggerConnected()) tcpClient.sendLog(message); }
    private void appendTrace(String text) { appendTo(traceArea, text); }
    private void logLine(String text) { appendTo(logArea, text); }
    private void appendTo(JTextArea area, String text) {
        area.append(text + "\n");
        area.setCaretPosition(area.getDocument().getLength());
    }
    private void setStatus(String text) { statusBar.setText("  " + text); }

    @Override public void dispose() {
        spawner.stopAll();
        runTimer.stop();
        chip.stop();
        tcpClient.close();
        super.dispose();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}

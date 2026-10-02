import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Date;

public class UIProcess
        extends JFrame
        implements IPCClient.MessageListener {

    // =========================================================
    // COLORS
    // =========================================================

    private static final Color BG =
            new Color(8, 12, 18);

    private static final Color PANEL =
            new Color(15, 21, 30);

    private static final Color PANEL2 =
            new Color(20, 28, 39);

    private static final Color BORDER =
            new Color(55, 70, 88);

    private static final Color TEXT =
            new Color(235, 242, 250);

    private static final Color MUTED =
            new Color(145, 160, 178);

    private static final Color BLUE =
            new Color(70, 150, 235);

    private static final Color GREEN =
            new Color(70, 200, 125);

    private static final Color ORANGE =
            new Color(240, 165, 65);

    private static final Color RED =
            new Color(225, 80, 90);

    private static final Color PURPLE =
            new Color(155, 110, 225);

    // =========================================================
    // IPC
    // =========================================================

    private IPCClient ipc;

    // =========================================================
    // PROGRAM MEMORY
    // =========================================================

    private DefaultListModel<String> programModel;

    private JList<String> programMemory;

    private JLabel currentInstruction;

    private JLabel instructionCategory;

    // =========================================================
    // CPU REGISTERS
    // =========================================================

    private JLabel accValue;
    private JLabel bValue;
    private JLabel pswValue;

    private JLabel pcValue;
    private JLabel spValue;

    private JLabel cyValue;
    private JLabel ovValue;

    private JLabel[] registerValues =
            new JLabel[8];

    private JLabel cpuStatus;

    // =========================================================
    // STACK
    // =========================================================

    private JLabel stackSP;
    private JLabel stackStatus;

    private JTextArea stackArea;

    // =========================================================
    // FIFO
    // =========================================================

    private JLabel fifoCount;
    private JLabel fifoStatus;

    private JTextArea fifoArea;

    // =========================================================
    // DATA MEMORY
    // =========================================================

    private JTextArea dataMemoryArea;

    // =========================================================
    // EXECUTION PANEL
    // =========================================================

    private JLabel phaseValue;
    private JLabel opcodeValue;
    private JLabel operandValue;
    private JLabel resultValue;
    private JLabel descriptionValue;

    private JPanel fetchPanel;
    private JPanel decodePanel;
    private JPanel executePanel;

    // =========================================================
    // EXECUTION TRACE
    // =========================================================

    private JTextArea executionTrace;

    // =========================================================
    // READY QUEUE
    // =========================================================

    private DefaultListModel<String> readyModel;

    private JList<String> readyQueue;

    private JLabel readyCount;

    // =========================================================
    // SCHEDULER
    // =========================================================

    private JLabel schedulerValue;

    // =========================================================
    // SYSTEM STATUS
    // =========================================================

    private JLabel ipcStatus;

    private JLabel coreStatus;

    // =========================================================
    // RUN
    // =========================================================

    private javax.swing.Timer runTimer;

    private int cycle = 0;

    private boolean running = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public UIProcess() {

        setTitle(
                "Nuvoton MS51FB9AE Microcontroller Simulator"
        );

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setSize(
                1500,
                950
        );

        setMinimumSize(
                new Dimension(
                        1200,
                        750
                )
        );

        setLocationRelativeTo(null);

        getContentPane().setBackground(
                BG
        );

        buildUI();

        initializeDisplay();

        connectToCore();
    }

    // =========================================================
    // BUILD UI
    // =========================================================

    private void buildUI() {

        setLayout(
                new BorderLayout(
                        8,
                        8
                )
        );

        add(
                createHeader(),
                BorderLayout.NORTH
        );

        add(
                createMainArea(),
                BorderLayout.CENTER
        );

        add(
                createFooter(),
                BorderLayout.SOUTH
        );
    }

    // =========================================================
    // HEADER
    // =========================================================

    private JPanel createHeader() {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(BG);

        panel.setBorder(
                new EmptyBorder(
                        12,
                        15,
                        5,
                        15
                )
        );

        JLabel title =
                new JLabel(
                        "NUVOTON MS51FB9AE MICROCONTROLLER SIMULATOR",
                        SwingConstants.CENTER
                );

        title.setForeground(TEXT);

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        24
                )
        );

        panel.add(
                title,
                BorderLayout.CENTER
        );

        JLabel ipcLabel =
                new JLabel(
                        "POSIX IPC"
                );

        ipcLabel.setForeground(MUTED);

        ipcLabel.setFont(
                new Font(
                        "Consolas",
                        Font.BOLD,
                        11
                )
        );

        panel.add(
                ipcLabel,
                BorderLayout.EAST
        );

        return panel;
    }

    // =========================================================
    // MAIN AREA
    // =========================================================

    private JPanel createMainArea() {

        JPanel main =
                new JPanel(
                        new BorderLayout(
                                8,
                                8
                        )
                );

        main.setBackground(BG);

        main.setBorder(
                new EmptyBorder(
                        0,
                        10,
                        5,
                        10
                )
        );

        main.add(
                createTopPanels(),
                BorderLayout.NORTH
        );

        main.add(
                createCenterPanels(),
                BorderLayout.CENTER
        );

        return main;
    }

    // =========================================================
    // TOP PANELS
    // =========================================================

    private JPanel createTopPanels() {

        JPanel panel =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                8,
                                8
                        )
                );

        panel.setBackground(BG);

        panel.setPreferredSize(
                new Dimension(
                        0,
                        350
                )
        );

        panel.add(
                createProgramMemory()
        );

        panel.add(
                createRegisterPanel()
        );

        panel.add(
                createStackPanel()
        );

        return panel;
    }

    // =========================================================
    // PROGRAM MEMORY
    // =========================================================

    private JPanel createProgramMemory() {

        JPanel card =
                createCard(
                        "PROGRAM MEMORY"
                );

        card.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        programModel =
                new DefaultListModel<>();

        programMemory =
                new JList<>(
                        programModel
                );

        programMemory.setBackground(
                new Color(
                        7,
                        11,
                        16
                )
        );

        programMemory.setForeground(TEXT);

        programMemory.setSelectionBackground(
                new Color(
                        55,
                        95,
                        145
                )
        );

        programMemory.setSelectionForeground(
                Color.WHITE
        );

        programMemory.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        13
                )
        );

        programMemory.setFixedCellHeight(
                22
        );

        JScrollPane scroll =
                new JScrollPane(
                        programMemory
                );

        scroll.setBorder(
                new LineBorder(BORDER)
        );

        card.add(
                scroll,
                BorderLayout.CENTER
        );

        JPanel bottom =
                new JPanel(
                        new GridLayout(
                                2,
                                1
                        )
                );

        bottom.setBackground(PANEL);

        currentInstruction =
                createSmallLabel(
                        "Current Instruction: -"
                );

        instructionCategory =
                createSmallLabel(
                        "Category: -"
                );

        bottom.add(
                currentInstruction
        );

        bottom.add(
                instructionCategory
        );

        card.add(
                bottom,
                BorderLayout.SOUTH
        );

        return card;
    }

    // =========================================================
    // REGISTER PANEL
    // =========================================================

    private JPanel createRegisterPanel() {

        JPanel card =
                createCard(
                        "CPU REGISTERS"
                );

        card.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        JPanel main =
                new JPanel(
                        new GridLayout(
                                4,
                                2,
                                10,
                                6
                        )
                );

        main.setBackground(PANEL);

        accValue =
                createRegisterValue("00H");

        bValue =
                createRegisterValue("00H");

        pswValue =
                createRegisterValue("00H");

        pcValue =
                createRegisterValue("0000H");

        spValue =
                createRegisterValue("07H");

        cyValue =
                createRegisterValue("0");

        ovValue =
                createRegisterValue("0");

        cpuStatus =
                createRegisterValue("READY");

        main.add(
                registerRow(
                        "A",
                        accValue
                )
        );

        main.add(
                registerRow(
                        "B",
                        bValue
                )
        );

        main.add(
                registerRow(
                        "PSW",
                        pswValue
                )
        );

        main.add(
                registerRow(
                        "PC",
                        pcValue
                )
        );

        main.add(
                registerRow(
                        "SP",
                        spValue
                )
        );

        main.add(
                registerRow(
                        "CY",
                        cyValue
                )
        );

        main.add(
                registerRow(
                        "OV",
                        ovValue
                )
        );

        main.add(
                registerRow(
                        "STATUS",
                        cpuStatus
                )
        );

        card.add(
                main,
                BorderLayout.NORTH
        );

        JPanel registers =
                new JPanel(
                        new GridLayout(
                                4,
                                2,
                                8,
                                5
                        )
                );

        registers.setBackground(PANEL);

        for (int i = 0; i < 8; i++) {

            registerValues[i] =
                    createRegisterValue(
                            "00H"
                    );
        }

        for (int i = 0; i < 8; i++) {

            registers.add(
                    registerRow(
                            "R" + i,
                            registerValues[i]
                    )
            );
        }

        card.add(
                registers,
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // STACK
    // =========================================================

    private JPanel createStackPanel() {

        JPanel card =
                createCard(
                        "STACK"
                );

        card.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        JPanel info =
                new JPanel(
                        new GridLayout(
                                2,
                                1
                        )
                );

        info.setBackground(PANEL);

        stackSP =
                createSmallLabel(
                        "SP: 07H"
                );

        stackStatus =
                createSmallLabel(
                        "Status: Empty"
                );

        info.add(
                stackSP
        );

        info.add(
                stackStatus
        );

        card.add(
                info,
                BorderLayout.NORTH
        );

        stackArea =
                createTextArea();

        stackArea.setText(
                "(stack data waiting for Core)"
        );

        card.add(
                new JScrollPane(
                        stackArea
                ),
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // CENTER
    // =========================================================

    private JPanel createCenterPanels() {

        JPanel center =
                new JPanel(
                        new BorderLayout(
                                8,
                                8
                        )
                );

        center.setBackground(BG);

        center.add(
                createMemoryPanels(),
                BorderLayout.NORTH
        );

        center.add(
                createExecutionPanel(),
                BorderLayout.CENTER
        );

        return center;
    }

    // =========================================================
    // MEMORY PANELS
    // =========================================================

    private JPanel createMemoryPanels() {

        JPanel panel =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                8,
                                8
                        )
                );

        panel.setBackground(BG);

        panel.setPreferredSize(
                new Dimension(
                        0,
                        190
                )
        );

        panel.add(
                createFIFOPanel()
        );

        panel.add(
                createDataMemoryPanel()
        );

        return panel;
    }

    // =========================================================
    // FIFO
    // =========================================================

    private JPanel createFIFOPanel() {

        JPanel card =
                createCard(
                        "FIFO QUEUE"
                );

        card.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        JPanel info =
                new JPanel(
                        new GridLayout(
                                2,
                                1
                        )
                );

        info.setBackground(PANEL);

        fifoCount =
                createSmallLabel(
                        "Count: 0 / 16"
                );

        fifoStatus =
                createSmallLabel(
                        "Status: OK"
                );

        info.add(
                fifoCount
        );

        info.add(
                fifoStatus
        );

        card.add(
                info,
                BorderLayout.NORTH
        );

        fifoArea =
                createTextArea();

        fifoArea.setText(
                "Front → Rear:\n"
                        + "(empty)"
        );

        card.add(
                new JScrollPane(
                        fifoArea
                ),
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // DATA MEMORY
    // =========================================================

    private JPanel createDataMemoryPanel() {

        JPanel card =
                createCard(
                        "DATA MEMORY (NON-ZERO ADDRESSES)"
                );

        card.setLayout(
                new BorderLayout()
        );

        dataMemoryArea =
                createTextArea();

        dataMemoryArea.setText(
                "(all zero / waiting for Core data)"
        );

        card.add(
                new JScrollPane(
                        dataMemoryArea
                ),
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // EXECUTION PANEL
    // =========================================================

    private JPanel createExecutionPanel() {

        JPanel panel =
                new JPanel(
                        new BorderLayout(
                                8,
                                8
                        )
                );

        panel.setBackground(BG);

        panel.add(
                createControls(),
                BorderLayout.NORTH
        );

        panel.add(
                createPipeline(),
                BorderLayout.CENTER
        );

        return panel;
    }

    // =========================================================
    // CONTROLS
    // =========================================================

    private JPanel createControls() {

        JPanel panel =
                createCard(
                        "EXECUTION CONTROL"
                );

        panel.setLayout(
                new FlowLayout(
                        FlowLayout.CENTER,
                        12,
                        8
                )
        );

        JButton load =
                createButton(
                        "LOAD",
                        BLUE
                );

        JButton step =
                createButton(
                        "STEP",
                        BLUE
                );

        JButton run =
                createButton(
                        "RUN",
                        GREEN
                );

        JButton reset =
                createButton(
                        "RESET",
                        ORANGE
                );

        load.addActionListener(
                e -> loadProgram()
        );

        step.addActionListener(
                e -> stepCPU()
        );

        run.addActionListener(
                e -> toggleRun()
        );

        reset.addActionListener(
                e -> resetCPU()
        );

        panel.add(load);

        panel.add(step);

        panel.add(run);

        panel.add(reset);

        return panel;
    }

    // =========================================================
    // PIPELINE
    // =========================================================

    private JPanel createPipeline() {

        JPanel outer =
                createCard(
                        "FETCH → DECODE → EXECUTE"
                );

        outer.setLayout(
                new BorderLayout(
                        8,
                        8
                )
        );

        JPanel stages =
                new JPanel(
                        new GridLayout(
                                1,
                                5,
                                8,
                                8
                        )
                );

        stages.setBackground(PANEL);

        fetchPanel =
                createStage(
                        "FETCH",
                        BLUE
                );

        decodePanel =
                createStage(
                        "DECODE",
                        PURPLE
                );

        executePanel =
                createStage(
                        "EXECUTE",
                        GREEN
                );

        stages.add(
                fetchPanel
        );

        stages.add(
                createArrow()
        );

        stages.add(
                decodePanel
        );

        stages.add(
                createArrow()
        );

        stages.add(
                executePanel
        );

        outer.add(
                stages,
                BorderLayout.NORTH
        );

        outer.add(
                createExecutionDetails(),
                BorderLayout.CENTER
        );

        outer.add(
                createTracePanel(),
                BorderLayout.SOUTH
        );

        return outer;
    }

    // =========================================================
    // PIPELINE STAGE
    // =========================================================

    private JPanel createStage(
            String name,
            Color color
    ) {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(PANEL2);

        panel.setBorder(
                new LineBorder(
                        BORDER,
                        1
                )
        );

        JLabel title =
                new JLabel(
                        name,
                        SwingConstants.CENTER
                );

        title.setForeground(color);

        title.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        15
                )
        );

        panel.add(
                title,
                BorderLayout.CENTER
        );

        JLabel state =
                new JLabel(
                        "WAITING",
                        SwingConstants.CENTER
                );

        state.setForeground(MUTED);

        state.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        11
                )
        );

        panel.add(
                state,
                BorderLayout.SOUTH
        );

        return panel;
    }

    // =========================================================
    // ARROW
    // =========================================================

    private JLabel createArrow() {

        JLabel arrow =
                new JLabel(
                        "→",
                        SwingConstants.CENTER
                );

        arrow.setForeground(BLUE);

        arrow.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        28
                )
        );

        return arrow;
    }

    // =========================================================
    // EXECUTION DETAILS
    // =========================================================

    private JPanel createExecutionDetails() {

        JPanel card =
                createCard(
                        "EXECUTION PANEL"
                );

        card.setLayout(
                new GridLayout(
                        1,
                        5,
                        8,
                        8
                )
        );

        phaseValue =
                createExecutionValue(
                        "IDLE"
                );

        opcodeValue =
                createExecutionValue(
                        "-"
                );

        operandValue =
                createExecutionValue(
                        "-"
                );

        resultValue =
                createExecutionValue(
                        "-"
                );

        descriptionValue =
                createExecutionValue(
                        "-"
                );

        card.add(
                executionField(
                        "CURRENT PHASE",
                        phaseValue
                )
        );

        card.add(
                executionField(
                        "OPCODE",
                        opcodeValue
                )
        );

        card.add(
                executionField(
                        "OPERAND",
                        operandValue
                )
        );

        card.add(
                executionField(
                        "RESULT",
                        resultValue
                )
        );

        card.add(
                executionField(
                        "DESCRIPTION",
                        descriptionValue
                )
        );

        return card;
    }

    // =========================================================
    // EXECUTION TRACE
    // =========================================================

    private JPanel createTracePanel() {

        JPanel card =
                createCard(
                        "EXECUTION TRACE"
                );

        card.setPreferredSize(
                new Dimension(
                        0,
                        125
                )
        );

        card.setLayout(
                new BorderLayout()
        );

        executionTrace =
                createTextArea();

        card.add(
                new JScrollPane(
                        executionTrace
                ),
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // FOOTER
    // =========================================================

    private JPanel createFooter() {

        JPanel footer =
                new JPanel(
                        new GridLayout(
                                1,
                                3,
                                8,
                                8
                        )
                );

        footer.setBackground(BG);

        footer.setBorder(
                new EmptyBorder(
                        0,
                        10,
                        10,
                        10
                )
        );

        footer.setPreferredSize(
                new Dimension(
                        0,
                        145
                )
        );

        footer.add(
                createReadyQueue()
        );

        footer.add(
                createScheduler()
        );

        footer.add(
                createSystemStatus()
        );

        return footer;
    }

    // =========================================================
    // READY QUEUE
    // =========================================================

    private JPanel createReadyQueue() {

        JPanel card =
                createCard(
                        "READY PROCESSES"
                );

        card.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        readyCount =
                createSmallLabel(
                        "Count: 0"
                );

        readyModel =
                new DefaultListModel<>();

        readyQueue =
                new JList<>(
                        readyModel
                );

        readyQueue.setBackground(
                new Color(
                        7,
                        11,
                        16
                )
        );

        readyQueue.setForeground(TEXT);

        readyQueue.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        12
                )
        );

        card.add(
                readyCount,
                BorderLayout.NORTH
        );

        card.add(
                new JScrollPane(
                        readyQueue
                ),
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // SCHEDULER
    // =========================================================

    private JPanel createScheduler() {

        JPanel card =
                createCard(
                        "SCHEDULER"
                );

        card.setLayout(
                new BorderLayout(
                        5,
                        5
                )
        );

        schedulerValue =
                createSmallLabel(
                        "Algorithm: FCFS"
                );

        card.add(
                schedulerValue,
                BorderLayout.NORTH
        );

        JLabel info =
                createSmallLabel(
                        "Scheduling controlled by Core"
                );

        card.add(
                info,
                BorderLayout.CENTER
        );

        return card;
    }

    // =========================================================
    // SYSTEM STATUS
    // =========================================================

    private JPanel createSystemStatus() {

        JPanel card =
                createCard(
                        "SYSTEM STATUS"
                );

        card.setLayout(
                new GridLayout(
                        2,
                        1
                )
        );

        ipcStatus =
                createStatusLabel(
                        "IPC: DISCONNECTED",
                        RED
                );

        coreStatus =
                createStatusLabel(
                        "CORE: WAITING",
                        ORANGE
                );

        card.add(
                ipcStatus
        );

        card.add(
                coreStatus
        );

        return card;
    }

    // =========================================================
    // CARD
    // =========================================================

    private JPanel createCard(
            String title
    ) {

        JPanel panel =
                new JPanel();

        panel.setBackground(PANEL);

        panel.setBorder(
                BorderFactory.createTitledBorder(
                        new LineBorder(BORDER),
                        title,
                        TitledBorder.LEFT,
                        TitledBorder.TOP,
                        new Font(
                                "Segoe UI",
                                Font.BOLD,
                                12
                        ),
                        TEXT
                )
        );

        return panel;
    }

    // =========================================================
    // SMALL LABEL
    // =========================================================

    private JLabel createSmallLabel(
            String text
    ) {

        JLabel label =
                new JLabel(text);

        label.setForeground(TEXT);

        label.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        12
                )
        );

        return label;
    }

    // =========================================================
    // REGISTER VALUE
    // =========================================================

    private JLabel createRegisterValue(
            String text
    ) {

        JLabel label =
                new JLabel(
                        text,
                        SwingConstants.RIGHT
                );

        label.setForeground(TEXT);

        label.setFont(
                new Font(
                        "Consolas",
                        Font.BOLD,
                        13
                )
        );

        return label;
    }

    // =========================================================
    // REGISTER ROW
    // =========================================================

    private JPanel registerRow(
            String name,
            JLabel value
    ) {

        JPanel row =
                new JPanel(
                        new BorderLayout()
                );

        row.setBackground(PANEL);

        JLabel nameLabel =
                new JLabel(name);

        nameLabel.setForeground(MUTED);

        nameLabel.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        row.add(
                nameLabel,
                BorderLayout.WEST
        );

        row.add(
                value,
                BorderLayout.EAST
        );

        return row;
    }

    // =========================================================
    // EXECUTION VALUE
    // =========================================================

    private JLabel createExecutionValue(
            String text
    ) {

        JLabel label =
                new JLabel(
                        text,
                        SwingConstants.CENTER
                );

        label.setForeground(TEXT);

        label.setFont(
                new Font(
                        "Consolas",
                        Font.BOLD,
                        12
                )
        );

        return label;
    }

    // =========================================================
    // EXECUTION FIELD
    // =========================================================

    private JPanel executionField(
            String title,
            JLabel value
    ) {

        JPanel panel =
                new JPanel(
                        new BorderLayout()
                );

        panel.setBackground(PANEL2);

        panel.setBorder(
                new LineBorder(BORDER)
        );

        JLabel name =
                new JLabel(
                        title,
                        SwingConstants.CENTER
                );

        name.setForeground(MUTED);

        name.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        10
                )
        );

        panel.add(
                name,
                BorderLayout.NORTH
        );

        panel.add(
                value,
                BorderLayout.CENTER
        );

        return panel;
    }

    // =========================================================
    // BUTTON
    // =========================================================

    private JButton createButton(
            String text,
            Color color
    ) {

        JButton button =
                new JButton(text);

        button.setForeground(color);

        button.setBackground(
                new Color(
                        12,
                        18,
                        27
                )
        );

        button.setFont(
                new Font(
                        "Segoe UI",
                        Font.BOLD,
                        12
                )
        );

        button.setFocusPainted(false);

        button.setBorder(
                new LineBorder(color)
        );

        button.setPreferredSize(
                new Dimension(
                        100,
                        35
                )
        );

        return button;
    }

    // =========================================================
    // TEXT AREA
    // =========================================================

    private JTextArea createTextArea() {

        JTextArea area =
                new JTextArea();

        area.setEditable(false);

        area.setBackground(
                new Color(
                        7,
                        11,
                        16
                )
        );

        area.setForeground(TEXT);

        area.setFont(
                new Font(
                        "Consolas",
                        Font.PLAIN,
                        12
                )
        );

        area.setBorder(
                new EmptyBorder(
                        6,
                        6,
                        6,
                        6
                )
        );

        return area;
    }

    // =========================================================
    // STATUS LABEL
    // =========================================================

    private JLabel createStatusLabel(
            String text,
            Color color
    ) {

        JLabel label =
                createSmallLabel(text);

        label.setForeground(color);

        label.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        return label;
    }

    // =========================================================
    // INITIALIZE DISPLAY
    // =========================================================

    private void initializeDisplay() {

        loadDefaultProgram();

        executionTrace.setText(
                "[SYSTEM] MS51FB9AE Simulator initialized.\n"
                        + "[SYSTEM] Program memory ready.\n"
                        + "[SYSTEM] Waiting for Core...\n"
        );
    }

    // =========================================================
    // DEFAULT PROGRAM
    // =========================================================

    private void loadDefaultProgram() {

        programModel.clear();

        String[] program = {

                "0000  MOV A,#10",
                "0001  MOV R1,#3",
                "0002  ADD A,R1",
                "0003  PUSH A",
                "0004  MOV A,#99",
                "0005  POP A",
                "0006  ENQUEUE A",
                "0007  ENQUEUE #20",
                "0008  DEQUEUE R2",
                "0009  SUBB A,R1",
                "000A  ANL A,R1",
                "000B  INC R1",
                "000C  SJMP 0",
                "000D  HALT"
        };

        for (String instruction :
                program) {

            programModel.addElement(
                    instruction
            );
        }

        programMemory.setSelectedIndex(0);

        updateInstruction(
                0,
                "MOV A,#10"
        );
    }

    // =========================================================
    // CONNECT TO CORE
    // =========================================================

    private void connectToCore() {

        ipc =
                new IPCClient(this);

        new Thread(
                () -> {

                    boolean success =
                            ipc.connectWithRetry(
                                    20,
                                    500
                            );

                    SwingUtilities.invokeLater(
                            () -> {

                                if (success) {

                                    ipcStatus.setText(
                                            "IPC: CONNECTED"
                                    );

                                    ipcStatus.setForeground(
                                            GREEN
                                    );

                                    coreStatus.setText(
                                            "CORE: READY"
                                    );

                                    coreStatus.setForeground(
                                            GREEN
                                    );

                                    addTrace(
                                            "[IPC] Connected to Core."
                                    );

                                    ipc.ping();

                                } else {

                                    ipcStatus.setText(
                                            "IPC: DISCONNECTED"
                                    );

                                    ipcStatus.setForeground(
                                            RED
                                    );

                                    coreStatus.setText(
                                            "CORE: NOT FOUND"
                                    );

                                    coreStatus.setForeground(
                                            RED
                                    );

                                    addTrace(
                                            "[IPC] Core not found."
                                    );
                                }
                            }
                    );

                },
                "IPC-Connector"
        ).start();
    }

    // =========================================================
    // LOAD PROGRAM
    // =========================================================

    private void loadProgram() {

        if (
                ipc == null ||
                !ipc.isConnected()
        ) {

            addTrace(
                    "[GUI] Core is not connected."
            );

            return;
        }

        String encoded =
                encodeProgramForCore();

        if (
                ipc.loadProgram(
                        encoded
                )
        ) {

            addTrace(
                    "[GUI] LOAD sent to Core."
            );

            coreStatus.setText(
                    "CORE: PROGRAM LOADED"
            );

            coreStatus.setForeground(
                    GREEN
            );
        }
    }

    // =========================================================
    // ENCODE COMPLETE PROGRAM
    // =========================================================

    private String encodeProgramForCore() {

        StringBuilder result =
                new StringBuilder();

        for (
                int i = 0;
                i < programModel.size();
                i++
        ) {

            String line =
                    programModel.get(i);

            int space =
                    line.indexOf(' ');

            if (space >= 0) {

                line =
                        line.substring(
                                space
                        ).trim();
            }

            String encoded =
                    encodeInstruction(
                            line
                    );

            if (encoded.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {

                result.append(';');
            }

            result.append(encoded);
        }

        return result.toString();
    }

    // =========================================================
    // ENCODE INSTRUCTION
    // =========================================================

    private String encodeInstruction(
            String instruction
    ) {

        instruction =
                instruction.trim();

        if (instruction.isEmpty()) {
            return "";
        }

        String[] parts =
                instruction.split(
                        "\\s+",
                        2
                );

        String opcode =
                parts[0].toUpperCase();

        String operands =
                parts.length > 1
                        ? parts[1].trim()
                        : "";

        operands =
                operands.replace(
                        "#",
                        ""
                );

        if (
                opcode.equals("HALT")
        ) {

            return "HALT";
        }

        if (
                opcode.equals("SJMP")
        ) {

            return "SJMP:"
                    + operands;
        }

        if (operands.isEmpty()) {

            return opcode;
        }

        return opcode
                + ":"
                + operands;
    }

    // =========================================================
    // STEP
    // =========================================================

    private void stepCPU() {

        if (
                ipc == null ||
                !ipc.isConnected()
        ) {

            addTrace(
                    "[GUI] Core not connected."
            );

            return;
        }

        cycle++;

        phaseValue.setText(
                "FETCH"
        );

        highlightStage(
                fetchPanel,
                BLUE
        );

        ipc.stepCPU();

        addTrace(
                "[GUI] STEP sent. Cycle "
                        + cycle
        );
    }

    // =========================================================
    // RUN
    // =========================================================

    private void toggleRun() {

        if (running) {

            stopRun();

        } else {

            startRun();
        }
    }

    // =========================================================
    // START RUN
    // =========================================================

    private void startRun() {

        if (
                ipc == null ||
                !ipc.isConnected()
        ) {

            addTrace(
                    "[GUI] Core not connected."
            );

            return;
        }

        running = true;

        cpuStatus.setText(
                "RUNNING"
        );

        cpuStatus.setForeground(
                GREEN
        );

        phaseValue.setText(
                "RUNNING"
        );

        addTrace(
                "[GUI] RUN started."
        );

        runTimer =
                new javax.swing.Timer(
                        300,
                        e -> {

                            if (
                                    !ipc.isConnected()
                            ) {

                                stopRun();

                                return;
                            }

                            cycle++;

                            ipc.stepCPU();
                        }
                );

        runTimer.start();
    }

    // =========================================================
    // STOP RUN
    // =========================================================

    private void stopRun() {

        running = false;

        if (runTimer != null) {

            runTimer.stop();

            runTimer = null;
        }

        if (
                cpuStatus != null &&
                !"HALTED".equals(
                        cpuStatus.getText()
                )
        ) {

            cpuStatus.setText(
                    "STOPPED"
            );

            cpuStatus.setForeground(
                    ORANGE
            );
        }

        addTrace(
                "[GUI] RUN stopped."
        );
    }

    // =========================================================
    // RESET
    // =========================================================

    private void resetCPU() {

        running = false;

        if (runTimer != null) {

            runTimer.stop();

            runTimer = null;
        }

        cycle = 0;

        if (
                ipc != null &&
                ipc.isConnected()
        ) {

            ipc.resetCPU();
        }

        accValue.setText("00H");
        bValue.setText("00H");
        pswValue.setText("00H");

        pcValue.setText("0000H");
        spValue.setText("07H");

        cyValue.setText("0");
        ovValue.setText("0");

        cpuStatus.setText(
                "READY"
        );

        cpuStatus.setForeground(
                TEXT
        );

        for (
                JLabel label :
                registerValues
        ) {

            label.setText(
                    "00H"
            );
        }

        fifoCount.setText(
                "Count: 0 / 16"
        );

        fifoArea.setText(
                "Front → Rear:\n"
                        + "(empty)"
        );

        stackSP.setText(
                "SP: 07H"
        );

        stackStatus.setText(
                "Status: Empty"
        );

        phaseValue.setText(
                "IDLE"
        );

        opcodeValue.setText(
                "-"
        );

        operandValue.setText(
                "-"
        );

        resultValue.setText(
                "-"
        );

        descriptionValue.setText(
                "-"
        );

        programMemory.setSelectedIndex(0);

        updateInstruction(
                0,
                "MOV A,#10"
        );

        addTrace(
                "[GUI] RESET sent."
        );
    }

    // =========================================================
    // CORE MESSAGE
    // =========================================================

    @Override
    public void onMessage(
            String message
    ) {

        SwingUtilities.invokeLater(
                () -> processCoreMessage(
                        message
                )
        );
    }

    // =========================================================
    // PROCESS CORE MESSAGE
    // =========================================================

    private void processCoreMessage(
            String message
    ) {

        if (message == null) {
            return;
        }

        addTrace(
                "[CORE] " + message
        );

        String[] fields =
                message.split(
                        "\\|"
                );

        for (
                String field :
                fields
        ) {

            processField(
                    field.trim()
            );
        }
    }

    // =========================================================
    // PROCESS FIELD
    // =========================================================

    private void processField(
            String field
    ) {

        if (
                field.equals(
                        "CORE_READY"
                )
        ) {

            coreStatus.setText(
                    "CORE: READY"
            );

            coreStatus.setForeground(
                    GREEN
            );

            return;
        }

        if (
                field.equals(
                        "PONG"
                )
        ) {

            coreStatus.setText(
                    "CORE: ONLINE"
            );

            coreStatus.setForeground(
                    GREEN
            );

            return;
        }

        if (
                field.equals(
                        "STEP"
                )
        ) {

            phaseValue.setText(
                    "FETCH"
            );

            highlightStage(
                    fetchPanel,
                    BLUE
            );

            return;
        }

        if (
                field.startsWith(
                        "OLD_PC="
                )
        ) {

            int oldPC =
                    parseInt(
                            valueOf(field)
                    );

            updatePipelineFetch(
                    oldPC
            );

            return;
        }

        if (
                field.startsWith(
                        "PC="
                )
        ) {

            int pc =
                    parseInt(
                            valueOf(field)
                    );

            pcValue.setText(
                    formatHex(
                            pc,
                            4
                    )
            );

            return;
        }

        if (
                field.startsWith(
                        "A="
                )
        ) {

            int value =
                    parseInt(
                            valueOf(field)
                    );

            accValue.setText(
                    formatHex(
                            value,
                            2
                    )
            );

            return;
        }

        if (
                field.startsWith(
                        "R1="
                )
        ) {

            int value =
                    parseInt(
                            valueOf(field)
                    );

            registerValues[1].setText(
                    formatHex(
                            value,
                            2
                    )
            );

            return;
        }

        if (
                field.startsWith(
                        "CY="
                )
        ) {

            cyValue.setText(
                    valueOf(field)
            );

            return;
        }

        if (
                field.startsWith(
                        "OV="
                )
        ) {

            ovValue.setText(
                    valueOf(field)
            );

            return;
        }

        if (
                field.startsWith(
                        "SP="
                )
        ) {

            int value =
                    parseInt(
                            valueOf(field)
                    );

            String hex =
                    formatHex(
                            value,
                            2
                    );

            spValue.setText(hex);

            stackSP.setText(
                    "SP: " + hex
            );

            return;
        }

        if (
                field.startsWith(
                        "QUEUE="
                )
        ) {

            int count =
                    parseInt(
                            valueOf(field)
                    );

            fifoCount.setText(
                    "Count: "
                            + count
                            + " / 16"
            );

            fifoStatus.setText(
                    "Status: OK"
            );

            return;
        }

        if (
                field.startsWith(
                        "RUNNING="
                )
        ) {

            int value =
                    parseInt(
                            valueOf(field)
                    );

            if (value == 1) {

                cpuStatus.setText(
                        "RUNNING"
                );

                cpuStatus.setForeground(
                        GREEN
                );

            } else {

                cpuStatus.setText(
                        "HALTED"
                );

                cpuStatus.setForeground(
                        RED
                );

                if (running) {

                    running = false;

                    if (runTimer != null) {

                        runTimer.stop();

                        runTimer = null;
                    }
                }
            }

            return;
        }

        if (
                field.startsWith(
                        "INSTRUCTION="
                )
        ) {

            String instruction =
                    valueOf(field);

            updateInstruction(
                    getCurrentPC(),
                    instruction
            );

            updatePipelineInstruction(
                    instruction
            );

            return;
        }

        if (
                field.startsWith(
                        "LOADED"
                )
        ) {

            coreStatus.setText(
                    "CORE: PROGRAM LOADED"
            );

            coreStatus.setForeground(
                    GREEN
            );

            return;
        }

        if (
                field.startsWith(
                        "HALTED"
                )
        ) {

            running = false;

            if (runTimer != null) {

                runTimer.stop();

                runTimer = null;
            }

            cpuStatus.setText(
                    "HALTED"
            );

            cpuStatus.setForeground(
                    RED
            );

            phaseValue.setText(
                    "HALTED"
            );

            return;
        }

        if (
                field.startsWith(
                        "ERROR"
                )
        ) {

            coreStatus.setText(
                    "CORE: ERROR"
            );

            coreStatus.setForeground(
                    RED
            );

            addTrace(
                    "[ERROR] " + field
            );
        }
    }

    // =========================================================
    // FETCH
    // =========================================================

    private void updatePipelineFetch(
            int pc
    ) {

        highlightStage(
                fetchPanel,
                BLUE
        );

        phaseValue.setText(
                "FETCH"
        );

        String instruction =
                getProgramInstruction(
                        pc
                );

        opcodeValue.setText(
                getOpcode(
                        instruction
                )
        );

        operandValue.setText(
                getOperand(
                        instruction
                )
        );

        descriptionValue.setText(
                getDescription(
                        getOpcode(
                                instruction
                        )
                )
        );
    }

    // =========================================================
    // DECODE / EXECUTE
    // =========================================================

    private void updatePipelineInstruction(
            String instruction
    ) {

        highlightStage(
                decodePanel,
                PURPLE
        );

        String opcode =
                getOpcode(
                        instruction
                );

        String operand =
                getOperand(
                        instruction
                );

        opcodeValue.setText(
                opcode
        );

        operandValue.setText(
                operand
        );

        descriptionValue.setText(
                getDescription(
                        opcode
                )
        );

        phaseValue.setText(
                "DECODE"
        );

        javax.swing.Timer timer =
                new javax.swing.Timer(
                        150,
                        e -> {

                            highlightStage(
                                    executePanel,
                                    GREEN
                            );

                            phaseValue.setText(
                                    "EXECUTE"
                            );

                            resultValue.setText(
                                    "Core state updated"
                            );
                        }
                );

        timer.setRepeats(false);

        timer.start();
    }

    // =========================================================
    // HIGHLIGHT STAGE
    // =========================================================

    private void highlightStage(
            JPanel panel,
            Color color
    ) {

        if (panel == null) {
            return;
        }

        panel.setBorder(
                new LineBorder(
                        color,
                        2
                )
        );

        panel.repaint();
    }

    // =========================================================
    // UPDATE INSTRUCTION
    // =========================================================

    private void updateInstruction(
            int pc,
            String instruction
    ) {

        if (
                programModel == null ||
                programModel.isEmpty()
        ) {

            return;
        }

        int index =
                Math.max(
                        0,
                        Math.min(
                                pc,
                                programModel.size() - 1
                        )
                );

        programMemory.setSelectedIndex(
                index
        );

        programMemory.ensureIndexIsVisible(
                index
        );

        currentInstruction.setText(
                "Current Instruction: "
                        + instruction
        );

        instructionCategory.setText(
                "Category: "
                        + getCategory(
                                instruction
                        )
        );
    }

    // =========================================================
    // CURRENT PC
    // =========================================================

    private int getCurrentPC() {

        try {

            String text =
                    pcValue.getText()
                            .replace(
                                    "H",
                                    ""
                            );

            return Integer.parseInt(
                    text,
                    16
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // GET PROGRAM INSTRUCTION
    // =========================================================

    private String getProgramInstruction(
            int pc
    ) {

        if (
                pc < 0 ||
                pc >= programModel.size()
        ) {

            return "HALT";
        }

        String line =
                programModel.get(pc);

        int space =
                line.indexOf(' ');

        if (space >= 0) {

            return line.substring(
                    space
            ).trim();
        }

        return line;
    }

    // =========================================================
    // OPCODE
    // =========================================================

    private String getOpcode(
            String instruction
    ) {

        if (
                instruction == null ||
                instruction.trim().isEmpty()
        ) {

            return "-";
        }

        String[] parts =
                instruction.trim()
                        .split(
                                "\\s+"
                        );

        return parts[0].toUpperCase();
    }

    // =========================================================
    // OPERAND
    // =========================================================

    private String getOperand(
            String instruction
    ) {

        if (instruction == null) {
            return "-";
        }

        String[] parts =
                instruction.trim()
                        .split(
                                "\\s+",
                                2
                        );

        if (parts.length < 2) {
            return "-";
        }

        return parts[1];
    }

    // =========================================================
    // CATEGORY
    // =========================================================

    private String getCategory(
            String instruction
    ) {

        String opcode =
                getOpcode(
                        instruction
                );

        switch (opcode) {

            case "MOV":
                return "Data Transfer";

            case "ADD":
            case "SUBB":
            case "INC":
                return "Arithmetic";

            case "ANL":
                return "Logical";

            case "PUSH":
            case "POP":
                return "Stack Operation";

            case "ENQUEUE":
            case "DEQUEUE":
                return "FIFO Operation";

            case "SJMP":
                return "Branch";

            case "HALT":
                return "Program Termination";

            default:
                return "Instruction";
        }
    }

    // =========================================================
    // DESCRIPTION
    // =========================================================

    private String getDescription(
            String opcode
    ) {

        if (opcode == null) {
            return "-";
        }

        switch (
                opcode.toUpperCase()
        ) {

            case "MOV":
                return "Move data";

            case "ADD":
                return "Add operands";

            case "SUBB":
                return "Subtract with borrow";

            case "ANL":
                return "Logical AND";

            case "INC":
                return "Increment register";

            case "PUSH":
                return "Push value onto stack";

            case "POP":
                return "Pop value from stack";

            case "ENQUEUE":
                return "Insert into FIFO";

            case "DEQUEUE":
                return "Remove from FIFO";

            case "SJMP":
                return "Short jump";

            case "HALT":
                return "Stop CPU";

            default:
                return "Execute instruction";
        }
    }

    // =========================================================
    // VALUE AFTER =
    // =========================================================

    private String valueOf(
            String field
    ) {

        int index =
                field.indexOf('=');

        if (index < 0) {
            return "";
        }

        return field.substring(
                index + 1
        ).trim();
    }

    // =========================================================
    // PARSE INTEGER
    // =========================================================

    private int parseInt(
            String value
    ) {

        try {

            value =
                    value.trim()
                            .toUpperCase();

            if (
                    value.endsWith("H")
            ) {

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        );

                return Integer.parseInt(
                        value,
                        16
                );
            }

            return Integer.parseInt(
                    value
            );

        } catch (Exception e) {

            return 0;
        }
    }

    // =========================================================
    // FORMAT HEX
    // =========================================================

    private String formatHex(
            int value,
            int digits
    ) {

        return String.format(
                "%0" + digits + "XH",
                value
        );
    }

    // =========================================================
    // TRACE
    // =========================================================

    private void addTrace(
            String message
    ) {

        if (executionTrace == null) {
            return;
        }

        String time =
                new java.text.SimpleDateFormat(
                        "HH:mm:ss"
                ).format(
                        new Date()
                );

        executionTrace.append(
                "["
                        + time
                        + "] "
                        + message
                        + "\n"
        );

        executionTrace.setCaretPosition(
                executionTrace.getDocument()
                        .getLength()
        );
    }

    // =========================================================
    // CLOSE
    // =========================================================

    @Override
    public void dispose() {

        if (runTimer != null) {

            runTimer.stop();

            runTimer = null;
        }

        if (ipc != null) {

            ipc.disconnect();
        }

        super.dispose();
    }

    // =========================================================
    // MAIN
    // =========================================================

    public static void main(
            String[] args
    ) {

        SwingUtilities.invokeLater(
                () -> {

                    UIProcess ui =
                            new UIProcess();

                    ui.setVisible(true);
                }
        );
    }
}

package gui;

import cpu_core.CPU;
import cpu_core.CpuSnapshot;
import cpu_core.Instruction;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.*;

public class MainWindow extends JFrame {

    private CPU cpu;

    private ArrayList<Instruction> program;

    private ControlPanel controlPanel;
    private CpuStatePanel cpuStatePanel;
    private ProgramPanel programPanel;
    private ExecutionTracePanel tracePanel;
    private StackPanel stackPanel;
    private QueuePanel queuePanel;
    private MemoryPanel memoryPanel;

    // Every panel that displays CPU state, registered once here.
    // updateAllPanels() never needs to change when a panel is added,
    // removed, or reworked internally -- only this list does.
    private final List<CpuView> views = new ArrayList<>();

    private JLabel statusBar;

    private Timer timer;

    public MainWindow() {

        cpu = new CPU();

        program = createProgram();

        setTitle(
                "Nuvoton MS51FB9AE Microcontroller Simulator"
        );

        setSize(1300, 800);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        setLocationRelativeTo(null);

        controlPanel =
                new ControlPanel();

        cpuStatePanel =
                new CpuStatePanel();

        programPanel =
                new ProgramPanel();

        tracePanel =
                new ExecutionTracePanel();

        stackPanel = new StackPanel();
        queuePanel = new QueuePanel();
        memoryPanel = new MemoryPanel();

        views.add(cpuStatePanel);
        views.add(stackPanel);
        views.add(queuePanel);
        views.add(memoryPanel);

        statusBar =
                new JLabel(
                        " Status: Ready"
                );

        createLayout();

        connectButtons();

        setVisible(true);
    }

    private void createLayout() {

        setLayout(
                new BorderLayout(8, 8)
        );

        JLabel title =
                new JLabel(
                        "NUVOTON MS51FB9AE MICROCONTROLLER SIMULATOR",
                        JLabel.CENTER
                );

        title.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        20
                )
        );

        add(
                title,
                BorderLayout.NORTH
        );

        JPanel infoGrid =
                new JPanel(
                        new GridLayout(2, 2, 8, 8)
                );

        infoGrid.add(cpuStatePanel);
        infoGrid.add(stackPanel);
        infoGrid.add(queuePanel);
        infoGrid.add(memoryPanel);

        JPanel center =
                new JPanel(
                        new GridLayout(
                                1,
                                2,
                                10,
                                10
                        )
                );

        center.add(programPanel);
        center.add(infoGrid);

        add(
                center,
                BorderLayout.CENTER
        );

        JPanel bottom =
                new JPanel(
                        new BorderLayout()
                );

        bottom.add(
                controlPanel,
                BorderLayout.NORTH
        );

        bottom.add(
                tracePanel,
                BorderLayout.CENTER
        );

        bottom.add(
                statusBar,
                BorderLayout.SOUTH
        );

        add(
                bottom,
                BorderLayout.SOUTH
        );
    }

    // Demonstration program covering all 12 instructions: MOV_A_DATA,
    // MOV_RN_DATA, ADD, SUBB, ANL, INC, SJMP, HALT, plus the Week 3
    // additions PUSH, POP, ENQUEUE, DEQUEUE.
    private ArrayList<Instruction> createProgram() {

        ArrayList<Instruction> list =
                new ArrayList<>();

        list.add(
                new Instruction(
                        "MOV_A_DATA",
                        Arrays.asList("10")
                )
        );

        list.add(
                new Instruction(
                        "MOV_RN_DATA",
                        Arrays.asList("R1", "3")
                )
        );

        list.add(
                new Instruction(
                        "ADD",
                        Arrays.asList("R1")
                )
        );

        list.add(
                new Instruction(
                        "PUSH",
                        Arrays.asList("A")
                )
        );

        list.add(
                new Instruction(
                        "MOV_A_DATA",
                        Arrays.asList("99")
                )
        );

        list.add(
                new Instruction(
                        "POP",
                        Arrays.asList("A")
                )
        );

        list.add(
                new Instruction(
                        "ENQUEUE",
                        Arrays.asList("A")
                )
        );

        list.add(
                new Instruction(
                        "ENQUEUE",
                        Arrays.asList("#20")
                )
        );

        list.add(
                new Instruction(
                        "DEQUEUE",
                        Arrays.asList("R2")
                )
        );

        list.add(
                new Instruction(
                        "SUBB",
                        Arrays.asList("R1")
                )
        );

        list.add(
                new Instruction(
                        "ANL",
                        Arrays.asList("R1")
                )
        );

        list.add(
                new Instruction(
                        "INC",
                        Arrays.asList("R1")
                )
        );

        list.add(
                new Instruction(
                        "SJMP",
                        Arrays.asList("0")
                )
        );

        list.add(
                new Instruction(
                        "HALT",
                        Collections.emptyList()
                )
        );

        return list;
    }

    private void connectButtons() {

        controlPanel
                .getLoadButton()
                .addActionListener(
                        e -> loadProgram()
                );

        controlPanel
                .getStepButton()
                .addActionListener(
                        e -> stepProgram()
                );

        controlPanel
                .getRunButton()
                .addActionListener(
                        e -> runProgram()
                );

        controlPanel
                .getResetButton()
                .addActionListener(
                        e -> resetProgram()
                );
    }

    private void loadProgram() {

        cpu.loadProgram(program);

        programPanel.showProgram(program);

        updateAllPanels("Ready");

        programPanel.showNextInstruction(
                cpu.getPC(),
                program
        );

        tracePanel.clearTrace();

        statusBar.setText(
                " Status: Program loaded successfully."
        );
    }

    private void stepProgram() {

        if (!cpu.isRunning()) {

            statusBar.setText(
                    " Status: Program halted."
            );

            stopTimer();

            return;
        }

        int oldPC =
                cpu.getPC();

        Instruction instruction;

        try {
            instruction = cpu.step();
        } catch (RuntimeException ex) {

            stopTimer();

            tracePanel.addTrace(
                    "ERROR   : " + ex.getMessage()
            );

            statusBar.setText(
                    " Status: Error - " + ex.getMessage()
            );

            return;
        }

        if (instruction == null) {
            return;
        }

        programPanel.showCurrentInstruction(
                oldPC,
                instruction
        );

        tracePanel.addTrace(
        "FETCH   \u2713 : PC = "
                + String.format("%04XH", oldPC)
);

tracePanel.addTrace(
        "DECODE  \u2713 : "
                + instruction
);

tracePanel.addTrace(
        "EXECUTE \u2713 : "
                + instruction.mnemonic
);
        tracePanel.addTrace(
                "PC      : "
                        + String.format(
                        "%04XH",
                        cpu.getPC()
                )
        );

        tracePanel.addTrace(
                "A       : "
                        + String.format(
                        "%02XH",
                        cpu.getA()
                )
        );

        tracePanel.addTrace(
                "R1      : "
                        + String.format(
                        "%02XH",
                        cpu.getR(1)
                )
        );

        tracePanel.addTrace(
                "CY      : "
                        + (cpu.isCY() ? "1" : "0")
        );

        tracePanel.addTrace(
                "OV      : "
                        + (cpu.isOV() ? "1" : "0")
        );

        tracePanel.addTrace(
                "SP      : "
                        + String.format(
                        "%02XH",
                        cpu.getSP()
                )
        );

        tracePanel.addTrace(
                "Queue   : "
                        + cpu.getQueue().getCount()
                        + "/"
                        + cpu.getQueue().getCapacity()
        );

        tracePanel.addTrace(
                "--------------------------------"
        );

        updateAllPanels(
                cpu.isRunning()
                        ? "Running"
                        : "Halted"
        );

        if (cpu.isRunning()) {

            programPanel.showNextInstruction(
                    cpu.getPC(),
                    program
            );

        } else {

            stopTimer();

            statusBar.setText(
                    " Status: Program halted."
            );
        }
    }

    private void runProgram() {

        if (!cpu.isRunning()) {

            statusBar.setText(
                    " Status: Press LOAD or RESET."
            );

            return;
        }

        if (timer == null) {

            timer =
                    new Timer(
                            200,
                            e -> stepProgram()
                    );
        }

        timer.start();

        statusBar.setText(
                " Status: Running..."
        );
    }

    private void resetProgram() {

        stopTimer();

        cpu.reset();

        tracePanel.clearTrace();

        updateAllPanels("Ready");

        programPanel.showNextInstruction(
                cpu.getPC(),
                program
        );

        statusBar.setText(
                " Status: CPU Reset."
        );
    }

    private void updateAllPanels(String status) {
        CpuSnapshot snapshot = cpu.getSnapshot();
        for (CpuView view : views) {
            view.refresh(snapshot, status);
        }
    }

    private void stopTimer() {

        if (
                timer != null &&
                timer.isRunning()
        ) {
            timer.stop();
        }
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(
                () -> new MainWindow()
        );
    }
}
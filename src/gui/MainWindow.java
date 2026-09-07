package gui;

import cpu_core.CPU;
import cpu_core.Instruction;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MainWindow extends JFrame {

    private CPU cpu;
    private ArrayList<Instruction> program;

    private ControlPanel controlPanel;
    private CpuStatePanel cpuStatePanel;
    private ProgramPanel programPanel;
    private ExecutionTracePanel tracePanel;
    private JLabel statusBar;

    private Timer runTimer;
    private boolean programLoaded;

    public MainWindow() {

        cpu = new CPU();
        program = createDemoProgram();

        setTitle("MS51FB9AE Microcontroller Simulator");
        setSize(950, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        controlPanel = new ControlPanel();
        cpuStatePanel = new CpuStatePanel();
        programPanel = new ProgramPanel();
        tracePanel = new ExecutionTracePanel();

        statusBar = new JLabel(
            " Status: Click Load to begin."
        );

        createLayout();
        connectButtons();

        setVisible(true);
    }

    private void createLayout() {

        setLayout(new BorderLayout(8, 8));

        JPanel middlePanel =
            new JPanel(new GridLayout(1, 2, 8, 8));

        middlePanel.add(programPanel);
        middlePanel.add(cpuStatePanel);

        JPanel bottomPanel =
            new JPanel(new BorderLayout());

        bottomPanel.add(
            controlPanel,
            BorderLayout.CENTER
        );

        bottomPanel.add(
            statusBar,
            BorderLayout.SOUTH
        );

        add(
            tracePanel,
            BorderLayout.NORTH
        );

        add(
            middlePanel,
            BorderLayout.CENTER
        );

        add(
            bottomPanel,
            BorderLayout.SOUTH
        );
    }

    private void connectButtons() {

        controlPanel.getLoadButton()
            .addActionListener(e -> loadProgram());

        controlPanel.getResetButton()
            .addActionListener(e -> resetCpu());

        controlPanel.getStepButton()
            .addActionListener(e -> stepProgram());

        controlPanel.getRunButton()
            .addActionListener(e -> runProgram());
    }

    private ArrayList<Instruction> createDemoProgram() {

        ArrayList<Instruction> demoProgram =
            new ArrayList<>();

        // 1. Data Transfer
        demoProgram.add(
            new Instruction(
                "MOV_A_DATA",
                Arrays.asList("10")
            )
        );

        // 2. Data Transfer
        demoProgram.add(
            new Instruction(
                "MOV_DIRECT_A",
                Arrays.asList("48")
            )
        );

        // 3. Arithmetic
        demoProgram.add(
            new Instruction(
                "ADD",
                Arrays.asList("R1")
            )
        );

        // 4. Arithmetic
        demoProgram.add(
            new Instruction(
                "SUBB",
                Arrays.asList("R1")
            )
        );

        // 5. Logical Operation
        demoProgram.add(
            new Instruction(
                "ANL",
                Arrays.asList("R1")
            )
        );

        // 6. Increment
        demoProgram.add(
            new Instruction(
                "INC",
                Arrays.asList("R1")
            )
        );

        // 7. Control Flow
        demoProgram.add(
            new Instruction(
                "SJMP",
                Arrays.asList("0")
            )
        );

        // 8. Program Termination
        demoProgram.add(
            new Instruction(
                "HALT",
                Collections.emptyList()
            )
        );

        return demoProgram;
    }

    private void loadProgram() {

        cpu.loadProgram(program);

        // Initial R1 value
        cpu.setR(1, 3);

        programLoaded = true;

        tracePanel.clearTrace();

        programPanel.showProgram(program);

        programPanel.showNextInstruction(
            cpu.getPC(),
            program
        );

        programPanel.showCategory("None");

        cpuStatePanel.updateState(
            cpu,
            "Ready"
        );

        statusBar.setText(
            " Program loaded successfully."
        );
    }

    private void resetCpu() {

        if (!programLoaded) {

            statusBar.setText(
                " Load the program first."
            );

            return;
        }

        stopRunningProgram();

        cpu.reset();

        cpu.setR(1, 3);

        tracePanel.clearTrace();

        programPanel.showCurrentInstruction(
            -1,
            null
        );

        programPanel.showCategory("None");

        cpuStatePanel.updateState(
            cpu,
            "Ready"
        );

        statusBar.setText(
            " CPU reset complete."
        );
    }

    private void stepProgram() {

        if (!programLoaded) {

            statusBar.setText(
                " Load the program first."
            );

            return;
        }

        if (!cpu.isRunning()) {

            stopRunningProgram();

            cpuStatePanel.updateState(
                cpu,
                "Halted"
            );

            programPanel.showCategory("None");

            statusBar.setText(
                " Program has halted. "
                + "Click Reset to run again."
            );

            return;
        }

        int beforePC = cpu.getPC();

        int beforeA = cpu.getA();

        int[] beforeRegisters =
            new int[8];

        for (int i = 0; i < 8; i++) {

            beforeRegisters[i] =
                cpu.getR(i);
        }

        Instruction instruction =
            cpu.step();

        if (instruction == null) {

            statusBar.setText(
                " No instruction available."
            );

            return;
        }

        // Get instruction category
        String category =
            cpu.getInstructionCategory(
                instruction.mnemonic
            );

        // Display category in ProgramPanel
        programPanel.showCategory(
            category
        );

        // Show execution trace
        showTrace(
            beforePC,
            beforeA,
            beforeRegisters,
            instruction
        );

        String state =
            cpu.isRunning()
                ? "Running"
                : "Halted";

        cpuStatePanel.updateState(
            cpu,
            state
        );

        programPanel.showNextInstruction(
            cpu.getPC(),
            program
        );

        if (!cpu.isRunning()) {

            stopRunningProgram();

            statusBar.setText(
                " Program halted."
            );

        } else {

            statusBar.setText(
                " Executed instruction at PC = "
                + String.format(
                    "%04X",
                    beforePC
                )
            );
        }
    }

    private void showTrace(
        int beforePC,
        int beforeA,
        int[] beforeRegisters,
        Instruction instruction
    ) {

        String category =
            cpu.getInstructionCategory(
                instruction.mnemonic
            );

        tracePanel.addTrace(
            "Instruction: "
            + instruction.toString()
        );

        tracePanel.addTrace(
            "Category: "
            + category
        );

        tracePanel.addTrace("");

        tracePanel.addTrace(
            "FETCH   ✓  Read instruction at PC = "
            + String.format(
                "%04X",
                beforePC
            )
        );

        tracePanel.addTrace(
            "DECODE  ✓  Instruction recognised"
        );

        tracePanel.addTrace(
            "EXECUTE ✓"
        );

        tracePanel.addTrace("");

        if (beforeA != cpu.getA()) {

            tracePanel.addTrace(
                "A: "
                + String.format(
                    "%02X",
                    beforeA
                )
                + " -> "
                + String.format(
                    "%02X",
                    cpu.getA()
                )
            );
        }

        for (int i = 0; i < 8; i++) {

            if (
                beforeRegisters[i]
                != cpu.getR(i)
            ) {

                tracePanel.addTrace(
                    "R" + i + ": "
                    + String.format(
                        "%02X",
                        beforeRegisters[i]
                    )
                    + " -> "
                    + String.format(
                        "%02X",
                        cpu.getR(i)
                    )
                );
            }
        }

        tracePanel.addTrace(
            "PC: "
            + String.format(
                "%04X",
                beforePC
            )
            + " -> "
            + String.format(
                "%04X",
                cpu.getPC()
            )
        );

        tracePanel.addTrace(
            "CY = "
            + cpu.isCY()
            + " | OV = "
            + cpu.isOV()
        );

        tracePanel.addTrace(
            "----------------------------------------"
        );
    }

    private void runProgram() {

        if (!programLoaded) {

            statusBar.setText(
                " Load the program first."
            );

            return;
        }

        if (!cpu.isRunning()) {

            statusBar.setText(
                " Program has halted. "
                + "Click Reset to run again."
            );

            return;
        }

        if (runTimer == null) {

            runTimer =
                new Timer(
                    700,
                    e -> stepProgram()
                );
        }

        runTimer.start();

        statusBar.setText(
            " Program is running..."
        );
    }

    private void stopRunningProgram() {

        if (
            runTimer != null
            && runTimer.isRunning()
        ) {

            runTimer.stop();
        }
    }

    public static void main(
        String[] args
    ) {

        SwingUtilities.invokeLater(
            () -> new MainWindow()
        );
    }
}

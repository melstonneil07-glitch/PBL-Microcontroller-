package gui;

import cpu_core.CPU;
import cpu_core.Instruction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

public class MainWindow extends JFrame {

    private CPU cpu;

    private ArrayList<Instruction> program;

    private ControlPanel controlPanel;
    private CpuStatePanel cpuStatePanel;
    private ProgramPanel programPanel;
    private ExecutionTracePanel tracePanel;

    private JLabel statusBar;

    private Timer timer;

    public MainWindow() {

        cpu = new CPU();

        program = createProgram();

        setTitle(
                "Nuvoton MS51FB9AE Microcontroller Simulator"
        );

        setSize(1100, 750);

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
        center.add(cpuStatePanel);

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
                        Arrays.asList("+1")
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

        cpuStatePanel.updateState(
                cpu,
                "Ready"
        );

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

        Instruction instruction =
                cpu.step();

        if (instruction == null) {
            return;
        }

        programPanel.showCurrentInstruction(
                oldPC,
                instruction
        );

        tracePanel.addTrace(
                "FETCH   : PC = "
                        + String.format(
                        "%04XH",
                        oldPC
                )
        );

        tracePanel.addTrace(
                "DECODE  : "
                        + instruction
        );

        tracePanel.addTrace(
                "EXECUTE : "
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
                "--------------------------------"
        );

        cpuStatePanel.updateState(
                cpu,
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
                            700,
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

        cpuStatePanel.updateState(
                cpu,
                "Ready"
        );

        programPanel.showNextInstruction(
                cpu.getPC(),
                program
        );

        statusBar.setText(
                " Status: CPU Reset."
        );
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

package gui;

import cpu_core.Instruction;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MainWindow extends JFrame
{
    // =========================================================
    // JAVA SOCKET IPC
    // =========================================================

    private static final String CORE_HOST = "127.0.0.1";
    private static final int CORE_PORT = 5000;

    private Socket coreSocket;
    private BufferedReader coreReader;
    private BufferedWriter coreWriter;

    // =========================================================
    // GUI
    // =========================================================

    private ArrayList<Instruction> program;

    private ControlPanel controlPanel;

    private ProgramPanel programPanel;

    private ExecutionTracePanel tracePanel;

    private StackPanel stackPanel;

    private QueuePanel queuePanel;

    private MemoryPanel memoryPanel;

    private CpuStatePanel cpuStatePanel;

    private JLabel statusBar;

    private Timer timer;

    private int currentPC = 0;

    private boolean coreReady = false;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public MainWindow()
    {
        program = createProgram();

        setTitle(
                "Nuvoton MS51FB9AE Microcontroller Simulator"
        );

        setSize(1300, 800);

        setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE
        );

        addWindowListener(
                new java.awt.event.WindowAdapter()
                {
                    @Override
                    public void windowClosing(
                            java.awt.event.WindowEvent e
                    )
                    {
                        if (coreWriter != null)
                        {
                            sendCommand("SHUTDOWN");
                        }

                        cleanupIPC();
                    }
                }
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

        stackPanel =
                new StackPanel();

        queuePanel =
                new QueuePanel();

        memoryPanel =
                new MemoryPanel();

        statusBar =
                new JLabel(
                        " Status: Connecting to Core..."
                );

        createLayout();

        connectButtons();

        connectToCore();

        setVisible(true);

        startReceiverTimer();
    }
    // =========================================================
    // GUI LAYOUT
    // =========================================================
    private void createLayout()
    {
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
                        new GridLayout(
                                2,
                                2,
                                8,
                                8
                        )
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
    // PROGRAM
    // =========================================================
    private ArrayList<Instruction> createProgram()
    {
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
    // =========================================================
    // BUTTONS
    // =========================================================
    private void connectButtons()
    {
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
    // =========================================================
    // CONNECT TO CORE
    // =========================================================
    private void connectToCore()
    {
        try
        {
            coreSocket = new Socket(CORE_HOST, CORE_PORT);

            coreReader = new BufferedReader(
                    new InputStreamReader(
                            coreSocket.getInputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            coreWriter = new BufferedWriter(
                    new OutputStreamWriter(
                            coreSocket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            coreReady = false;

            statusBar.setText(
                    " Status: Connected to Core. Waiting for READY..."
            );

            sendCommand("PING");
        }
        catch (IOException ex)
        {
            statusBar.setText(
                    " Status: Core not available. Start Core first."
            );

            System.err.println(
                    "Could not connect to Core: " + ex.getMessage()
            );
        }
    }
    // =========================================================
    // LOAD
    // =========================================================
    private void loadProgram()
    {
        if (!isConnected())
        {
            statusBar.setText(
                    " Status: Core is not connected."
            );

            return;
        }

        String serializedProgram =
                serializeProgram();

        sendCommand(
                "LOAD|" + serializedProgram
        );

        currentPC = 0;

        programPanel.showProgram(program);

        programPanel.showNextInstruction(
                currentPC,
                program
        );

        tracePanel.clearTrace();

        statusBar.setText(
                " Status: Program sent to Core."
        );
    }
    // STEP
    // =========================================================
    private void stepProgram()
    {
        if (!isConnected())
        {
            statusBar.setText(
                    " Status: Core is not connected."
            );

            return;
        }

        sendCommand("STEP");

        statusBar.setText(
                " Status: STEP command sent to Core."
        );
    }
    // =========================================================
    // RUN
    // =========================================================
    private void runProgram()
    {
        if (!isConnected())
        {
            statusBar.setText(
                    " Status: Core is not connected."
            );

            return;
        }

        if (timer == null)
        {
            timer =
                    new Timer(
                            200,
                            e -> sendCommand("STEP")
                    );
        }

        timer.start();

        statusBar.setText(
                " Status: Running through Core..."
        );
    }
    // =========================================================
    // RESET
    // =========================================================
    private void resetProgram()
    {
        stopTimer();

        if (!isConnected())
        {
            statusBar.setText(
                    " Status: Core is not connected."
            );

            return;
        }

        sendCommand("RESET");

        currentPC = 0;

        tracePanel.clearTrace();

        programPanel.showNextInstruction(
                currentPC,
                program
        );

        statusBar.setText(
                " Status: Reset command sent to Core."
        );
    }
    // =========================================================
    // SEND COMMAND TO CORE
    // =========================================================
    private void sendCommand(String command)
    {
        if (coreWriter == null)
        {
            return;
        }

        try
        {
            coreWriter.write(command);
            coreWriter.newLine();
            coreWriter.flush();
        }
        catch (IOException ex)
        {
            coreReady = false;

            statusBar.setText(
                    " Status: Failed to send command to Core."
            );

            System.err.println(
                    "Core send error: " + ex.getMessage()
            );
        }
    }
    // =========================================================
    // RECEIVE CORE UPDATES
    // =========================================================
    private void startReceiverTimer()
    {
        Timer receiverTimer =
                new Timer(100, e -> receiveCoreMessages());

        receiverTimer.start();
    }

    private void receiveCoreMessages()
    {
        if (coreReader == null)
        {
            return;
        }

        try
        {
            while (coreReader.ready())
            {
                String message = coreReader.readLine();

                if (message == null)
                {
                    coreReady = false;
                    statusBar.setText(
                            " Status: Core disconnected."
                    );
                    return;
                }

                processCoreMessage(message);
            }
        }
        catch (IOException ex)
        {
            coreReady = false;

            statusBar.setText(
                    " Status: Core connection lost."
            );
        }
    }
    // =========================================================
    // PROCESS CORE MESSAGE
    // =========================================================
    private void processCoreMessage(
            String message
    )
    {
        System.out.println(
                "Core -> GUI: " + message
        );

        if (message.equals("CORE_READY"))
        {
            coreReady = true;

            statusBar.setText(
                    " Status: Core READY."
            );

            return;
        }

        if (message.equals("PONG"))
        {
            coreReady = true;

            statusBar.setText(
                    " Status: Core connected."
            );

            return;
        }

        if (message.startsWith("LOADED|"))
        {
            tracePanel.addTrace(
                    "CORE     : Program loaded"
            );

            statusBar.setText(
                    " Status: Program loaded in Core."
            );

            return;
        }

        if (message.startsWith("RESET|"))
        {
            currentPC = 0;

            tracePanel.addTrace(
                    "CORE     : CPU Reset"
            );

            tracePanel.addTrace(
                    "PC       : 0000H"
            );

            programPanel.showNextInstruction(
                    currentPC,
                    program
            );

            statusBar.setText(
                    " Status: CPU Reset."
            );

            return;
        }

        if (message.startsWith("STEP|"))
        {
            processStepMessage(message);

            return;
        }

        if (message.startsWith("HALTED|"))
        {
            stopTimer();

            tracePanel.addTrace(
                    "CORE     : CPU Halted"
            );

            statusBar.setText(
                    " Status: Program halted."
            );

            return;
        }

        if (message.startsWith("ERROR|"))
        {
            stopTimer();

            String error =
                    message.substring(6);

            tracePanel.addTrace(
                    "ERROR    : " + error
            );

            statusBar.setText(
                    " Status: Error - " + error
            );

            return;
        }

        if (message.equals("CORE_SHUTDOWN"))
        {
            stopTimer();

            coreReady = false;

            statusBar.setText(
                    " Status: Core stopped."
            );
        }
    }
    // =========================================================
    // PROCESS STEP MESSAGE
    // =========================================================
    private void processStepMessage(
            String message
    )
    {
        String[] parts =
                message.split("\\|");

        int oldPC = currentPC;

        int newPC = currentPC;

        String instructionText =
                "UNKNOWN";

        String aValue = "0";

        String r1Value = "0";

        String cyValue = "0";

        String ovValue = "0";

        String spValue = "0";

        String queueValue = "0";

        String runningValue = "1";

        for (String part : parts)
        {
            if (part.startsWith("OLD_PC="))
            {
                oldPC =
                        parseInt(
                                part.substring(7)
                        );
            }
            else if (part.startsWith("PC="))
            {
                newPC =
                        parseInt(
                                part.substring(3)
                        );
            }
            else if (part.startsWith("A="))
            {
                aValue =
                        part.substring(2);
            }
            else if (part.startsWith("R1="))
            {
                r1Value =
                        part.substring(3);
            }
            else if (part.startsWith("CY="))
            {
                cyValue =
                        part.substring(3);
            }
            else if (part.startsWith("OV="))
            {
                ovValue =
                        part.substring(3);
            }
            else if (part.startsWith("SP="))
            {
                spValue =
                        part.substring(3);
            }
            else if (part.startsWith("QUEUE="))
            {
                queueValue =
                        part.substring(6);
            }
            else if (part.startsWith("RUNNING="))
            {
                runningValue =
                        part.substring(8);
            }
            else if (part.startsWith("INSTRUCTION="))
            {
                instructionText =
                        part.substring(12);
            }
        }

        currentPC = newPC;

        Instruction instruction = null;

        if (
                oldPC >= 0 &&
                oldPC < program.size()
        )
        {
            instruction =
                    program.get(oldPC);
        }

        if (instruction != null)
        {
            programPanel.showCurrentInstruction(
                    oldPC,
                    instruction
            );
        }

        tracePanel.addTrace(
                "FETCH   ✓ : PC = "
                        + String.format(
                                "%04XH",
                                oldPC
                        )
        );

        tracePanel.addTrace(
                "DECODE  ✓ : "
                        + instructionText
        );

        tracePanel.addTrace(
                "EXECUTE ✓ : "
                        + instructionText
        );

        tracePanel.addTrace(
                "PC      : "
                        + String.format(
                                "%04XH",
                                newPC
                        )
        );

        tracePanel.addTrace(
                "A       : "
                        + aValue
        );

        tracePanel.addTrace(
                "R1      : "
                        + r1Value
        );

        tracePanel.addTrace(
                "CY      : "
                        + cyValue
        );

        tracePanel.addTrace(
                "OV      : "
                        + ovValue
        );

        tracePanel.addTrace(
                "SP      : "
                        + spValue
        );

        tracePanel.addTrace(
                "Queue   : "
                        + queueValue
        );

        tracePanel.addTrace(
                "--------------------------------"
        );

        if (runningValue.equals("1"))
        {
            programPanel.showNextInstruction(
                    newPC,
                    program
            );

            statusBar.setText(
                    " Status: Core executed instruction."
            );
        }
        else
        {
            stopTimer();

            statusBar.setText(
                    " Status: Program halted."
            );
        }

        // Read the CPU state from POSIX shared memory.
        readSharedMemory();
    }

    // =========================================================
    // READ SHARED MEMORY
    // =========================================================

    private void readSharedMemory()
    {
        /*
         * The Windows/Java version no longer uses POSIX shared memory.
         * CPU state is delivered directly in the STEP message from Core.
         * This method is kept so the existing GUI flow remains simple.
         */
    }

    // =========================================================
    // SERIALIZE PROGRAM
    // =========================================================

    private String serializeProgram()
    {
        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < program.size();
             i++)
        {
            Instruction instruction =
                    program.get(i);

            if (i > 0)
            {
                result.append(";");
            }

            result.append(
                    instruction.mnemonic
            );

            if (
                    instruction.operands != null &&
                    !instruction.operands.isEmpty()
            )
            {
                result.append(":");

                result.append(
                        String.join(
                                ",",
                                instruction.operands
                        )
                );
            }
        }

        return result.toString();
    }
    // =========================================================
    // INTEGER PARSER
    // =========================================================
    private int parseInt(String value)
    {
        try
        {
            return Integer.parseInt(
                    value.trim()
            );
        }
        catch (NumberFormatException ex)
        {
            return 0;
        }
    }
    // CONNECTION CHECK

    private boolean isConnected()
    {
        return
                coreSocket != null &&
                coreSocket.isConnected() &&
                !coreSocket.isClosed() &&
                coreWriter != null &&
                coreReady;
    }
    // STOP TIMER
    private void stopTimer()
    {
        if (
                timer != null &&
                timer.isRunning()
        )
        {
            timer.stop();
        }
    }

    // CLEANUP
    private void cleanupIPC()
    {
        stopTimer();

        if (coreSocket != null)
        {
            try
            {
                coreSocket.close();
            }
            catch (IOException ignored)
            {
            }
        }
        coreSocket = null;
        coreReader = null;
        coreWriter = null;
        coreReady = false;
    }
    // MAIN
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(
                () -> new MainWindow()
        );
    }
}
package gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import cpu_core.Instruction;

public class MainWindow extends JFrame
{
    // =========================================================
    // POSIX IPC
    // =========================================================

    interface PosixIPC extends Library
    {
        PosixIPC INSTANCE = Native.load("c", PosixIPC.class);

        int mq_open(
                String name,
                int oflag,
                int mode,
                Pointer attr
        );

        int mq_send(
                int mqdes,
                byte[] message,
                NativeLong length,
                int priority
        );

        int mq_receive(
                int mqdes,
                byte[] message,
                NativeLong length,
                Pointer priority
        );

        int mq_close(int mqdes);

        int shm_open(
                String name,
                int oflag,
                int mode
        );

        Pointer mmap(
                Pointer addr,
                NativeLong length,
                int prot,
                int flags,
                int fd,
                NativeLong offset
        );

        int munmap(
                Pointer addr,
                NativeLong length
        );

        Pointer sem_open(
                String name,
                int oflag,
                int mode,
                int value
        );

        int sem_wait(Pointer semaphore);

        int sem_post(Pointer semaphore);

        int sem_close(Pointer semaphore);
    }

    // =========================================================
    // POSIX CONSTANTS
    // =========================================================

    private static final int O_CREAT = 0x40;
    private static final int O_RDWR = 0x2;
    private static final int O_NONBLOCK = 0x800;

    private static final int PROT_READ = 0x1;
    private static final int PROT_WRITE = 0x2;

    private static final int MAP_SHARED = 0x01;

    // =========================================================
    // IPC NAMES
    // =========================================================

    private static final String GUI_TO_CORE_QUEUE =
            "/microcontroller_gui_to_core";

    private static final String CORE_TO_GUI_QUEUE =
            "/microcontroller_core_to_gui";

    private static final String SHM_NAME =
            "/microcontroller_shared_memory";

    private static final String SEM_NAME =
            "/microcontroller_semaphore";

    private static final int SHARED_MEMORY_SIZE = 28;

    // =========================================================
    // IPC HANDLES
    // =========================================================

    private int guiToCoreQueue = -1;

    private int coreToGuiQueue = -1;

    private int sharedMemoryFd = -1;

    private Pointer sharedMemory = Pointer.NULL;

    private Pointer semaphore = Pointer.NULL;

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

    // =========================================================
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
        guiToCoreQueue =
                PosixIPC.INSTANCE.mq_open(
                        GUI_TO_CORE_QUEUE,
                        O_RDWR,
                        0666,
                        Pointer.NULL
                );

        if (guiToCoreQueue == -1)
        {
            statusBar.setText(
                    " Status: Core queue not available."
            );

            return;
        }

        coreToGuiQueue =
                PosixIPC.INSTANCE.mq_open(
                        CORE_TO_GUI_QUEUE,
                        O_RDWR | O_NONBLOCK,
                        0666,
                        Pointer.NULL
                );

        if (coreToGuiQueue == -1)
        {
            statusBar.setText(
                    " Status: Core response queue not available."
            );

            return;
        }

        sharedMemoryFd =
                PosixIPC.INSTANCE.shm_open(
                        SHM_NAME,
                        O_RDWR,
                        0666
                );

        if (sharedMemoryFd == -1)
        {
            statusBar.setText(
                    " Status: Shared memory not available."
            );

            return;
        }

        sharedMemory =
                PosixIPC.INSTANCE.mmap(
                        Pointer.NULL,
                        new NativeLong(
                                SHARED_MEMORY_SIZE
                        ),
                        PROT_READ | PROT_WRITE,
                        MAP_SHARED,
                        sharedMemoryFd,
                        new NativeLong(0)
                );

        if (
                Pointer.nativeValue(sharedMemory)
                        == -1L
        )
        {
            statusBar.setText(
                    " Status: Shared memory mapping failed."
            );

            return;
        }

        semaphore =
                PosixIPC.INSTANCE.sem_open(
                        SEM_NAME,
                        0,
                        0666,
                        1
                );

        if (
                Pointer.nativeValue(semaphore)
                        == -1L
        )
        {
            statusBar.setText(
                    " Status: Semaphore not available."
            );

            return;
        }

        sendCommand("PING");

        statusBar.setText(
                " Status: Connected to Core."
        );
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

    // =========================================================
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
        if (guiToCoreQueue == -1)
            return;

        byte[] message =
                command.getBytes(
                        StandardCharsets.UTF_8
                );

        int result =
                PosixIPC.INSTANCE.mq_send(
                        guiToCoreQueue,
                        message,
                        new NativeLong(
                                message.length
                        ),
                        0
                );

        if (result != 0)
        {
            statusBar.setText(
                    " Status: Failed to send command."
            );
        }
    }

    // =========================================================
    // RECEIVE CORE UPDATES
    // =========================================================

    private void startReceiverTimer()
    {
        Timer receiverTimer =
                new Timer(
                        100,
                        e -> receiveCoreMessages()
                );

        receiverTimer.start();
    }

    private void receiveCoreMessages()
    {
        if (coreToGuiQueue == -1)
            return;

        byte[] received =
                new byte[8192];

        int result;

        do
        {
            result =
                    PosixIPC.INSTANCE.mq_receive(
                            coreToGuiQueue,
                            received,
                            new NativeLong(
                                    received.length
                            ),
                            Pointer.NULL
                    );

            if (result > 0)
            {
                String message =
                        new String(
                                received,
                                0,
                                result,
                                StandardCharsets.UTF_8
                        );

                processCoreMessage(message);
            }

        }
        while (result > 0);
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
        if (
                sharedMemory == Pointer.NULL ||
                semaphore == Pointer.NULL
        )
        {
            return;
        }

        if (
                Pointer.nativeValue(semaphore)
                        == -1L
        )
        {
            return;
        }

        int result =
                PosixIPC.INSTANCE.sem_wait(
                        semaphore
                );

        if (result != 0)
            return;

        try
        {
            int a =
                    sharedMemory.getInt(0);

            int pc =
                    sharedMemory.getInt(4);

            int sp =
                    sharedMemory.getInt(8);

            int cy =
                    sharedMemory.getInt(12);

            int ov =
                    sharedMemory.getInt(16);

            int r1 =
                    sharedMemory.getInt(20);

            int queue =
                    sharedMemory.getInt(24);

            System.out.println(
                    "Shared Memory -> "
                            + "A=" + a
                            + ", PC=" + pc
                            + ", SP=" + sp
                            + ", CY=" + cy
                            + ", OV=" + ov
                            + ", R1=" + r1
                            + ", QUEUE=" + queue
            );
        }
        finally
        {
            PosixIPC.INSTANCE.sem_post(
                    semaphore
            );
        }
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
                guiToCoreQueue != -1 &&
                coreToGuiQueue != -1 &&
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

        if (guiToCoreQueue != -1)
        {
            PosixIPC.INSTANCE.mq_close(
                    guiToCoreQueue
            );

            guiToCoreQueue = -1;
        }
        if (coreToGuiQueue != -1)
        {
            PosixIPC.INSTANCE.mq_close(
                    coreToGuiQueue
            );

            coreToGuiQueue = -1;
        }
        if (semaphore != Pointer.NULL)
        {
            PosixIPC.INSTANCE.sem_close(
                    semaphore
            );

            semaphore = Pointer.NULL;
        }
        if (sharedMemory != Pointer.NULL)
        {
            PosixIPC.INSTANCE.munmap(
                    sharedMemory,
                    new NativeLong(
                            SHARED_MEMORY_SIZE
                    )
            );

            sharedMemory = Pointer.NULL;
        }
    }
    // MAIN
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(
                () -> new MainWindow()
        );
    }
}
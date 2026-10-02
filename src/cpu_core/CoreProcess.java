package cpu_core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

public class CoreProcess
{
    // =========================================================
    // Linux POSIX API through JNA
    // =========================================================

    interface PosixIPC extends Library
    {
        PosixIPC INSTANCE = Native.load("c", PosixIPC.class);

        // ---------- POSIX Message Queue ----------

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

        int mq_unlink(String name);

        // ---------- POSIX Shared Memory ----------

        int shm_open(
                String name,
                int oflag,
                int mode
        );

        int ftruncate(
                int fd,
                NativeLong length
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

        int shm_unlink(String name);

        // ---------- POSIX Semaphore ----------

        Pointer sem_open(
                String name,
                int oflag,
                int mode,
                int value
        );

        int sem_wait(Pointer semaphore);

        int sem_post(Pointer semaphore);

        int sem_close(Pointer semaphore);

        int sem_unlink(String name);
    }

    // =========================================================
    // POSIX constants
    // =========================================================

    private static final int O_CREAT = 0x40;
    private static final int O_RDWR = 0x2;

    private static final int PROT_READ = 0x1;
    private static final int PROT_WRITE = 0x2;

    private static final int MAP_SHARED = 0x01;

    // =========================================================
    // POSIX IPC names
    // =========================================================

    // GUI -> Core
    private static final String GUI_TO_CORE_QUEUE =
            "/microcontroller_gui_to_core";

    // Core -> GUI
    private static final String CORE_TO_GUI_QUEUE =
            "/microcontroller_core_to_gui";

    // Core -> Logger
    private static final String CORE_TO_LOGGER_QUEUE =
            "/microcontroller_core_logger";

    // Shared CPU state
    private static final String SHM_NAME =
            "/microcontroller_shared_memory";

    // Protects shared CPU state
    private static final String SEM_NAME =
            "/microcontroller_semaphore";

    // =========================================================
    // Shared memory layout
    // =========================================================

    // 0  = A
    // 4  = PC
    // 8  = SP
    // 12 = CY
    // 16 = OV
    // 20 = R1
    // 24 = Queue count

    private static final int SHARED_MEMORY_SIZE = 28;

    // =========================================================
    // Main Core Process
    // =========================================================

    public static void main(String[] args)
    {
        System.out.println("=================================");
        System.out.println("       CORE PROCESS STARTED");
        System.out.println("=================================");

        CPU cpu = new CPU();

        System.out.println("CPU created successfully");

        int guiToCoreQueue = -1;
        int coreToGuiQueue = -1;
        int loggerQueue = -1;

        Pointer sharedMemory = Pointer.NULL;
        Pointer semaphore = Pointer.NULL;

        try
        {
            // -------------------------------------------------
            // Remove old IPC objects from previous crashed run
            // -------------------------------------------------

            PosixIPC.INSTANCE.mq_unlink(
                    GUI_TO_CORE_QUEUE
            );

            PosixIPC.INSTANCE.mq_unlink(
                    CORE_TO_GUI_QUEUE
            );

            PosixIPC.INSTANCE.mq_unlink(
                    CORE_TO_LOGGER_QUEUE
            );

            PosixIPC.INSTANCE.shm_unlink(
                    SHM_NAME
            );

            PosixIPC.INSTANCE.sem_unlink(
                    SEM_NAME
            );

            // -------------------------------------------------
            // GUI -> CORE message queue
            // -------------------------------------------------

            guiToCoreQueue =
                    PosixIPC.INSTANCE.mq_open(
                            GUI_TO_CORE_QUEUE,
                            O_CREAT | O_RDWR,
                            0666,
                            Pointer.NULL
                    );

            if (guiToCoreQueue == -1)
            {
                throw new RuntimeException(
                        "Failed to create GUI -> Core queue"
                );
            }

            // -------------------------------------------------
            // CORE -> GUI message queue
            // -------------------------------------------------

            coreToGuiQueue =
                    PosixIPC.INSTANCE.mq_open(
                            CORE_TO_GUI_QUEUE,
                            O_CREAT | O_RDWR,
                            0666,
                            Pointer.NULL
                    );

            if (coreToGuiQueue == -1)
            {
                throw new RuntimeException(
                        "Failed to create Core -> GUI queue"
                );
            }

            // -------------------------------------------------
            // CORE -> LOGGER message queue
            // -------------------------------------------------

            loggerQueue =
                    PosixIPC.INSTANCE.mq_open(
                            CORE_TO_LOGGER_QUEUE,
                            O_CREAT | O_RDWR,
                            0666,
                            Pointer.NULL
                    );

            if (loggerQueue == -1)
            {
                throw new RuntimeException(
                        "Failed to create Core -> Logger queue"
                );
            }

            System.out.println(
                    "POSIX Message Queues created"
            );

            // -------------------------------------------------
            // POSIX SHARED MEMORY
            // -------------------------------------------------

            int shmFd =
                    PosixIPC.INSTANCE.shm_open(
                            SHM_NAME,
                            O_CREAT | O_RDWR,
                            0666
                    );

            if (shmFd == -1)
            {
                throw new RuntimeException(
                        "Failed to create shared memory"
                );
            }

            int resizeResult =
                    PosixIPC.INSTANCE.ftruncate(
                            shmFd,
                            new NativeLong(
                                    SHARED_MEMORY_SIZE
                            )
                    );

            if (resizeResult != 0)
            {
                throw new RuntimeException(
                        "Failed to resize shared memory"
                );
            }

            sharedMemory =
                    PosixIPC.INSTANCE.mmap(
                            Pointer.NULL,
                            new NativeLong(
                                    SHARED_MEMORY_SIZE
                            ),
                            PROT_READ | PROT_WRITE,
                            MAP_SHARED,
                            shmFd,
                            new NativeLong(0)
                    );

            if (Pointer.nativeValue(sharedMemory) == -1L)
            {
                throw new RuntimeException(
                        "Failed to map shared memory"
                );
            }

            System.out.println(
                    "POSIX Shared Memory created"
            );

            // -------------------------------------------------
            // POSIX SEMAPHORE
            // -------------------------------------------------

            semaphore =
                    PosixIPC.INSTANCE.sem_open(
                            SEM_NAME,
                            O_CREAT,
                            0666,
                            1
                    );

            if (Pointer.nativeValue(semaphore) == -1L)
            {
                throw new RuntimeException(
                        "Failed to create semaphore"
                );
            }

            System.out.println(
                    "POSIX Semaphore created"
            );

            // -------------------------------------------------
            // Initial CPU state
            // -------------------------------------------------

            writeSharedState(
                    cpu,
                    sharedMemory,
                    semaphore
            );

            sendUpdate(
                    coreToGuiQueue,
                    loggerQueue,
                    "CORE_READY"
            );

            System.out.println();
            System.out.println(
                    "Core is READY and waiting for GUI commands..."
            );

            // =================================================
            // MAIN COMMAND LOOP
            // =================================================

            boolean running = true;

            while (running)
            {
                byte[] received =
                        new byte[8192];

                int receiveResult =
                        PosixIPC.INSTANCE.mq_receive(
                                guiToCoreQueue,
                                received,
                                new NativeLong(
                                        received.length
                                ),
                                Pointer.NULL
                        );

                if (receiveResult < 0)
                {
                    continue;
                }

                String command =
                        new String(
                                received,
                                0,
                                receiveResult,
                                StandardCharsets.UTF_8
                        );

                System.out.println(
                        "GUI command: " + command
                );

                // ---------------------------------------------
                // LOAD
                // ---------------------------------------------

                if (command.startsWith("LOAD|"))
                {
                    String programData =
                            command.substring(5);

                    ArrayList<Instruction> program =
                            parseProgram(programData);

                    cpu.loadProgram(program);

                    writeSharedState(
                            cpu,
                            sharedMemory,
                            semaphore
                    );

                    sendUpdate(
                            coreToGuiQueue,
                            loggerQueue,
                            "LOADED|PC=" + cpu.getPC()
                    );

                    System.out.println(
                            "Program loaded from GUI"
                    );
                }

                // ---------------------------------------------
                // STEP
                // ---------------------------------------------

                else if (command.equals("STEP"))
                {
                    if (!cpu.isRunning())
                    {
                        sendUpdate(
                                coreToGuiQueue,
                                loggerQueue,
                                "HALTED|PC=" + cpu.getPC()
                        );

                        continue;
                    }

                    int oldPC =
                            cpu.getPC();

                    Instruction instruction;

                    try
                    {
                        instruction =
                                cpu.step();
                    }
                    catch (RuntimeException ex)
                    {
                        sendUpdate(
                                coreToGuiQueue,
                                loggerQueue,
                                "ERROR|" + ex.getMessage()
                        );

                        continue;
                    }

                    writeSharedState(
                            cpu,
                            sharedMemory,
                            semaphore
                    );

                    String instructionText =
                            instruction == null
                                    ? "NONE"
                                    : instruction.toString();

                    String update =
                            "STEP"
                            + "|OLD_PC=" + oldPC
                            + "|PC=" + cpu.getPC()
                            + "|A=" + cpu.getA()
                            + "|R1=" + cpu.getR(1)
                            + "|CY=" + (cpu.isCY() ? 1 : 0)
                            + "|OV=" + (cpu.isOV() ? 1 : 0)
                            + "|SP=" + cpu.getSP()
                            + "|QUEUE="
                            + cpu.getQueue().getCount()
                            + "|RUNNING="
                            + (cpu.isRunning() ? 1 : 0)
                            + "|INSTRUCTION="
                            + instructionText;

                    sendUpdate(
                            coreToGuiQueue,
                            loggerQueue,
                            update
                    );

                    System.out.println(
                            "CPU step completed"
                    );
                }

                // ---------------------------------------------
                // RESET
                // ---------------------------------------------

                else if (command.equals("RESET"))
                {
                    cpu.reset();

                    writeSharedState(
                            cpu,
                            sharedMemory,
                            semaphore
                    );

                    sendUpdate(
                            coreToGuiQueue,
                            loggerQueue,
                            "RESET|PC=" + cpu.getPC()
                    );

                    System.out.println(
                            "CPU reset"
                    );
                }

                // ---------------------------------------------
                // RUN
                //
                // GUI can repeatedly send STEP commands.
                // The actual CPU execution remains in Core.
                // ---------------------------------------------

                else if (command.equals("PING"))
                {
                    sendUpdate(
                            coreToGuiQueue,
                            loggerQueue,
                            "PONG"
                    );
                }

                // ---------------------------------------------
                // SHUTDOWN
                // ---------------------------------------------

                else if (command.equals("SHUTDOWN"))
                {
                    sendUpdate(
                            coreToGuiQueue,
                            loggerQueue,
                            "CORE_SHUTDOWN"
                    );

                    running = false;
                }

                // ---------------------------------------------
                // Unknown command
                // ---------------------------------------------

                else
                {
                    sendUpdate(
                            coreToGuiQueue,
                            loggerQueue,
                            "ERROR|Unknown command: "
                            + command
                    );
                }
            }
        }
        finally
        {
            // =================================================
            // CLEANUP
            // =================================================

            System.out.println();
            System.out.println(
                    "Cleaning up Core IPC..."
            );

            if (semaphore != Pointer.NULL)
            {
                PosixIPC.INSTANCE.sem_close(
                        semaphore
                );

                PosixIPC.INSTANCE.sem_unlink(
                        SEM_NAME
                );
            }

            if (sharedMemory != Pointer.NULL)
            {
                PosixIPC.INSTANCE.munmap(
                        sharedMemory,
                        new NativeLong(
                                SHARED_MEMORY_SIZE
                        )
                );

                PosixIPC.INSTANCE.shm_unlink(
                        SHM_NAME
                );
            }

            if (guiToCoreQueue != -1)
            {
                PosixIPC.INSTANCE.mq_close(
                        guiToCoreQueue
                );

                PosixIPC.INSTANCE.mq_unlink(
                        GUI_TO_CORE_QUEUE
                );
            }

            if (coreToGuiQueue != -1)
            {
                PosixIPC.INSTANCE.mq_close(
                        coreToGuiQueue
                );

                PosixIPC.INSTANCE.mq_unlink(
                        CORE_TO_GUI_QUEUE
                );
            }

            if (loggerQueue != -1)
            {
                PosixIPC.INSTANCE.mq_close(
                        loggerQueue
                );

                PosixIPC.INSTANCE.mq_unlink(
                        CORE_TO_LOGGER_QUEUE
                );
            }

            System.out.println(
                    "Core Process Stopped"
            );
        }
    }

    // =========================================================
    // Write CPU state into POSIX shared memory
    // =========================================================

    private static void writeSharedState(
            CPU cpu,
            Pointer sharedMemory,
            Pointer semaphore
    )
    {
        PosixIPC.INSTANCE.sem_wait(
                semaphore
        );

        try
        {
            sharedMemory.setInt(
                    0,
                    cpu.getA()
            );

            sharedMemory.setInt(
                    4,
                    cpu.getPC()
            );

            sharedMemory.setInt(
                    8,
                    cpu.getSP()
            );

            sharedMemory.setInt(
                    12,
                    cpu.isCY() ? 1 : 0
            );

            sharedMemory.setInt(
                    16,
                    cpu.isOV() ? 1 : 0
            );

            sharedMemory.setInt(
                    20,
                    cpu.getR(1)
            );

            sharedMemory.setInt(
                    24,
                    cpu.getQueue().getCount()
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
    // Send Core -> GUI and Core -> Logger event
    // =========================================================

    private static void sendUpdate(
            int coreToGuiQueue,
            int loggerQueue,
            String messageText
    )
    {
        byte[] message =
                messageText.getBytes(
                        StandardCharsets.UTF_8
                );

        PosixIPC.INSTANCE.mq_send(
                coreToGuiQueue,
                message,
                new NativeLong(
                        message.length
                ),
                0
        );

        PosixIPC.INSTANCE.mq_send(
                loggerQueue,
                message,
                new NativeLong(
                        message.length
                ),
                0
        );
    }

    // =========================================================
    // Convert GUI program text into Instruction objects
    // =========================================================

    private static ArrayList<Instruction> parseProgram(
            String programData
    )
    {
        ArrayList<Instruction> program =
                new ArrayList<>();

        if (programData == null ||
                programData.isEmpty())
        {
            return program;
        }

        String[] instructions =
                programData.split(";");

        for (String instructionText : instructions)
        {
            if (instructionText.trim().isEmpty())
            {
                continue;
            }

            String[] parts =
                    instructionText.split(
                            ":",
                            2
                    );

            String mnemonic =
                    parts[0];

            ArrayList<String> operands =
                    new ArrayList<>();

            if (parts.length > 1 &&
                    !parts[1].isEmpty())
            {
                operands.addAll(
                        Arrays.asList(
                                parts[1].split(",")
                        )
                );
            }

            program.add(
                    new Instruction(
                            mnemonic,
                            operands
                    )
            );
        }

        return program;
    }
}
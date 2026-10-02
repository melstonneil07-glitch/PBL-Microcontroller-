import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/**
 * IPCClient
 *
 * GUI <-> CORE communication using POSIX Named Pipes (FIFO).
 *
 * GUI -> Core:
 *      /tmp/ms51_ipc/ui_to_core.fifo
 *
 * Core -> GUI:
 *      /tmp/ms51_ipc/core_to_ui.fifo
 *
 * The Core internally uses POSIX Message Queue.
 * The Core -> Logging connection uses POSIX Socket.
 */
public class IPCClient {

    private static final String IPC_DIRECTORY =
            "/tmp/ms51_ipc";

    private static final String UI_TO_CORE =
            IPC_DIRECTORY + "/ui_to_core.fifo";

    private static final String CORE_TO_UI =
            IPC_DIRECTORY + "/core_to_ui.fifo";

    private BufferedWriter writer;
    private BufferedReader reader;

    private Thread readerThread;

    private volatile boolean connected = false;

    private MessageListener listener;


    /* =====================================================
       MESSAGE LISTENER
       ===================================================== */

    public interface MessageListener {
        void onMessage(String message);
        void onConnectionChanged(boolean connected);
    }


    /* =====================================================
       CONSTRUCTOR
       ===================================================== */

    public IPCClient(MessageListener listener) {
        this.listener = listener;
    }


    /* =====================================================
       CONNECT TO CORE
       ===================================================== */

    public boolean connect() {

        try {

            Path directory = Paths.get(IPC_DIRECTORY);

            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            /*
             * FIFO files should normally be created by
             * the Core process using mkfifo.
             *
             * We do NOT create normal files here because
             * they are not POSIX FIFOs.
             */

            if (!Files.exists(Paths.get(UI_TO_CORE))) {

                System.out.println(
                        "[IPC] UI -> Core FIFO not found."
                );

                return false;
            }

            if (!Files.exists(Paths.get(CORE_TO_UI))) {

                System.out.println(
                        "[IPC] Core -> UI FIFO not found."
                );

                return false;
            }


            /*
             * Open GUI -> Core pipe.
             */

            writer = Files.newBufferedWriter(
                    Paths.get(UI_TO_CORE),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.WRITE
            );


            /*
             * Open Core -> GUI pipe.
             */

            reader = Files.newBufferedReader(
                    Paths.get(CORE_TO_UI),
                    StandardCharsets.UTF_8
            );


            connected = true;

            if (listener != null) {
                listener.onConnectionChanged(true);
            }


            /*
             * Start receiver thread.
             */

            startReaderThread();


            System.out.println(
                    "[IPC] Connected to Core."
            );

            return true;

        } catch (Exception e) {

            System.out.println(
                    "[IPC] Connection failed: "
                            + e.getMessage()
            );

            connected = false;

            if (listener != null) {
                listener.onConnectionChanged(false);
            }

            return false;
        }
    }


    /* =====================================================
       START READER THREAD
       ===================================================== */

    private void startReaderThread() {

        readerThread = new Thread(() -> {

            try {

                String message;

                while (connected &&
                        (message = reader.readLine()) != null) {

                    final String received = message;

                    if (listener != null) {

                        javax.swing.SwingUtilities.invokeLater(
                                () -> listener.onMessage(received)
                        );
                    }
                }

            } catch (IOException e) {

                if (connected) {

                    System.out.println(
                            "[IPC] Core connection closed."
                    );

                    connected = false;

                    if (listener != null) {

                        javax.swing.SwingUtilities.invokeLater(
                                () -> listener.onConnectionChanged(false)
                        );
                    }
                }
            }

        }, "Core-Receiver");

        readerThread.setDaemon(true);
        readerThread.start();
    }


    /* =====================================================
       SEND COMMAND
       ===================================================== */

    public synchronized void sendCommand(String command) {

        if (!connected || writer == null) {

            System.out.println(
                    "[IPC] Core is not connected."
            );

            return;
        }

        try {

            writer.write(command);
            writer.newLine();
            writer.flush();

            System.out.println(
                    "[IPC] Sent: " + command
            );

        } catch (IOException e) {

            System.out.println(
                    "[IPC] Send failed: "
                            + e.getMessage()
            );
        }
    }


    /* =====================================================
       COMMON CORE COMMANDS
       ===================================================== */

    public void runCPU() {
        sendCommand("RUN");
    }

    public void stepCPU() {
        sendCommand("STEP");
    }

    public void resetCPU() {
        sendCommand("RESET");
    }

    public void stopCPU() {
        sendCommand("STOP");
    }

    public void loadProgram() {
        sendCommand("LOAD_PROGRAM");
    }

    public void startProcess() {
        sendCommand("START_PROCESS");
    }

    public void setPriority(int priority) {
        sendCommand("SET_PRIORITY " + priority);
    }


    /* =====================================================
       SEND CUSTOM COMMAND
       ===================================================== */

    public void sendCustomCommand(String command) {

        if (command == null ||
                command.trim().isEmpty()) {
            return;
        }

        sendCommand(command.trim());
    }


    /* =====================================================
       DISCONNECT
       ===================================================== */

    public void disconnect() {

        connected = false;

        try {

            if (writer != null) {
                writer.close();
            }

            if (reader != null) {
                reader.close();
            }

        } catch (IOException ignored) {
        }

        writer = null;
        reader = null;

        if (listener != null) {
            listener.onConnectionChanged(false);
        }

        System.out.println(
                "[IPC] Disconnected from Core."
        );
    }


    /* =====================================================
       CONNECTION STATUS
       ===================================================== */

    public boolean isConnected() {
        return connected;
    }
}

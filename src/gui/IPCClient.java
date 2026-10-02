import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;

import java.nio.charset.StandardCharsets;

public class IPCClient {

    // =========================================================
    // MESSAGE LISTENER
    // =========================================================

    public interface MessageListener {
        void onMessage(String message);
    }

    // =========================================================
    // POSIX LIBRARY
    // =========================================================

    interface PosixIPC extends Library {

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

        int mq_unlink(String name);
    }

    // =========================================================
    // POSIX CONSTANTS
    // =========================================================

    private static final int O_RDWR = 0x0002;

    // =========================================================
    // MESSAGE QUEUE NAMES
    // =========================================================

    private static final String GUI_TO_CORE_QUEUE =
            "/microcontroller_gui_to_core";

    private static final String CORE_TO_GUI_QUEUE =
            "/microcontroller_core_to_gui";

    // =========================================================
    // QUEUE DESCRIPTORS
    // =========================================================

    private int guiToCoreQueue = -1;

    private int coreToGuiQueue = -1;

    // =========================================================
    // STATE
    // =========================================================

    private volatile boolean connected = false;

    private volatile boolean listening = false;

    private Thread receiverThread;

    private final MessageListener listener;

    // =========================================================
    // CONSTRUCTOR
    // =========================================================

    public IPCClient(MessageListener listener) {

        this.listener = listener;
    }

    // =========================================================
    // CONNECT
    // =========================================================

    public boolean connect() {

        if (connected) {
            return true;
        }

        try {

            /*
             * The Core creates the POSIX queues.
             * The GUI only opens them.
             */

            guiToCoreQueue =
                    PosixIPC.INSTANCE.mq_open(
                            GUI_TO_CORE_QUEUE,
                            O_RDWR,
                            0,
                            Pointer.NULL
                    );

            if (guiToCoreQueue == -1) {

                return false;
            }

            coreToGuiQueue =
                    PosixIPC.INSTANCE.mq_open(
                            CORE_TO_GUI_QUEUE,
                            O_RDWR,
                            0,
                            Pointer.NULL
                    );

            if (coreToGuiQueue == -1) {

                PosixIPC.INSTANCE.mq_close(
                        guiToCoreQueue
                );

                guiToCoreQueue = -1;

                return false;
            }

            connected = true;

            startReceiver();

            return true;

        } catch (Exception e) {

            close();

            return false;
        }
    }

    // =========================================================
    // CONNECT WITH RETRY
    // =========================================================

    public boolean connectWithRetry(
            int attempts,
            int delayMillis
    ) {

        for (int i = 0; i < attempts; i++) {

            if (connect()) {
                return true;
            }

            try {

                Thread.sleep(delayMillis);

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();

                return false;
            }
        }

        return false;
    }

    // =========================================================
    // START RECEIVER
    // =========================================================

    private void startReceiver() {

        if (listening) {
            return;
        }

        listening = true;

        receiverThread =
                new Thread(
                        this::receiveLoop,
                        "Core-To-GUI-Receiver"
                );

        receiverThread.setDaemon(true);

        receiverThread.start();
    }

    // =========================================================
    // RECEIVE LOOP
    // =========================================================

    private void receiveLoop() {

        byte[] buffer =
                new byte[8192];

        while (
                listening &&
                connected
        ) {

            try {

                int received =
                        PosixIPC.INSTANCE.mq_receive(
                                coreToGuiQueue,
                                buffer,
                                new NativeLong(
                                        buffer.length
                                ),
                                Pointer.NULL
                        );

                if (received < 0) {

                    continue;
                }

                String message =
                        new String(
                                buffer,
                                0,
                                received,
                                StandardCharsets.UTF_8
                        );

                if (listener != null) {

                    listener.onMessage(
                            message
                    );
                }

            } catch (Exception e) {

                if (
                        connected &&
                        listener != null
                ) {

                    listener.onMessage(
                            "ERROR|IPC receive error: "
                                    + e.getMessage()
                    );
                }

                break;
            }
        }
    }

    // =========================================================
    // SEND MESSAGE
    // =========================================================

    private boolean send(
            String message
    ) {

        if (
                !connected ||
                guiToCoreQueue == -1
        ) {

            return false;
        }

        try {

            byte[] data =
                    message.getBytes(
                            StandardCharsets.UTF_8
                    );

            int result =
                    PosixIPC.INSTANCE.mq_send(
                            guiToCoreQueue,
                            data,
                            new NativeLong(
                                    data.length
                            ),
                            0
                    );

            return result == 0;

        } catch (Exception e) {

            if (listener != null) {

                listener.onMessage(
                        "ERROR|IPC send error: "
                                + e.getMessage()
                );
            }

            return false;
        }
    }

    // =========================================================
    // LOAD PROGRAM
    // =========================================================

    public boolean loadProgram(
            String program
    ) {

        return send(
                "LOAD|" + program
        );
    }

    // =========================================================
    // STEP
    // =========================================================

    public boolean stepCPU() {

        return send("STEP");
    }

    // =========================================================
    // RESET
    // =========================================================

    public boolean resetCPU() {

        return send("RESET");
    }

    // =========================================================
    // PING
    // =========================================================

    public boolean ping() {

        return send("PING");
    }

    // =========================================================
    // SHUTDOWN
    // =========================================================

    public boolean shutdownCore() {

        return send("SHUTDOWN");
    }

    // =========================================================
    // CONNECTION STATUS
    // =========================================================

    public boolean isConnected() {

        return connected;
    }

    // =========================================================
    // CLOSE
    // =========================================================

    public void close() {

        listening = false;

        connected = false;

        if (guiToCoreQueue != -1) {

            try {

                PosixIPC.INSTANCE.mq_close(
                        guiToCoreQueue
                );

            } catch (Exception ignored) {
            }

            guiToCoreQueue = -1;
        }

        if (coreToGuiQueue != -1) {

            try {

                PosixIPC.INSTANCE.mq_close(
                        coreToGuiQueue
                );

            } catch (Exception ignored) {
            }

            coreToGuiQueue = -1;
        }
    }

    // =========================================================
    // DISCONNECT
    // =========================================================

    public void disconnect() {

        close();
    }
}

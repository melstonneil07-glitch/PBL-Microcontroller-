package gui;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class TCPClient {

    public interface MessageListener {
        void onCoreMessage(String message);
        void onConnectionChanged(boolean connected);
    }

    private final String host;
    private final int corePort;
    private final int loggerPort;

    private Socket coreSocket;
    private BufferedReader coreReader;
    private BufferedWriter coreWriter;

    private Socket loggerSocket;
    private BufferedWriter loggerWriter;

    private volatile boolean coreConnected = false;
    private volatile boolean loggerConnected = false;

    private MessageListener listener;

    public TCPClient(String host, int corePort, int loggerPort) {
        this.host = host;
        this.corePort = corePort;
        this.loggerPort = loggerPort;
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public boolean connectCore() {
        try {
            coreSocket = new Socket();
            coreSocket.connect(
                    new InetSocketAddress(host, corePort),
                    3000
            );

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

            coreConnected = true;

            if (listener != null) {
                listener.onConnectionChanged(true);
            }

            startCoreReceiver();

            return true;

        } catch (IOException e) {
            coreConnected = false;

            if (listener != null) {
                listener.onConnectionChanged(false);
            }

            return false;
        }
    }

    public boolean connectLogger() {
        try {
            loggerSocket = new Socket();
            loggerSocket.connect(
                    new InetSocketAddress(host, loggerPort),
                    3000
            );

            loggerWriter = new BufferedWriter(
                    new OutputStreamWriter(
                            loggerSocket.getOutputStream(),
                            StandardCharsets.UTF_8
                    )
            );

            loggerConnected = true;

            return true;

        } catch (IOException e) {
            loggerConnected = false;
            return false;
        }
    }

    private void startCoreReceiver() {

        Thread receiverThread = new Thread(() -> {

            try {
                String message;

                while (coreConnected &&
                        (message = coreReader.readLine()) != null) {

                    if (listener != null) {
                        listener.onCoreMessage(message);
                    }
                }

            } catch (IOException e) {

                if (coreConnected) {
                    coreConnected = false;

                    if (listener != null) {
                        listener.onConnectionChanged(false);
                    }
                }
            }

        }, "Core-TCP-Receiver");

        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public synchronized void sendToCore(String message) {

        if (!coreConnected || coreWriter == null) {
            return;
        }

        try {
            coreWriter.write(message);
            coreWriter.newLine();
            coreWriter.flush();

        } catch (IOException e) {
            coreConnected = false;

            if (listener != null) {
                listener.onConnectionChanged(false);
            }
        }
    }

    public synchronized void sendLog(String message) {

        if (!loggerConnected || loggerWriter == null) {
            return;
        }

        try {
            loggerWriter.write(message);
            loggerWriter.newLine();
            loggerWriter.flush();

        } catch (IOException e) {
            loggerConnected = false;
        }
    }

    public boolean isCoreConnected() {
        return coreConnected;
    }

    public boolean isLoggerConnected() {
        return loggerConnected;
    }

    public void closeCore() {

        coreConnected = false;

        try {
            if (coreSocket != null) {
                coreSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public void closeLogger() {

        loggerConnected = false;

        try {
            if (loggerSocket != null) {
                loggerSocket.close();
            }
        } catch (IOException ignored) {
        }
    }

    public void close() {
        closeCore();
        closeLogger();
    }
}

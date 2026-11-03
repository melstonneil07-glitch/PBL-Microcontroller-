package gui;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import logging.LogClient;

/**
 * UI-side IPC endpoints: a TCP connection to the Core process (commands out, replies in on a
 * dedicated receiver thread) and a LogClient connection to the Logging process.
 * Both connect attempts are single-shot; callers retry (see MainWindow).
 */
public class TCPClient {

    public interface MessageListener {
        void onCoreMessage(String message);
        void onConnectionChanged(boolean connected);
    }

    private final String host;
    private final int corePort;
    private final int loggerPort;

    private Socket coreSocket;
    private BufferedWriter coreWriter;
    private LogClient logClient;

    private volatile boolean coreConnected = false;
    private volatile boolean loggerConnected = false;

    private MessageListener listener;
    private volatile String lastError = "";

    public TCPClient(String host, int corePort, int loggerPort) {
        this.host = host;
        this.corePort = corePort;
        this.loggerPort = loggerPort;
    }

    public void setMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    /** One connection attempt to the Core. Returns false (silently) if the Core is not reachable. */
    public synchronized boolean connectCore() {
        closeCore();
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, corePort), 3000);
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            coreWriter = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            coreSocket = socket;
            coreConnected = true;
            if (listener != null) listener.onConnectionChanged(true);
            startCoreReceiver(socket, reader);
            return true;
        } catch (IOException e) {
            try { socket.close(); } catch (IOException ignored) { }
            coreConnected = false;
            lastError = e.getClass().getSimpleName() + ": " + e.getMessage();
            return false;
        }
    }

    /** One connection attempt to the Logging process. */
    public synchronized boolean connectLogger() {
        if (logClient != null) logClient.close();
        try {
            LogClient client = new LogClient("UI", host, loggerPort);
            client.connect();
            logClient = client;
            loggerConnected = true;
            return true;
        } catch (IOException e) {
            loggerConnected = false;
            return false;
        }
    }

    private void startCoreReceiver(Socket socket, BufferedReader reader) {
        Thread receiverThread = new Thread(() -> {
            try {
                String message;
                while ((message = reader.readLine()) != null) {
                    if (listener != null) listener.onCoreMessage(message);
                }
            } catch (IOException ignored) {
                // fall through: treated the same as the Core closing the connection
            }
            // Only report the loss if this is still the active connection.
            synchronized (TCPClient.this) {
                if (coreSocket == socket && coreConnected) {
                    coreConnected = false;
                    if (listener != null) listener.onConnectionChanged(false);
                }
            }
        }, "Core-TCP-Receiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    public synchronized boolean sendToCore(String message) {
        if (!coreConnected || coreWriter == null) return false;
        try {
            coreWriter.write(message);
            coreWriter.newLine();
            coreWriter.flush();
            return true;
        } catch (IOException e) {
            coreConnected = false;
            if (listener != null) listener.onConnectionChanged(false);
            return false;
        }
    }

    public void sendLog(String message) {
        LogClient client = logClient;
        if (!loggerConnected || client == null) return;
        if (!client.send(logging.LogLevel.INFO, message)) loggerConnected = false;  // lets the caller reconnect
    }

    /** Reason the last connectCore() attempt failed (empty if none). */
    public String getLastError() { return lastError; }

    public boolean isCoreConnected() { return coreConnected; }

    public boolean isLoggerConnected() { return loggerConnected; }

    public synchronized void closeCore() {
        coreConnected = false;
        try {
            if (coreSocket != null) coreSocket.close();
        } catch (IOException ignored) {
        }
        coreSocket = null;
        coreWriter = null;
    }

    public void closeLogger() {
        loggerConnected = false;
        if (logClient != null) logClient.close();
    }

    public void close() {
        closeCore();
        closeLogger();
    }
}

package logging;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;

// Thin IPC client used by the UI process and the Core process to send
// log entries to the Logging process over a TCP socket. Each process
// creates exactly one LogClient, connects it once at startup, and
// reuses it for every log call afterward.
//
// Usage from the Core process (inside CPU.execute(), for example):
//   LogClient log = new LogClient("CORE", "127.0.0.1", LoggingServer.DEFAULT_PORT);
//   log.connect();
//   log.info("Executed PUSH A (SP=08H)");
//   ...
//   log.close();
//
// Usage from the UI process (inside MainWindow, for example):
//   LogClient log = new LogClient("UI", "127.0.0.1", LoggingServer.DEFAULT_PORT);
//   log.connect();
//   log.info("User clicked STEP");
public class LogClient implements AutoCloseable {

    private final String source;
    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter writer;

    public LogClient(String source, String host, int port) {
        this.source = source;
        this.host = host;
        this.port = port;
    }

    // Connects to the Logging process. Call once at process startup.
    public synchronized void connect() throws IOException {
        closeQuietly();
        socket = new Socket(host, port);
        writer = new PrintWriter(
                new OutputStreamWriter(socket.getOutputStream()),
                true /* autoFlush on println */
        );
    }

    public synchronized boolean isConnected() {
        return writer != null && socket != null && !socket.isClosed();
    }

    public void debug(String message) { send(LogLevel.DEBUG, message); }
    public void info(String message)  { send(LogLevel.INFO, message); }
    public void warn(String message)  { send(LogLevel.WARNING, message); }
    public void error(String message) { send(LogLevel.ERROR, message); }

    // Thread-safe: multiple threads within the same process (e.g. the
    // Core process's Swing/step thread and its own IPC listener thread)
    // can log concurrently without corrupting the socket stream.
    // Returns false if the entry could not be delivered (not connected, or the
    // Logging process went away), so callers can reconnect.
    public synchronized boolean send(LogLevel level, String message) {
        if (writer == null) {
            return false;
        }
        LogMessage msg = new LogMessage(Instant.now(), source, level, message);
        writer.println(msg.serialize());
        if (writer.checkError()) {      // PrintWriter swallows IOExceptions; this reveals them
            closeQuietly();
            return false;
        }
        return true;
    }

    private void closeQuietly() {
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        writer = null;
    }

    @Override
    public synchronized void close() {
        closeQuietly();
    }
}
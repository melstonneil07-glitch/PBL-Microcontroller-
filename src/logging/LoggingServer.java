package logging;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Week 4 Logging process (Student 3's component).
//
// IPC mechanism: TCP sockets over the loopback interface (127.0.0.1).
// Sockets (AF_INET, SOCK_STREAM) are part of the POSIX.1-2008 sockets
// API (<sys/socket.h>) -- this is a genuine POSIX IPC mechanism, and
// the one that also runs portably in plain Java on Windows without
// native code or JNI (true shm_open/mq_open-style POSIX IPC has no
// standard Java binding).
//
// Design: one acceptor loop hands each connecting client (the UI
// process, the Core process, or both) its own thread so neither
// blocks the other. All client threads push parsed LogMessages onto
// a single shared BlockingQueue; one dedicated writer thread drains
// that queue and persists entries to disk, so slow disk I/O never
// blocks a client's socket read.
public class LoggingServer {

    public static final int DEFAULT_PORT = 6061;

    private final int port;
    private final Path logFile;
    private final BlockingQueue<LogMessage> queue = new LinkedBlockingQueue<>();

    public LoggingServer(int port, Path logFile) {
        this.port = port;
        this.logFile = logFile;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        Path logFile = args.length > 1 ? Paths.get(args[1]) : Paths.get("logs.txt");

        LoggingServer server = new LoggingServer(port, logFile);
        server.start();
    }

    public void start() throws IOException {

        Thread writerThread = new Thread(this::writerLoop, "log-writer");
        writerThread.setDaemon(true);
        writerThread.start();

        try (ServerSocket serverSocket = new ServerSocket(port, 50, java.net.InetAddress.getByName("127.0.0.1"))) {
            System.out.println("[LoggingServer] Listening on 127.0.0.1:" + port
                    + "  (writing to " + logFile.toAbsolutePath() + ")");

            while (true) {
                Socket client = serverSocket.accept();
                Thread handler = new Thread(
                        () -> handleClient(client),
                        "log-client-" + client.getPort()
                );
                handler.setDaemon(true);
                handler.start();
            }
        }
    }

    // One thread per connected client (UI process, Core process, or
    // both at once). Reads newline-delimited log lines until the
    // client disconnects.
    private void handleClient(Socket client) {
        String remote = client.getInetAddress() + ":" + client.getPort();
        System.out.println("[LoggingServer] Client connected: " + remote);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(client.getInputStream()))) {

            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    LogMessage msg = LogMessage.parse(line);
                    queue.put(msg);
                } catch (RuntimeException parseEx) {
                    // Malformed line (bad format, timestamp or level): skip it
                    // but keep serving this client instead of dropping it.
                    System.err.println("[LoggingServer] Ignored bad log line: " + parseEx.getMessage());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            // Client disconnected or socket error -- nothing else to do.
        } finally {
            System.out.println("[LoggingServer] Client disconnected: " + remote);
            try {
                client.close();
            } catch (IOException ignored) {
            }
        }
    }

    // Single dedicated writer: drains the queue, prints each entry to
    // the console (live view for the demo), and appends it to the
    // persisted log file.
    private void writerLoop() {
        try (BufferedWriter fileWriter = Files.newBufferedWriter(
                logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {

            while (true) {
                LogMessage msg = queue.take(); // blocks until a message arrives
                String line = msg.toString();

                System.out.println(line);
                fileWriter.write(line);
                fileWriter.newLine();
                fileWriter.flush(); // keep the file current even if the process is killed
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            System.err.println("[LoggingServer] Failed to write log file: " + e.getMessage());
        }
    }
}
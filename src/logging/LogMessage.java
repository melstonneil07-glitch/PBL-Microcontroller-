package logging;

import java.time.Instant;

// One log entry, and the wire protocol used to send it over the IPC
// socket connection between the UI/Core processes and the Logging
// process.
//
// Wire format (one line of UTF-8 text per message, newline-terminated):
//   <ISO-8601 timestamp>|<SOURCE>|<LEVEL>|<message>
// e.g.
//   2026-10-02T10:15:30.123Z|CORE|INFO|Executed PUSH A (SP=08H)
//
// The message field is split with a limit of 4, so any '|' characters
// that happen to appear inside the message text itself are left intact
// (they just become part of the 4th captured group) -- only newlines
// are stripped from the message, since the protocol is line-delimited.
public class LogMessage {

    public final Instant timestamp;
    public final String source;   // e.g. "UI", "CORE"
    public final LogLevel level;
    public final String message;

    public LogMessage(Instant timestamp, String source, LogLevel level, String message) {
        this.timestamp = timestamp;
        this.source = source;
        this.level = level;
        this.message = message;
    }

    // Serializes this entry to one line of the wire protocol.
    public String serialize() {
        String safeMessage = message.replace("\n", " ").replace("\r", " ");
        return timestamp.toString() + "|" + source + "|" + level + "|" + safeMessage;
    }

    // Parses one line of the wire protocol back into a LogMessage.
    // Throws IllegalArgumentException on a malformed line.
    public static LogMessage parse(String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Malformed log line: " + line);
        }
        Instant ts = Instant.parse(parts[0]);
        String source = parts[1];
        LogLevel level = LogLevel.valueOf(parts[2]);
        String message = parts.length > 3 ? parts[3] : "";
        return new LogMessage(ts, source, level, message);
    }

    @Override
    public String toString() {
        return String.format("[%s] [%-5s] [%s] %s", timestamp, level, source, message);
    }
}
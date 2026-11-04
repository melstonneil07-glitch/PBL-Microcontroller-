package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the assembly the user types in the UI into the Core's wire format
 * (e.g. "MOV_A_DATA:10;MOV_RN_DATA:R1,3;ADD:R1;HALT").
 *
 * One instruction per line. Case-insensitive. Supported syntax (the CPU's instruction set):
 *
 *   MOV A,#data        MOV Rn,#data
 *   ADD A,Rn           SUBB A,Rn          XRL A,Rn
 *   INC Rn
 *   SJMP label|offset  (offset is relative to the next instruction, same as the CPU)
 *   PUSH A|Rn|#data    POP A|Rn
 *   ENQUEUE A|Rn|#data DEQUEUE A|Rn
 *   HALT
 *
 * Numbers: decimal (10), hex (0x0A or 0AH) or binary (0b1010). Immediates must fit in a byte.
 * Comments start with ';' or '//'. A label is a name followed by ':' (own line or before an instruction).
 */
public final class AssemblyParser {

    /** Thrown for any syntax problem; carries the 1-based line number. */
    public static final class AssemblyException extends Exception {
        public final int line;

        AssemblyException(int line, String message) {
            super("Line " + line + ": " + message);
            this.line = line;
        }
    }

    /** A successfully assembled program. */
    public static final class Result {
        /** Core wire format, ready for LOADPROCS/LOAD. */
        public final String wire;
        /** Number of instructions. */
        public final int size;

        Result(String wire, int size) {
            this.wire = wire;
            this.size = size;
        }
    }

    private AssemblyParser() { }

    private static final class Pending {
        final int line;
        final String mnemonic;
        final String[] operands;

        Pending(int line, String mnemonic, String[] operands) {
            this.line = line;
            this.mnemonic = mnemonic;
            this.operands = operands;
        }
    }

    public static Result parse(String text) throws AssemblyException {
        List<Pending> instructions = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();

        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            String line = stripComment(lines[i]).trim();
            if (line.isEmpty()) continue;

            // Leading labels ("loop:" or "loop: ADD A,R1")
            while (true) {
                int colon = line.indexOf(':');
                if (colon <= 0) break;
                String label = line.substring(0, colon).trim();
                if (!label.matches("[A-Za-z_][A-Za-z0-9_]*")) break;
                String key = label.toUpperCase(Locale.ROOT);
                if (labels.containsKey(key)) throw new AssemblyException(lineNo, "label '" + label + "' defined twice");
                labels.put(key, instructions.size());
                line = line.substring(colon + 1).trim();
            }
            if (line.isEmpty()) continue;

            String mnemonic;
            String rest;
            int space = indexOfWhitespace(line);
            if (space < 0) {
                mnemonic = line;
                rest = "";
            } else {
                mnemonic = line.substring(0, space);
                rest = line.substring(space).trim();
            }
            String[] operands = rest.isEmpty() ? new String[0] : rest.split("\\s*,\\s*", -1);
            instructions.add(new Pending(lineNo, mnemonic.toUpperCase(Locale.ROOT), operands));
        }

        if (instructions.isEmpty()) {
            throw new AssemblyException(1, "the program is empty - type at least one instruction (e.g. HALT)");
        }

        StringBuilder wire = new StringBuilder();
        for (int index = 0; index < instructions.size(); index++) {
            Pending p = instructions.get(index);
            if (index > 0) wire.append(';');
            wire.append(encode(p, index, labels));
        }
        return new Result(wire.toString(), instructions.size());
    }

    private static String encode(Pending p, int index, Map<String, Integer> labels) throws AssemblyException {
        String[] ops = p.operands;
        switch (p.mnemonic) {
            case "MOV": {
                expectCount(p, 2);
                if (!ops[1].startsWith("#")) {
                    throw new AssemblyException(p.line, "MOV needs an immediate source, e.g. MOV " + ops[0] + ",#10");
                }
                int value = immediate(p, ops[1].substring(1));
                if (isA(ops[0])) return "MOV_A_DATA:" + value;
                if (isRegister(ops[0])) return "MOV_RN_DATA:" + register(p, ops[0]) + "," + value;
                throw new AssemblyException(p.line, "MOV destination must be A or R0-R7, got '" + ops[0] + "'");
            }
            case "ADD":
            case "SUBB":
            case "XRL": {
                expectCount(p, 2);
                if (!isA(ops[0])) throw new AssemblyException(p.line, p.mnemonic + " destination must be A");
                return p.mnemonic + ":" + register(p, ops[1]);
            }
            case "INC": {
                expectCount(p, 1);
                return "INC:" + register(p, ops[0]);
            }
            case "SJMP": {
                expectCount(p, 1);
                String target = ops[0];
                Integer label = labels.get(target.toUpperCase(Locale.ROOT));
                if (label != null) {
                    // The CPU adds the offset to the PC *after* the fetch, i.e. index + 1.
                    return "SJMP:" + (label - (index + 1));
                }
                try {
                    return "SJMP:" + parseNumber(target);
                } catch (NumberFormatException ex) {
                    throw new AssemblyException(p.line, "unknown label or offset '" + target + "'");
                }
            }
            case "PUSH": {
                expectCount(p, 1);
                return "PUSH:" + byteOperand(p, ops[0], true);
            }
            case "ENQUEUE": {
                expectCount(p, 1);
                return "ENQUEUE:" + byteOperand(p, ops[0], true);
            }
            case "POP": {
                expectCount(p, 1);
                return "POP:" + byteOperand(p, ops[0], false);
            }
            case "DEQUEUE": {
                expectCount(p, 1);
                return "DEQUEUE:" + byteOperand(p, ops[0], false);
            }
            case "HALT": {
                expectCount(p, 0);
                return "HALT";
            }
            default:
                throw new AssemblyException(p.line, "unknown instruction '" + p.mnemonic + "'");
        }
    }

    // A / Rn, and optionally #data (sources only). Returns the Core wire operand.
    private static String byteOperand(Pending p, String op, boolean allowImmediate) throws AssemblyException {
        if (isA(op)) return "A";
        if (isRegister(op)) return register(p, op);
        if (allowImmediate && op.startsWith("#")) return "#" + immediate(p, op.substring(1));
        throw new AssemblyException(p.line, p.mnemonic + " operand must be A, R0-R7"
                + (allowImmediate ? " or #data" : "") + ", got '" + op + "'");
    }

    private static boolean isA(String op) {
        return op.equalsIgnoreCase("A");
    }

    private static boolean isRegister(String op) {
        return op.matches("(?i)R[0-7]");
    }

    private static String register(Pending p, String op) throws AssemblyException {
        if (!isRegister(op)) throw new AssemblyException(p.line, "expected a register R0-R7, got '" + op + "'");
        return op.toUpperCase(Locale.ROOT);
    }

    private static int immediate(Pending p, String text) throws AssemblyException {
        try {
            int value = parseNumber(text);
            if (value < -128 || value > 255) {
                throw new AssemblyException(p.line, "value " + text + " does not fit in a byte (-128..255)");
            }
            return value & 0xFF;
        } catch (NumberFormatException ex) {
            throw new AssemblyException(p.line, "'" + text + "' is not a valid number");
        }
    }

    static int parseNumber(String raw) {
        String s = raw.trim().toUpperCase(Locale.ROOT);
        boolean negative = s.startsWith("-");
        if (negative) s = s.substring(1);
        int value;
        if (s.startsWith("0X")) value = Integer.parseInt(s.substring(2), 16);
        else if (s.startsWith("0B")) value = Integer.parseInt(s.substring(2), 2);
        else if (s.endsWith("H")) value = Integer.parseInt(s.substring(0, s.length() - 1), 16);
        else value = Integer.parseInt(s);
        return negative ? -value : value;
    }

    private static void expectCount(Pending p, int expected) throws AssemblyException {
        if (p.operands.length != expected) {
            throw new AssemblyException(p.line, p.mnemonic + " takes " + expected + " operand(s), got " + p.operands.length);
        }
    }

    private static int indexOfWhitespace(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isWhitespace(s.charAt(i))) return i;
        }
        return -1;
    }

    private static String stripComment(String line) {
        int semicolon = line.indexOf(';');
        int slashes = line.indexOf("//");
        int cut = -1;
        if (semicolon >= 0) cut = semicolon;
        if (slashes >= 0 && (cut < 0 || slashes < cut)) cut = slashes;
        return cut >= 0 ? line.substring(0, cut) : line;
    }
}

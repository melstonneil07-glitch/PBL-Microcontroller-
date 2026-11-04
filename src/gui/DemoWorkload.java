package gui;

/** Demo workload shared by the GUI and the console UI (one 14-instruction program; the Core can run several): {name, arrival tick, program in Core wire format}. */
public final class DemoWorkload {

    public static final String[][] PROCESSES = {
        {"P1", "0", "MOV_A_DATA:10;MOV_RN_DATA:R1,3;ADD:R1;PUSH:A;MOV_A_DATA:99;POP:A;ENQUEUE:A;ENQUEUE:#20;"
                + "DEQUEUE:R2;SUBB:R1;XRL:R1;INC:R1;SJMP:0;HALT"}
    };

    /** The demo program in the assembly syntax of the UI editor (same program as PROCESSES[0]). */
    public static final String DEFAULT_ASSEMBLY =
            "; Type your program here, one instruction per line, then press LOAD.\n"
          + "MOV A,#10\n"
          + "MOV R1,#3\n"
          + "ADD A,R1\n"
          + "PUSH A\n"
          + "MOV A,#99\n"
          + "POP A\n"
          + "ENQUEUE A\n"
          + "ENQUEUE #20\n"
          + "DEQUEUE R2\n"
          + "SUBB A,R1\n"
          + "XRL A,R1\n"
          + "INC R1\n"
          + "SJMP 0\n"
          + "HALT\n";

    private DemoWorkload() { }

    /** The LOADPROCS command for the built-in demo workload. */
    public static String loadCommand() {
        return loadCommand(PROCESSES);
    }

    /** The LOADPROCS command understood by the Core process, for any process list {name, arrival, program}. */
    public static String loadCommand(String[][] processes) {
        StringBuilder command = new StringBuilder("LOADPROCS");
        for (String[] p : processes) {
            command.append('|').append(p[0]).append('@').append(p[1]).append('@').append(p[2]);
        }
        return command.toString();
    }
}

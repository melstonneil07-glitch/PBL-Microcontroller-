package gui;

/** Demo FCFS workload shared by the GUI and the console UI: {name, arrival tick, program in Core wire format}. */
public final class DemoWorkload {

    public static final String[][] PROCESSES = {
        {"P1", "0", "MOV_A_DATA:10;MOV_RN_DATA:R1,3;ADD:R1;MOV_DIRECT_A:48;PUSH:A;MOV_A_DATA:99;POP:A;ENQUEUE:A;"
                + "ENQUEUE:#20;DEQUEUE:R2;SUBB:R1;ANL:R1;MOV_DIRECT_A:49;INC:R1;SJMP:0;HALT"},
        {"P2", "2", "MOV_A_DATA:5;MOV_RN_DATA:R1,7;ADD:R1;MOV_DIRECT_A:64;PUSH:A;POP:R3;HALT"},
        {"P3", "4", "MOV_RN_DATA:R1,200;MOV_A_DATA:100;ADD:R1;MOV_DIRECT_A:80;MOV_DIRECT_DATA:81,171;ENQUEUE:A;DEQUEUE:R2;HALT"}
    };

    private DemoWorkload() { }

    /** The LOADPROCS command understood by the Core process. */
    public static String loadCommand() {
        StringBuilder command = new StringBuilder("LOADPROCS");
        for (String[] p : PROCESSES) {
            command.append('|').append(p[0]).append('@').append(p[1]).append('@').append(p[2]);
        }
        return command.toString();
    }
}

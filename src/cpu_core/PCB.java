package cpu_core;

import java.util.ArrayList;

// Process Control Block: everything the OS layer tracks about one program.
public class PCB {

    public final int pid;
    public final String name;
    public final int arrivalTime;                 // in clock ticks (1 tick = 1 instruction)
    public final ArrayList<Instruction> program;

    public ProcessState state = ProcessState.NEW;
    public CpuContext context;                    // saved CPU state while not running

    public int executed = 0;                      // instructions executed so far (CPU burst)
    public int startTime = -1;                    // first tick on the CPU
    public int finishTime = -1;
    public String fault = null;                   // set if the process died with an error

    public PCB(int pid, String name, int arrivalTime, ArrayList<Instruction> program) {
        this.pid = pid;
        this.name = name;
        this.arrivalTime = arrivalTime;
        this.program = program;
        this.context = CpuContext.fresh(program);
    }

    // Time spent in the ready queue. Live value for unfinished processes.
    public int waitingTime(int clock) {
        if (state == ProcessState.NEW) return 0;
        int end = (state == ProcessState.TERMINATED) ? finishTime : clock;
        return Math.max(0, end - arrivalTime - executed);
    }

    public int turnaroundTime() {
        return state == ProcessState.TERMINATED ? finishTime - arrivalTime : -1;
    }

    // Puts the process back to its just-created state (used by RESET).
    void resetToNew() {
        state = ProcessState.NEW;
        context = CpuContext.fresh(program);
        executed = 0;
        startTime = -1;
        finishTime = -1;
        fault = null;
    }
}

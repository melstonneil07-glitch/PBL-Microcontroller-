package cpu_core;

import java.util.ArrayList;
import java.util.List;

// The OS layer on top of the CPU: owns the process table, the clock,
// the scheduler and the context switch. One tick() = one clock tick:
//   1. admit processes whose arrival time has been reached
//   2. if the CPU is free, ask the scheduler for the next process and
//      context-switch it in
//   3. execute one instruction of the running process
//   4. if it halted (or faulted), terminate it
// Time unit: 1 tick = 1 executed instruction.
public class ProcessManager {

    // One bar of the Gantt chart. pid 0 = CPU idle.
    public static final class Segment {
        public final int pid;
        public final int start;
        public int end;
        Segment(int pid, int start, int end) { this.pid = pid; this.start = start; this.end = end; }
    }

    // What happened during one tick (consumed by CoreProcess for IPC/logging).
    public static final class TickResult {
        public enum Kind { EXECUTED, IDLE, DONE }
        public Kind kind;
        public final List<String> events = new ArrayList<>();   // human-readable scheduler events
        public PCB process;          // process that ran this tick (EXECUTED)
        public Instruction instruction;
        public int oldPc;
        public boolean terminated;   // process finished on this tick
    }

    private final CPU cpu;
    private final Scheduler scheduler;
    private final List<PCB> table = new ArrayList<>();
    private final List<Segment> gantt = new ArrayList<>();

    private int clock = 0;
    private int nextPid = 1;
    private int contextSwitches = 0;
    private PCB running = null;
    private PCB lastRan = null;

    public ProcessManager(CPU cpu, Scheduler scheduler) {
        this.cpu = cpu;
        this.scheduler = scheduler;
    }

    // ---------- process table ----------

    public PCB create(String name, int arrivalTime, ArrayList<Instruction> program) {
        if (program == null || program.isEmpty()) {
            throw new IllegalArgumentException("Process " + name + " has an empty program");
        }
        if (arrivalTime < 0) {
            throw new IllegalArgumentException("Arrival time must be >= 0 for " + name);
        }
        PCB pcb = new PCB(nextPid++, name, arrivalTime, program);
        table.add(pcb);
        return pcb;
    }

    // Removes every process and restarts the clock.
    public void clear() {
        table.clear();
        nextPid = 1;
        restartClock();
    }

    // Keeps the process table but returns every process to NEW and clock to 0.
    public void reset() {
        for (PCB p : table) p.resetToNew();
        restartClock();
    }

    private void restartClock() {
        scheduler.clear();
        gantt.clear();
        clock = 0;
        contextSwitches = 0;
        running = null;
        lastRan = null;
    }

    // ---------- one clock tick ----------

    public TickResult tick() {
        TickResult result = new TickResult();

        // 1. admit arrivals (table order = creation order = FCFS tie-break)
        List<PCB> arriving = new ArrayList<>();
        for (PCB p : table) {
            if (p.state == ProcessState.NEW && p.arrivalTime <= clock) arriving.add(p);
        }
        arriving.sort((x, y) -> x.arrivalTime != y.arrivalTime
                ? Integer.compare(x.arrivalTime, y.arrivalTime)
                : Integer.compare(x.pid, y.pid));
        for (PCB p : arriving) {
            p.state = ProcessState.READY;
            scheduler.add(p);
            result.events.add("ADMIT|PID=" + p.pid + "|PNAME=" + p.name + "|CLOCK=" + clock);
        }

        // 2. dispatch if the CPU is free
        if (running == null) {
            PCB next = scheduler.selectNext();
            if (next == null) {
                if (allTerminated()) {
                    result.kind = TickResult.Kind.DONE;
                    return result;
                }
                result.kind = TickResult.Kind.IDLE;   // processes exist but none has arrived yet
                addToGantt(0);
                clock++;
                return result;
            }
            dispatch(next, result);
        }

        // 3. execute one instruction
        PCB pcb = running;
        result.kind = TickResult.Kind.EXECUTED;
        result.process = pcb;
        result.oldPc = cpu.getPC();
        boolean faulted = false;
        try {
            result.instruction = cpu.step();
        } catch (RuntimeException ex) {
            faulted = true;
            pcb.fault = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        }
        if (result.instruction != null) pcb.executed++;
        addToGantt(pcb.pid);
        clock++;

        // 4. terminate if halted / ran off the program / faulted
        if (faulted || !cpu.isRunning()) {
            pcb.state = ProcessState.TERMINATED;
            pcb.finishTime = clock;
            pcb.context = cpu.saveContext();
            running = null;
            lastRan = pcb;
            result.terminated = true;
            result.events.add("TERMINATE|PID=" + pcb.pid + "|PNAME=" + pcb.name
                    + "|CLOCK=" + clock + "|EXECUTED=" + pcb.executed
                    + (pcb.fault != null ? "|FAULT=" + pcb.fault : ""));
        }
        return result;
    }

    private void dispatch(PCB next, TickResult result) {
        if (lastRan != null && lastRan != next) {
            contextSwitches++;
            result.events.add("CONTEXT_SWITCH|FROM=" + lastRan.name + "|TO=" + next.name
                    + "|CLOCK=" + clock);
        }
        cpu.restoreContext(next.context);
        next.state = ProcessState.RUNNING;
        if (next.startTime < 0) next.startTime = clock;
        running = next;
        result.events.add("DISPATCH|PID=" + next.pid + "|PNAME=" + next.name
                + "|CLOCK=" + clock + "|WAITED=" + next.waitingTime(clock));
    }

    private void addToGantt(int pid) {
        Segment last = gantt.isEmpty() ? null : gantt.get(gantt.size() - 1);
        if (last != null && last.pid == pid && last.end == clock) {
            last.end = clock + 1;
        } else {
            gantt.add(new Segment(pid, clock, clock + 1));
        }
    }

    // ---------- queries ----------

    public boolean allTerminated() {
        for (PCB p : table) if (p.state != ProcessState.TERMINATED) return false;
        return true;
    }

    public boolean isEmpty() { return table.isEmpty(); }
    public int getClock() { return clock; }
    public int getContextSwitches() { return contextSwitches; }
    public PCB getRunning() { return running; }
    public Scheduler getScheduler() { return scheduler; }
    public List<PCB> getProcesses() { return new ArrayList<>(table); }
    public List<Segment> getGantt() { return new ArrayList<>(gantt); }

    public double averageWaitingTime() {
        int n = 0, sum = 0;
        for (PCB p : table) if (p.state == ProcessState.TERMINATED) { n++; sum += p.waitingTime(clock); }
        return n == 0 ? 0 : (double) sum / n;
    }

    public double averageTurnaroundTime() {
        int n = 0, sum = 0;
        for (PCB p : table) if (p.state == ProcessState.TERMINATED) { n++; sum += p.turnaroundTime(); }
        return n == 0 ? 0 : (double) sum / n;
    }
}

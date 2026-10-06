package cpu_core;

import java.util.List;

// A CPU scheduling policy. ProcessManager only talks to this interface,
// so Round Robin / Priority can be added later without touching it.
public interface Scheduler {

    String name();

    // Called when a process becomes READY.
    void add(PCB process);

    // Removes and returns the process that should run next, or null.
    PCB selectNext();

    boolean isEmpty();

    // Ready queue contents in dispatch order (for display).
    List<PCB> readyList();

    void clear();
}

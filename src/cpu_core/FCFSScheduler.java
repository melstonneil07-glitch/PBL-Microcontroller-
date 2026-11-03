package cpu_core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

// First-Come, First-Served: a plain FIFO ready queue. The process that
// became READY first runs first, and (non-preemptive) keeps the CPU
// until it terminates.
public class FCFSScheduler implements Scheduler {

    private final ArrayDeque<PCB> readyQueue = new ArrayDeque<>();

    @Override public String name() { return "FCFS"; }

    @Override public void add(PCB process) { readyQueue.addLast(process); }

    @Override public PCB selectNext() { return readyQueue.pollFirst(); }

    @Override public boolean isEmpty() { return readyQueue.isEmpty(); }

    @Override public List<PCB> readyList() { return new ArrayList<>(readyQueue); }

    @Override public void clear() { readyQueue.clear(); }
}

package cpu_core;

// Life-cycle of a process under the scheduler.
//   NEW        -> created, arrival time not reached yet
//   READY      -> admitted, waiting in the ready queue
//   RUNNING    -> currently owns the CPU
//   TERMINATED -> executed HALT (or faulted)
public enum ProcessState {
    NEW, READY, RUNNING, TERMINATED
}

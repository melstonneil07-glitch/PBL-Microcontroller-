# Week 4 – Multi-process simulator and IPC

## Architecture

```
 +-------------+   commands (STEP, LOADPROCS, RESET)    +--------------------------------+
 | UI process  | -------------------------------------> | Core process                   |
 | gui.        |   TCP 127.0.0.1:6060, line protocol    | cpu_core.CoreProcess           |
 | MainWindow  | <------------------------------------- |  CPU, Memory, Stack, Queue,    |
 +------+------+   replies/events (STEP, PROCS, ...)    |  FCFS scheduler, PCBs          |
        |                                                +---------------+----------------+
        | log entries                                                    | log entries
        | TCP 127.0.0.1:6061 (LogMessage format)                         | TCP 127.0.0.1:6061
        v                                                                v
 +----------------------------------------------------------------------------------+
 | Logging process  logging.LoggingServer  ->  simulator.log                        |
 +----------------------------------------------------------------------------------+
```

Flow: UI -> Core -> CPU / Memory / Stack / Queue -> Logger. Each box is its own JVM / OS process.

## IPC mechanism and why

TCP sockets on the loopback interface (POSIX sockets API; `java.net.Socket` is a thin wrapper over it).
* Bidirectional and ordered, so one connection carries commands and replies.
* The Core and Logging processes act as servers, so the UI/Core/Logger can be started in any order,
  restarted independently, and a second client (e.g. the console UI) can attach.
* Works unchanged on Windows, Linux and macOS (pipes/fork/shared memory are not portable from Java).
* Costs more per message than shared memory or pipes (see "IPC overhead" in the benchmark task).

Wire format: one text line per message, `KIND|KEY=VALUE|KEY=VALUE...`. See the header comment of
`cpu_core/CoreProcess.java` for the command list; `PROCS` carries the process table, ready queue and Gantt data.

## Threads (and where FCFS applies)

| Process | Threads |
|---------|---------|
| UI      | Swing event thread; `Core-TCP-Receiver` (reads replies); connect/retry threads for Core and Logger |
| Core    | worker (executes commands); `Core-UI-Reader` (socket -> FIFO queue); `Core-Log-Forwarder` (FIFO queue -> Logger) |
| Logging | acceptor thread; one thread per client; one writer thread |

* **Requests are served first-come-first-served**: the reader thread appends UI commands to a FIFO queue
  and a single worker takes them in arrival order. Only the worker touches CPU/scheduler state.
* **Processes are scheduled FCFS** (`FCFSScheduler`): 1 tick = 1 instruction, non-preemptive, context switch
  via `CPU.saveContext()/restoreContext()`; each process has its own registers, stack, queue and data memory.
* Logging from the Core never blocks the CPU: entries are queued and forwarded asynchronously, and are
  delivered later if the Logger was not up yet.

## Running

```
scripts/run_all.bat            (Windows)         scripts/run_all.sh   (Linux/macOS)
scripts/run_all.bat --console  text UI           scripts/run_all.sh --console
```
or manually, in three terminals (any order): `java -cp out logging.LoggingServer`,
`java -cp out cpu_core.CoreProcess`, `java -cp out;lib/jna-5.15.0.jar gui.MainWindow`.
Change ports with `-Dsim.corePort=` / `-Dsim.logPort=` on **all** processes (default 6060 / 6061).
If the UI shows "Core: Disconnected", the status bar says which port it is trying and the SYSTEM LOG shows the
connection error. If the UI is started on its own (e.g. from an IDE) and finds no Core/Logger after a few seconds,
it starts them itself as separate processes (`app.ServiceSpawner`) and stops them when the window closes.

## Tests

* `java -cp out app.IpcSystemTest` starts real separate processes and checks: 3 distinct PIDs, FCFS results
  (finish 14/20/26), log file content, start-order independence, UI reconnect, FIFO ordering of pipelined requests.
* `java -cp out cpu_core.SchedulerTest` – FCFS scheduler, PCB, context isolation (unit level).
* `java -cp out logging.LoggingIPCTest` – Logging process IPC.

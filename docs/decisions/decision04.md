# Week 4:

## Work Completed:
**1. Implemented separate UI, Core, and Logging processes.**

**2. Implemented TCP socket communication between UI and Core.**

**3. Implemented TCP socket communication between UI, Core, and Logging.**

**4. Implemented the Core command-processing system using a FIFO command queue.**

**5. Integrated CPU execution with process management and FCFS scheduling.**

**6. Implemented Process Control Blocks (PCB) and CPU context switching.**

**7. Implemented asynchronous logging using a BlockingQueue and a dedicated writer thread.**

**8. Added process launching and integration support for the three-process simulator.**

## Work Tested:
**1. Tested TCP communication between UI and Core.**

**2. Tested log message transmission from UI and Core to Logging.**

**3. Tested concurrent logging from UI and Core clients.**

**4. Tested log message storage in the log file.**

**5. Tested FIFO ordering of pipelined Core commands.**

**6. Tested FCFS scheduling and process execution order.**

**7. Tested PCB state, register isolation, and stack/queue context isolation.**

**8. Tested process termination, fault handling, and reset functionality.**

**9. Added integration tests for process startup, reconnection, and communication between separate JVM processes.**

## Work Accepted:
**1. Three-process simulator architecture (UI, Core, and Logging).**

**2. TCP socket-based inter-process communication.**

**3. Core command processing using a FIFO queue.**

**4. Logging synchronization using BlockingQueue and a dedicated writer thread.**

**5. FCFS scheduling and Process Control Block implementation.**

**6. CPU context saving and restoration for process isolation.**

**7. IPC and scheduler test implementations.**

**8. Integration and process-launching support.**

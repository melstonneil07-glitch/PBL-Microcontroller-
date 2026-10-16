**Date: 06-10-2026**  

**Meeting Number: 4** 

**Participants: 4**

## Agenda:
**1. Discussing the implementation of separate UI, Core, and Logging processes.**

**2. Discussing TCP socket communication between the processes.**

**3. Discussing Core command processing using a FIFO queue.**

**4. Discussing FCFS scheduling, PCB, and CPU context switching.**

**5. Discussing asynchronous logging and synchronization.**

**6. Reviewing IPC, scheduler, and integration testing.**

## Discussion:
**1. The team discussed separating the simulator into three independent processes: UI, Core, and Logging.**

**2. The team discussed implementing TCP socket communication between UI and Core, and between UI/Core and Logging.**

**3. The team discussed processing Core commands in FIFO order using a command queue and a worker thread.**

**4. The team discussed FCFS scheduling, Process Control Blocks (PCB), and CPU context saving and restoration.**

**5. The team discussed asynchronous logging using a `BlockingQueue` and a dedicated writer thread.**

**6. The team discussed testing log transmission, FIFO command ordering, FCFS scheduling, process isolation, and reset functionality.**

**7. The team reviewed integration tests for separate JVM processes, process startup, reconnection, and communication.**

**8. The team discussed measuring execution time, IPC overhead, CPU usage, and memory usage.**

## Decisions:
**1. Maintain the three-process architecture consisting of UI, Core, and Logging.**

**2. Use TCP sockets for inter-process communication.**

**3. Use a FIFO command queue and a single worker thread to process Core commands in arrival order.**

**4. Use FCFS scheduling with PCB-based process management and CPU context switching.**

**5. Use a `BlockingQueue` and a dedicated writer thread for asynchronous logging.**

**6. Perform IPC, scheduler, and integration testing to verify communication and process isolation.**

**7. Record benchmark measurements and update the Week 4 project documentation.**

## Action Items:
**1. Melston Neil AlbuquerqueIntegrate → UI, Core, and Logging processes; perform system integration testing and benchmark execution time, IPC overhead, CPU usage, and memory usage.**

**2. Disha N Poojary → Verify UI-to-Core TCP communication, command transmission, response handling, and UI reconnection.**

**3. Vaishnavi → Verify Core command processing, FIFO ordering, FCFS scheduling, PCB management, and CPU context switching.**

**4. Nischith Srinivasa Shetty → Verify Logging process communication, log message transmission, `BlockingQueue` synchronization, log file storage, and logging error handling.**
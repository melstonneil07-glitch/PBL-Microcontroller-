# Week 4 Weekly Status Report:

## Planned Work:
**1. Separate the simulator into UI, Core, and Logging processes.**

**2. Select suitable communication mechanisms for inter-process communication (IPC).**

**3. Define communication interfaces between the three processes.**

**4. Implement IPC between UI, Core, and Logging processes.**

**5. Integrate CPU, Memory, Stack, and Queue into the Core Process.**

**6. Implement the Logging Process for execution and error logging.**

**7. Use threads where appropriate within the processes.**

**8. Integrate all three processes and resolve IPC/interface issues.**

**9. Create and execute IPC test cases.**

## Completed Work:
**1. Separated the simulator architecture into UI, Core, and Logging processes.**

**2. Implemented TCP socket communication between the UI and Core processes.**

**3. Established communication interfaces for exchanging commands and CPU state information.**

**4. Integrated CPU, Memory, Stack, and Queue functionality into the Core Process.**

**5. Implemented UI controls for loading programs, stepping, running, and resetting the simulator.**

**6. Implemented CPU state updates to display register values, PC, SP, flags, and queue information.**

**7. Implemented the Logging Process for recording execution events and errors.**

**8. Integrated the process communication and logging components.**

**9. Performed IPC and integration testing to identify communication and interface issues.**

**10. Reviewed synchronization requirements for communication between processes and threads.**

## Issues Encountered:
**1. Adapting the IPC implementation to the Windows environment.**

**2. Ensuring compatibility between the UI and Core communication interfaces.**

**3. Maintaining consistent CPU state updates during instruction execution.**

**4. Handling communication failures and errors between processes.**

**5. Coordinating execution logging with Core Process operations.**

**6. Managing synchronization between threads to prevent inconsistent state updates.**

## Decisions Made:
**1. The simulator will be divided into three separate processes: UI, Core, and Logging.**

**2. The UI Process will handle user interaction and display simulator information.**

**3. The Core Process will handle CPU execution, Memory, Stack, and Queue operations.**

**4. The Logging Process will record execution events and errors.**

**5. TCP sockets will be used for communication between the UI and Core processes.**

**6. Process communication will use clearly defined interfaces and message formats.**

**7. Threads will be used where appropriate to support concurrent operations.**

**8. Synchronization will be applied where necessary to maintain consistent shared state and communication.**

**9. IPC testing will cover command transmission, state updates, error handling, and process integration.**

## Work Accepted:
**1. Three-process simulator architecture consisting of UI, Core, and Logging.**

**2. TCP socket communication between the UI and Core processes.**

**3. Core Process integration with CPU, Memory, Stack, and Queue.**

**4. UI controls for program loading, execution, stepping, and resetting.**

**5. CPU state display and updates in the GUI.**

**6. Logging functionality for execution events and errors.**

**7. Integration of process communication interfaces.**

**8. IPC and integration test cases for validating communication and execution.**


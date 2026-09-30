# Week 3 Weekly Status Report:

## Planned Work:
**1. Implement Read/Write operations for Data Memory.**

**2. Implement Read/Write operations for Program Memory.**

**3. Implement Stack Memory with Stack Pointer (SP).**

**4. Implement PUSH and POP operations.**

**5. Implement Stack Peek and LIFO ordering.**

**6. Implement FIFO Queue Memory.**

**7. Implement Enqueue and Dequeue operations.**

**8. Implement Queue empty, underflow, and full conditions.**

**9. Display Memory, Stack/SP, and Queue information in the GUI.**

**10. Create a processor-specific Assembly demonstration program for Queue operations.**

**11. Perform component and integration testing.**

## Completed Work:
**1. Implemented Data Memory Read/Write operations.**

**2. Implemented Program Memory Read/Write operations.**

**3. Implemented Stack Memory with Stack Pointer (SP).**

**4. Implemented PUSH and POP operations.**

**5. Implemented Stack Peek functionality.**

**6. Verified Stack LIFO ordering.**

**7. Implemented FIFO Queue Memory.**

**8. Implemented Enqueue and Dequeue operations.**

**9. Implemented Queue FIFO ordering.**

**10. Implemented Stack and Queue boundary condition handling.**

**11. Integrated Memory, Stack, and Queue functionality with the simulator.**

## Pending Work:
**1. Further improvement of GUI visualization for Memory, Stack, and Queue.**

**2. Further refinement of integration and error handling.**

## Issues Encountered:
**1. Handling invalid memory addresses and memory access errors.**

**2. Managing Stack empty, underflow, and overflow conditions.**

**3. Managing Queue empty, underflow, and full conditions.**

**4. Ensuring correct FIFO and LIFO ordering during operations.**

## Decisions Made:
**1. Memory operations will be handled through dedicated memory modules.**

**2. Stack functionality will use a dedicated Stack Memory module with a Stack Pointer (SP).**

**3. Stack operations will follow LIFO ordering.**

**4. Queue functionality will follow FIFO ordering.**

**5. Separate Queue operations will be provided for Enqueue and Dequeue.**

**6. Empty, underflow, overflow, and full conditions will be explicitly handled.**

**7. GUI will display the current Memory, Stack/SP, and Queue states.**

**8. Testing will include both individual component tests and integration tests.**

## Work Accepted:
**1. Data Memory Read/Write functionality.**

**2. Program Memory Read/Write functionality.**

**3. Stack Memory and Stack Pointer implementation.**

**4. PUSH, POP, and Peek operations.**

**5. Stack LIFO ordering.**

**6. FIFO Queue implementation.**

**7. Enqueue and Dequeue operations.**

**8. Queue FIFO ordering.**

**9. Stack and Queue boundary condition handling.**

**10. Memory, Stack, and Queue test results.**

## Work Not Completed:
**1. Complete GUI visualization improvements.**

**2. Further integration and refinement of all Week-3 components.**

**3. Additional debugging and refinement where required.**

## Work Carried Forward:

**1. Further GUI improvements.**

**2. Further simulator integration.**

**3. Improved error handling.**

**4. Additional testing and debugging.**

## Week 4 Plan:
**1. Separate the simulator into UI, Core, and Logging processes.**

**2. Investigate and select a suitable POSIX IPC mechanism.**

**3. Define communication between the three processes.**

**4. Implement IPC between UI, Core, and Logging processes.**

**5. Integrate CPU, Memory, Stack, and Queue into the Core Process.**

**6. Implement the Logging Process for execution and error logging.**

**7. Use threads where appropriate within the processes.**

**8. Integrate all three processes and resolve IPC/interface issues.**

**9. Create and execute IPC test cases.**
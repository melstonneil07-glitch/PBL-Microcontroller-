# Week 4 Simulator Test Results

## Test Components

The simulator includes:
- CPU Instruction Execution
- Data Memory
- Program Memory
- Stack Memory
- Queue Memory
- GUI and RAM View
- Reset and HALT Operations

## Test Results

| Test Case | Expected Result | Actual Result | Status |
|---|---|---|---|
| MOV A, #10 | A = 10 | A = 10 | PASS |
| MOV R1, #7 | R1 = 7 | R1 = 7 | PASS |
| ADD A, R1 | A = 8 | A = 8 | PASS |
| SUBB A, R1 | A = 6 | A = 6 | PASS |
| ANL A, R1 | A = 8 | A = 8 | PASS |
| INC R1 | R1 = 6 | R1 = 6 | PASS |
| SJMP | PC = 3 | PC = 3 | PASS |
| HALT | Running = false | Running = false | PASS |
| Reset Stack Pointer | SP = 7 | SP = 7 | PASS |
| Demo Final A | A = 2 | A = 2 | PASS |
| Demo Final R1 | R1 = 4 | R1 = 4 | PASS |
| Demo Running State | Running = false | Running = false | PASS |
| Data Memory Write | RAM[10] = 50 | RAM[10] = 50 | PASS |
| PUSH/POP Final A | A = 13 | A = 13 | PASS |
| PUSH/POP Stack Pointer | SP = 7 | SP = 7 | PASS |
| POP on Empty Stack | Empty condition detected | Detected | PASS |
| Queue FIFO — R0 | R0 = 5 | R0 = 5 | PASS |
| Queue FIFO — R1 | R1 = 7 | R1 = 7 | PASS |
| DEQUEUE on Empty Queue | Empty condition detected | Detected | PASS |
| ENQUEUE on Full Queue | Full condition detected | Detected | PASS |
| MOV R1, #3 — RAM View | RAM[01H] = 3 | RAM[01H] = 3 | PASS |
| PUSH A — RAM View | RAM[01H] = 3, RAM[08H] = 9 | Correct values displayed | PASS |
| Registers After HALT | RAM values remain unchanged | Values remained unchanged | PASS |

## Conclusion

All **23 out of 23 test cases passed successfully**.

The CPU instruction execution, Data Memory, Stack, Queue, reset and HALT operations, and RAM view behaved as expected during testing. Stack LIFO ordering, Queue FIFO ordering, and empty/full boundary conditions were also verified.
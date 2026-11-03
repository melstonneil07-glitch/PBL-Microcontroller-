# Memory System Test Results — Week 3

## Memory Components

The simulator includes:

- Program Memory
- Data Memory
- Stack Memory
- Queue Memory

## Test Results

| Test Case | Expected Result | Actual Result | Status |
|---|---|---|---|
| Data Memory Read/Write | Data should be stored and retrieved correctly | Correct value retrieved | PASS |
| Program Memory Read/Write | Program byte should be stored and retrieved correctly | Correct value retrieved | PASS |
| Stack Push/Pop | Pushed value should be returned by pop | Correct value returned | PASS |
| Stack Peek | Top stack value should be returned without removing it | Correct value returned | PASS |
| Stack Order | Stack should follow LIFO order | LIFO order verified | PASS |
| CPU-Data Memory | CPU should store accumulator value in data memory | Correct value stored | PASS |
| Invalid Data Memory Address | Invalid address should be rejected | Exception generated | PASS |
| Stack Empty Condition | Empty stack condition should be detected | Empty condition detected | PASS |
| Stack Underflow | Stack underflow should be detected | Underflow detected | PASS |
| Stack Overflow | Stack overflow should be detected | Overflow detected | PASS |
| Queue Memory | Queue data should be stored and retrieved correctly | Correct value retrieved | PASS |
| Queue FIFO Order | Queue should follow FIFO order | FIFO order verified | PASS |
| Queue Empty Condition | Empty queue condition should be detected | Empty condition detected | PASS |
| Queue Underflow | Queue underflow should be detected | Underflow detected | PASS |
| Queue Full Condition | Full queue condition should be detected | Full condition detected | PASS |

## Conclusion

All implemented **Memory, Stack, and Queue operations** tested successfully.

The **memory system, stack operations, and FIFO queue functionality** are functioning correctly for the current **Week-3 implementation**.
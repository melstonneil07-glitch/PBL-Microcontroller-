# Memory System Test Results

## Memory Components

The simulator includes:
- Program Memory
- Data Memory
- Stack Memory

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

## Conclusion

All implemented memory operations tested successfully.
The memory system and CPU-data-memory interaction are functioning correctly for the current Week-2 prototype.
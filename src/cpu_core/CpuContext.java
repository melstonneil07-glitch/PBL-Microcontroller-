package cpu_core;

import java.util.ArrayList;
import memory.DataMemory;
import memory.QueueMemory;
import memory.StackMemory;

// Everything the CPU must remember to pause a process and resume it
// later: registers, PC, flags, and the process's private memories.
// Stored inside the process's PCB. CPU.saveContext() fills one in and
// CPU.restoreContext() loads one back -- that pair IS the context switch.
public class CpuContext {

    final int a;
    final int[] registers;   // R0-R7
    final int pc;
    final boolean cy;
    final boolean ov;
    final boolean running;
    final ArrayList<Instruction> program;
    final DataMemory dataMemory;
    final StackMemory stack;
    final QueueMemory queue;

    CpuContext(int a, int[] registers, int pc, boolean cy, boolean ov, boolean running,
               ArrayList<Instruction> program, DataMemory dataMemory,
               StackMemory stack, QueueMemory queue) {
        this.a = a;
        this.registers = registers;
        this.pc = pc;
        this.cy = cy;
        this.ov = ov;
        this.running = running;
        this.program = program;
        this.dataMemory = dataMemory;
        this.stack = stack;
        this.queue = queue;
    }

    // Initial context for a brand-new process: zeroed registers, PC = 0,
    // and its own empty data memory, stack and queue.
    public static CpuContext fresh(ArrayList<Instruction> program) {
        return new CpuContext(0, new int[8], 0, false, false, !program.isEmpty(),
                program, new DataMemory(), new StackMemory(), new QueueMemory());
    }
}

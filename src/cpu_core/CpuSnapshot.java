package cpu_core;

import java.util.List;

// A frozen snapshot of everything the GUI needs to display, taken at
// one point in time. GUI panels depend on THIS class, never on CPU,
// StackMemory, or QueueMemory directly. If CPU's internals change
// (how registers are stored, how the stack/queue work internally),
// nothing in the gui package needs to change, as long as CPU.getSnapshot()
// keeps producing a CpuSnapshot with these same fields. Only adding a
// genuinely new piece of state to display touches both sides.
public class CpuSnapshot {

    public final int a;
    public final int[] registers; // R0-R7
    public final int pc;
    public final boolean cy;
    public final boolean ov;
    public final boolean running;

    public final int sp;
    public final List<Integer> stackContents; // bottom -> top
    public final boolean stackEmpty;
    public final boolean stackFull;

    public final List<Integer> queueContents; // front -> rear
    public final int queueCount;
    public final int queueCapacity;
    public final boolean queueEmpty;
    public final boolean queueFull;

    // Non-zero data memory contents, as [address, value] pairs.
    public final List<int[]> nonZeroMemory;

    public CpuSnapshot(int a, int[] registers, int pc, boolean cy, boolean ov, boolean running,
                        int sp, List<Integer> stackContents, boolean stackEmpty, boolean stackFull,
                        List<Integer> queueContents, int queueCount, int queueCapacity,
                        boolean queueEmpty, boolean queueFull, List<int[]> nonZeroMemory) {
        this.a = a;
        this.registers = registers;
        this.pc = pc;
        this.cy = cy;
        this.ov = ov;
        this.running = running;
        this.sp = sp;
        this.stackContents = stackContents;
        this.stackEmpty = stackEmpty;
        this.stackFull = stackFull;
        this.queueContents = queueContents;
        this.queueCount = queueCount;
        this.queueCapacity = queueCapacity;
        this.queueEmpty = queueEmpty;
        this.queueFull = queueFull;
        this.nonZeroMemory = nonZeroMemory;
    }
}
package cpu_core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Test cases for the CPU's fetch/decode/execute cycle.
// TC01-TC09: core instructions (MOV_A_DATA, MOV_RN_DATA, ADD, SUBB,
// ANL, INC, SJMP, HALT) plus the full demonstration program.
// TC10-TC15: Week 3 Stack (PUSH/POP) and FIFO Queue (ENQUEUE/DEQUEUE).
//
// Note: this microcontroller's instruction set has no MOV_DIRECT_A
// (data-transfer is MOV_A_DATA and MOV_RN_DATA only), so DataMemory
// read/write is tested directly via CPU.readDataMemory()/writeDataMemory()
// (see TC-MEM below) rather than through an instruction.
public class CPUTest {

    private static int total = 0;
    private static int passed = 0;

    public static void main(String[] args) {
        System.out.println("Test    Instruction        Expected            Actual              Status");
        System.out.println("--------------------------------------------------------------------------");

        testMovAData();
        testMovRnData();
        testAdd();
        testSubb();
        testAnl();
        testInc();
        testSjmp();
        testHalt();
        testStackResetOnReset();
        testDemoProgram();

        testDataMemoryReadWrite();
        testPushPop();
        testStackUnderflow();
        testQueueFifoOrder();
        testQueueEmptyCondition();
        testQueueFullCondition();

        System.out.println("--------------------------------------------------------------------------");
        System.out.println(passed + " / " + total + " test cases passed.");
    }

    private static void check(String id, String instruction, String expected, Object actual) {
        total++;
        boolean ok = expected.equals(String.valueOf(actual));
        if (ok) passed++;
        System.out.printf("%-7s %-18s %-20s %-20s %s%n",
                id, instruction, expected, actual, ok ? "PASS" : "FAIL");
    }

    // TC01: MOV A, #10  -> A = 10
    private static void testMovAData() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.step();
        check("TC01", "MOV A, #10", "10", cpu.getA());
    }

    // TC02: MOV R1, #7  -> R1 = 7
    private static void testMovRnData() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_RN_DATA", Arrays.asList("R1", "7")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.step();
        check("TC02", "MOV R1, #7", "7", cpu.getR(1));
    }

    // TC03: ADD A, R1  -> A=5, R1=3 => A=8
    private static void testAdd() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("5")));
        program.add(new Instruction("ADD", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.setR(1, 3);
        cpu.step();
        cpu.step();
        check("TC03", "ADD A, R1", "8", cpu.getA());
    }

    // TC04: SUBB A, R1  -> A=10, R1=4, CY=0 => A=6
    private static void testSubb() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("SUBB", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.setR(1, 4);
        cpu.step();
        cpu.step();
        check("TC04", "SUBB A, R1", "6", cpu.getA());
    }

    // TC05: ANL A, R1  -> A=12, R1=10 => A=8
    private static void testAnl() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("12")));
        program.add(new Instruction("ANL", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.setR(1, 10);
        cpu.step();
        cpu.step();
        check("TC05", "ANL A, R1", "8", cpu.getA());
    }

    // TC06: INC R1  -> R1=5 => R1=6
    private static void testInc() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("INC", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.setR(1, 5);
        cpu.step();
        check("TC06", "INC R1", "6", cpu.getR(1));
    }

    // TC07: SJMP rel  -> PC after fetching SJMP at index 0 is 1, offset +2 => PC=3
    private static void testSjmp() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("SJMP", Arrays.asList("2")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        program.add(new Instruction("HALT", Collections.emptyList()));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.step();
        check("TC07", "SJMP rel", "3", cpu.getPC());
    }

    // TC08: HALT  -> running becomes false
    private static void testHalt() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.step();
        check("TC08", "HALT", "false", cpu.isRunning());
    }

    // TC09: reset() puts the stack pointer back to its 8051 power-on value (0x07)
    private static void testStackResetOnReset() {
        CPU cpu = new CPU();
        cpu.reset();
        check("TC09", "reset() -> SP", "7", cpu.getSP());
    }

    // TC10: Full demonstration program (MOV_A_DATA / MOV_RN_DATA / ADD / SUBB / ANL / INC / HALT)
    private static void testDemoProgram() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("MOV_RN_DATA", Arrays.asList("R1", "3")));
        program.add(new Instruction("ADD", Arrays.asList("R1")));     // A = 13
        program.add(new Instruction("SUBB", Arrays.asList("R1")));    // A = 10
        program.add(new Instruction("ANL", Arrays.asList("R1")));     // A = 2
        program.add(new Instruction("INC", Arrays.asList("R1")));     // R1 = 4
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.run();
        check("TC10a", "Demo: final A", "2", cpu.getA());
        check("TC10b", "Demo: final R1", "4", cpu.getR(1));
        check("TC10c", "Demo: running", "false", cpu.isRunning());
    }

    // TC-MEM: DataMemory read/write via CPU (no assembly instruction reaches it)
    private static void testDataMemoryReadWrite() {
        CPU cpu = new CPU();
        cpu.writeDataMemory(10, 50);
        check("TC-MEM", "writeDataMemory(10,50)", "50", cpu.readDataMemory(10));
    }

    // TC11: PUSH A then POP A -> A survives being overwritten in between
    private static void testPushPop() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("13")));
        program.add(new Instruction("PUSH", Arrays.asList("A")));
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("99")));
        program.add(new Instruction("POP", Arrays.asList("A")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.run();
        check("TC11a", "PUSH/POP: final A", "13", cpu.getA());
        check("TC11b", "PUSH/POP: SP after", "7", cpu.getSP());
    }

    // TC12: POP with nothing pushed -> stack underflow exception
    private static void testStackUnderflow() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("POP", Arrays.asList("A")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        boolean threw = false;
        try {
            cpu.step();
        } catch (IllegalStateException e) {
            threw = true;
        }
        check("TC12", "POP on empty stack", "true", threw);
    }

    // TC13: ENQUEUE #5, ENQUEUE #7, DEQUEUE R0, DEQUEUE R1 -> FIFO order preserved (5 then 7)
    private static void testQueueFifoOrder() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("ENQUEUE", Arrays.asList("#5")));
        program.add(new Instruction("ENQUEUE", Arrays.asList("#7")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R0")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.run();
        check("TC13a", "Queue FIFO: R0", "5", cpu.getR(0));
        check("TC13b", "Queue FIFO: R1", "7", cpu.getR(1));
    }

    // TC14: DEQUEUE with nothing enqueued -> empty-queue exception
    private static void testQueueEmptyCondition() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("DEQUEUE", Arrays.asList("R0")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        boolean threw = false;
        try {
            cpu.step();
        } catch (IllegalStateException e) {
            threw = true;
        }
        check("TC14", "DEQUEUE on empty queue", "true", threw);
    }

    // TC15: Fill the queue to capacity, then one more ENQUEUE -> full-queue exception
    private static void testQueueFullCondition() {
        CPU cpu = new CPU();
        int capacity = cpu.getQueue().getCapacity();
        for (int i = 0; i < capacity; i++) {
            cpu.getQueue().enqueue(i);
        }
        boolean threw = false;
        try {
            cpu.getQueue().enqueue(99);
        } catch (IllegalStateException e) {
            threw = true;
        }
        check("TC15", "ENQUEUE on full queue", "true", threw);
    }
}
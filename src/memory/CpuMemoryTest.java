package memory;

import cpu_core.CPU;
import cpu_core.Instruction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// CPU + Memory integration tests.
// Note: this microcontroller's instruction set has no MOV_DIRECT_A
// (data-transfer is MOV_A_DATA and MOV_RN_DATA only, per the
// professor's guidance), so there is no assembly instruction that
// writes to DataMemory. Data memory read/write is exercised directly
// through CPU's plain readDataMemory()/writeDataMemory() methods
// instead. Stack and Queue ARE reachable through instructions
// (PUSH/POP, ENQUEUE/DEQUEUE), so those are tested via a loaded
// program and cpu.step(), matching the original test's style.
public class CpuMemoryTest {

    public static void main(String[] args) {

        // ===== Test 1: CPU <-> Data Memory (direct read/write) =====

        CPU cpu = new CPU();

        cpu.writeDataMemory(10, 50);

        if (cpu.readDataMemory(10) == 50) {
            System.out.println("CPU-Data Memory Test: PASS");
        } else {
            System.out.println("CPU-Data Memory Test: FAIL");
        }

        // ===== Test 2: CPU <-> Stack (PUSH/POP through an instruction program) =====

        CPU stackCpu = new CPU();

        ArrayList<Instruction> stackProgram = new ArrayList<>();

        // Put 77 into accumulator
        stackProgram.add(new Instruction(
            "MOV_A_DATA",
            Arrays.asList("77")
        ));

        // Push accumulator onto the stack
        stackProgram.add(new Instruction(
            "PUSH",
            Arrays.asList("A")
        ));

        // Overwrite accumulator so POP's result is unambiguous
        stackProgram.add(new Instruction(
            "MOV_A_DATA",
            Arrays.asList("0")
        ));

        // Pop back into the accumulator
        stackProgram.add(new Instruction(
            "POP",
            Arrays.asList("A")
        ));

        stackProgram.add(new Instruction(
            "HALT",
            Collections.emptyList()
        ));

        stackCpu.loadProgram(stackProgram);
        stackCpu.run();

        if (stackCpu.getA() == 77 && stackCpu.getSP() == 0x07) {
            System.out.println("CPU-Stack Memory Test: PASS");
        } else {
            System.out.println("CPU-Stack Memory Test: FAIL");
        }

        // ===== Test 3: CPU <-> Queue (ENQUEUE/DEQUEUE through an instruction program) =====

        CPU queueCpu = new CPU();

        ArrayList<Instruction> queueProgram = new ArrayList<>();

        // Enqueue two immediate values
        queueProgram.add(new Instruction(
            "ENQUEUE",
            Arrays.asList("#11")
        ));

        queueProgram.add(new Instruction(
            "ENQUEUE",
            Arrays.asList("#22")
        ));

        // Dequeue both into registers, in FIFO order
        queueProgram.add(new Instruction(
            "DEQUEUE",
            Arrays.asList("R0")
        ));

        queueProgram.add(new Instruction(
            "DEQUEUE",
            Arrays.asList("R1")
        ));

        queueProgram.add(new Instruction(
            "HALT",
            Collections.emptyList()
        ));

        queueCpu.loadProgram(queueProgram);
        queueCpu.run();

        if (queueCpu.getR(0) == 11 && queueCpu.getR(1) == 22) {
            System.out.println("CPU-Queue Memory Test: PASS");
        } else {
            System.out.println("CPU-Queue Memory Test: FAIL");
        }
    }
}
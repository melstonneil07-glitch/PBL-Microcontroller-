package memory;

import cpu_core.CPU;
import cpu_core.Instruction;
import java.util.ArrayList;
import java.util.Arrays;

public class CpuMemoryTest {

    public static void main(String[] args) {

        CPU cpu = new CPU();

        ArrayList<Instruction> program = new ArrayList<>();

        // Put 50 into accumulator
        program.add(new Instruction(
            "MOV_A_DATA",
            Arrays.asList("50")
        ));

        // Store accumulator value into Data Memory address 10
        program.add(new Instruction(
            "MOV_DIRECT_A",
            Arrays.asList("10")
        ));

        // Stop the program
        program.add(new Instruction(
            "HALT",
            Arrays.asList()
        ));

        // Load program into CPU
        cpu.loadProgram(program);

        // Execute first instruction
        cpu.step();

        // Execute second instruction
        cpu.step();

        // Check Data Memory
        if (cpu.readDataMemory(10) == 50) {
            System.out.println("CPU-Data Memory Test: PASS");
        } else {
            System.out.println("CPU-Data Memory Test: FAIL");
        }
    }
}

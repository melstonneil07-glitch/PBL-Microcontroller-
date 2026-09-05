package cpu_core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Demonstrates the FETCH -> DECODE -> EXECUTE cycle using the demo
// program from PBL.md:
//   MOV A, #10
//   MOV 30H, A
//   ADD A, R1
//   INC R1
//   HALT
public class CPUDemo {

    public static void main(String[] args) {
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("MOV_DIRECT_A", Arrays.asList("48"))); 
        program.add(new Instruction("ADD", Arrays.asList("R1")));
        program.add(new Instruction("INC", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));

        CPU cpu = new CPU();
        cpu.loadProgram(program);
        cpu.setR(1, 3); 

        System.out.println("Starting FETCH -> DECODE -> EXECUTE trace\n");

        while (cpu.isRunning()) {
            int beforeA = cpu.getA();
            int beforePC = cpu.getPC();

            Instruction fetched = cpu.fetch();
            System.out.println("FETCH  \u2713  " + fetched + "  @PC=" + beforePC);

            Instruction decoded = cpu.decode(fetched);
            System.out.println("DECODE \u2713  recognized instruction");

            cpu.execute(decoded);
            System.out.println("EXECUTE\u2713");

            System.out.println("Result: A " + beforeA + " -> " + cpu.getA()
                                + " | PC " + beforePC + " -> " + cpu.getPC()
                                + " | CY=" + cpu.isCY() + " OV=" + cpu.isOV());
            System.out.println("--------------------------------------------------");
        }

        System.out.println("\nProgram halted.");
        System.out.println("Final A = " + cpu.getA());
        System.out.println("Final R1 = " + cpu.getR(1));
        System.out.println("DataMemory[0x30] = " + cpu.readDataMemory(48));
    }
}
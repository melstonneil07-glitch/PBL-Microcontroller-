package cpu_core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Demonstrates the FETCH -> DECODE -> EXECUTE cycle across all 12
// implemented instructions, including the Week 3 Stack and FIFO
// Queue additions:
//   MOV A, #10
//   MOV R1, #3
//   ADD A, R1        -> A = 13
//   PUSH A           -> stack: [13]
//   MOV A, #99
//   POP A            -> A restored to 13
//   ENQUEUE A        -> queue: [13]
//   ENQUEUE #20      -> queue: [13, 20]
//   DEQUEUE R2       -> R2 = 13, queue: [20]
//   SUBB A, R1       -> A = 10
//   ANL A, R1        -> A = 2
//   INC R1           -> R1 = 4
//   HALT
public class CPUDemo {

    public static void main(String[] args) {
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("MOV_RN_DATA", Arrays.asList("R1", "3")));
        program.add(new Instruction("ADD", Arrays.asList("R1")));
        program.add(new Instruction("PUSH", Arrays.asList("A")));
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("99")));
        program.add(new Instruction("POP", Arrays.asList("A")));
        program.add(new Instruction("ENQUEUE", Arrays.asList("A")));
        program.add(new Instruction("ENQUEUE", Arrays.asList("#20")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R2")));
        program.add(new Instruction("SUBB", Arrays.asList("R1")));
        program.add(new Instruction("ANL", Arrays.asList("R1")));
        program.add(new Instruction("INC", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));

        CPU cpu = new CPU();
        cpu.loadProgram(program);

        System.out.println("Starting FETCH -> DECODE -> EXECUTE trace\n");

        while (cpu.isRunning()) {
            int beforeA = cpu.getA();
            int beforePC = cpu.getPC();

            Instruction fetched = cpu.fetch();
            if (fetched == null) {
                break;
            }
            System.out.println("FETCH  \u2713  " + fetched + "  @PC=" + beforePC);

            Instruction decoded = cpu.decode(fetched);
            String category = cpu.getInstructionCategory(decoded.mnemonic);
            System.out.println("DECODE \u2713  recognized instruction  [" + category + "]");

            cpu.execute(decoded);
            System.out.println("EXECUTE\u2713");

            System.out.println("Result: A " + beforeA + " -> " + cpu.getA()
                                + " | PC " + beforePC + " -> " + cpu.getPC()
                                + " | SP=0x" + Integer.toHexString(cpu.getSP())
                                + " | Queue=" + cpu.getQueue().snapshot());
            System.out.println("--------------------------------------------------");
        }

        System.out.println("\nProgram halted.");
        System.out.println("Final A = " + cpu.getA());
        System.out.println("Final R1 = " + cpu.getR(1));
        System.out.println("Final R2 (from DEQUEUE) = " + cpu.getR(2));
        System.out.println("Final Stack contents (bottom->top) = " + cpu.getStack().snapshot());
        System.out.println("Final Queue contents (front->rear) = " + cpu.getQueue().snapshot());
    }
}
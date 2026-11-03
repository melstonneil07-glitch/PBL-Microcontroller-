package cpu_core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Week 3 deliverable 5: Assembly Validation for the FIFO Queue.
//
// This is a processor-specific Assembly program (expressed as
// Instruction objects, exactly like every other program in this
// simulator) that is loaded and executed through the CPU class --
// the same FETCH -> DECODE -> EXECUTE pipeline used everywhere else
// -- to validate the Queue implementation. It demonstrates:
//   - Multiple ENQUEUE operations (4 values pushed onto the queue)
//   - Multiple DEQUEUE operations (4 values pulled back off)
//   - Correct FIFO ordering (values come back out in the same order
//     they went in: 10, 20, 30, 40)
//   - Expected vs actual results, printed as a PASS/FAIL record
public class QueueValidation {

    public static void main(String[] args) {

        // Assembly program:
        //   ENQUEUE #10
        //   ENQUEUE #20
        //   ENQUEUE #30
        //   ENQUEUE #40
        //   DEQUEUE R0     ; expect 10 (first in, first out)
        //   DEQUEUE R1     ; expect 20
        //   DEQUEUE R2     ; expect 30
        //   DEQUEUE R3     ; expect 40
        //   HALT
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("ENQUEUE", Arrays.asList("#10")));
        program.add(new Instruction("ENQUEUE", Arrays.asList("#20")));
        program.add(new Instruction("ENQUEUE", Arrays.asList("#30")));
        program.add(new Instruction("ENQUEUE", Arrays.asList("#40")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R0")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R1")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R2")));
        program.add(new Instruction("DEQUEUE", Arrays.asList("R3")));
        program.add(new Instruction("HALT", Collections.emptyList()));

        int[] expected = {10, 20, 30, 40};

        CPU cpu = new CPU();
        cpu.loadProgram(program);

        System.out.println("Queue Validation Program");
        System.out.println("=========================\n");

        int step = 1;
        while (cpu.isRunning()) {
            int beforePC = cpu.getPC();
            Instruction fetched = cpu.fetch();
            if (fetched == null) break;
            Instruction decoded = cpu.decode(fetched);
            cpu.execute(decoded);

            System.out.printf("Step %2d  PC=%04XH  %-16s  Queue=%s%n",
                    step, beforePC, decoded, cpu.getQueue().snapshot());
            step++;
        }

        System.out.println("\nExpected vs Actual (FIFO order):");
        System.out.println("---------------------------------");
        System.out.printf("%-10s %-10s %-10s %s%n", "Dequeue#", "Expected", "Actual", "Status");

        boolean allPass = true;
        for (int i = 0; i < 4; i++) {
            int actual = cpu.getR(i);
            boolean ok = actual == expected[i];
            allPass &= ok;
            System.out.printf("%-10d %-10d %-10d %s%n",
                    i + 1, expected[i], actual, ok ? "PASS" : "FAIL");
        }

        System.out.println("\nFinal queue state: " + cpu.getQueue().snapshot()
                + " (empty=" + cpu.getQueue().isEmpty() + ")");

        System.out.println("\nOverall FIFO Validation: " + (allPass ? "PASS" : "FAIL"));
    }
}
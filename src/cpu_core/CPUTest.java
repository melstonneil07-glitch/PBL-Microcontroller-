package cpu_core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

// Test cases for the CPU's fetch/decode/execute cycle.
// One test case per selected instruction, plus one for the full
// demonstration program, printed as a simple PASS/FAIL test record
// (matching the format required in PBL.md).
public class CPUTest {

    private static int total = 0;
    private static int passed = 0;

    public static void main(String[] args) {
        System.out.println("Test   Instruction        Expected            Actual              Status");
        System.out.println("--------------------------------------------------------------------------");

        testMovAData();
        testMovDirectA();
        testAdd();
        testSubb();
        testAnl();
        testInc();
        testSjmp();
        testHalt();
        testStackResetOnReset();
        testDemoProgram();

        System.out.println("--------------------------------------------------------------------------");
        System.out.println(passed + " / " + total + " test cases passed.");
    }

    private static void check(String id, String instruction, String expected, Object actual) {
        total++;
        boolean ok = expected.equals(String.valueOf(actual));
        if (ok) passed++;
        System.out.printf("%-6s %-18s %-20s %-20s %s%n",
                id, instruction, expected, actual, ok ? "PASS" : "FAIL");
    }

    // TC01: MOV A, #10  -> A = 10
    private static void testMovAData() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);

        cpu.step(); // MOV A, #10
        check("TC01", "MOV A, #10", "10", cpu.getA());
    }

    // TC02: MOV 30H, A  -> DataMemory[48] = A (A preset to 25)
    private static void testMovDirectA() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("25")));
        program.add(new Instruction("MOV_DIRECT_A", Arrays.asList("48"))); // 0x30
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);

        cpu.step(); // MOV A, #25
        cpu.step(); // MOV 30H, A
        check("TC02", "MOV 30H, A", "25", cpu.readDataMemory(48));
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

        cpu.step(); // MOV A, #5
        cpu.step(); // ADD A, R1
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

        cpu.step(); // MOV A, #10
        cpu.step(); // SUBB A, R1
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

        cpu.step(); // MOV A, #12
        cpu.step(); // ANL A, R1
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

        cpu.step(); // INC R1
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

        cpu.step(); // SJMP +2
        check("TC07", "SJMP rel", "3", cpu.getPC());
    }

    // TC08: HALT  -> running becomes false
    private static void testHalt() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);

        cpu.step(); // HALT
        check("TC08", "HALT", "false", cpu.isRunning());
    }

    // TC09: reset() puts the stack pointer back to its 8051 power-on value (0x07)
    private static void testStackResetOnReset() {
        CPU cpu = new CPU();
        cpu.reset();
        check("TC09", "reset() -> SP", "7", cpu.getSP());
    }

    // TC10: Full demonstration program from PBL.md
    // MOV A,#10 / MOV 30H,A / ADD A,R1 / INC R1 / HALT   (R1 preset to 3)
    private static void testDemoProgram() {
        CPU cpu = new CPU();
        ArrayList<Instruction> program = new ArrayList<>();
        program.add(new Instruction("MOV_A_DATA", Arrays.asList("10")));
        program.add(new Instruction("MOV_DIRECT_A", Arrays.asList("48")));
        program.add(new Instruction("ADD", Arrays.asList("R1")));
        program.add(new Instruction("INC", Arrays.asList("R1")));
        program.add(new Instruction("HALT", Collections.emptyList()));
        cpu.loadProgram(program);
        cpu.setR(1, 3);

        cpu.run(); // repeatedly fetch/decode/execute until HALT

        check("TC10a", "Demo: final A", "13", cpu.getA());
        check("TC10b", "Demo: final R1", "4", cpu.getR(1));
        check("TC10c", "Demo: DataMem[48]", "10", cpu.readDataMemory(48));
        check("TC10d", "Demo: running", "false", cpu.isRunning());
    }
}
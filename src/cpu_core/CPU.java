package cpu_core;
import java.util.ArrayList;
import java.util.HashMap;
import memory.DataMemory;
import memory.StackMemory;

// Simplified CPU for the MS51FB9AE (8051-based) emulator.
// Implements the logical FETCH -> DECODE -> EXECUTE cycle for the
// 8 selected instructions from PBL.md, using the dedicated memory
// classes from the `memory` package, plus plain Java data
// structures where those classes don't apply:
//   - ArrayList    : registers, program memory (decoded instructions)
//   - DataMemory   : 256-byte internal RAM (data memory)
//   - StackMemory  : processor stack (8051-style, SP starts at 0x07)
//   - HashMap      : instruction/opcode lookup
//
// Note: ProgramMemory (memory package) models raw byte storage for
// machine code, but this CPU's `step()` operates on already-decoded
// Instruction objects (mnemonic + operands), not raw bytes, so
// program memory here stays an ArrayList<Instruction> rather than a
// ProgramMemory instance. If/when a real byte-level encoder/decoder
// is added, ProgramMemory can replace it.
public class CPU {

    // --- Registers ---
    private int a = 0; // AccumulatorsA
    private final ArrayList<Integer> r = new ArrayList<>(); // R0-R7

    // --- Program Counter ---
    private int pc = 0;

    // --- Flags ---
    private boolean cy = false; // Carry
    private boolean ov = false; // Overflow

    // --- Execution state ---
    private boolean running = false;

    // --- Memory ---
    private ArrayList<Instruction> programMemory = new ArrayList<>(); // decoded instructions
    private final DataMemory dataMemory = new DataMemory();           // 256 bytes of RAM
    private final StackMemory stack = new StackMemory();               // processor stack

    // --- Instruction lookup: mnemonic -> functional category ---
    private final HashMap<String, String> instructionTable = new HashMap<>();

    public CPU() {
        instructionTable.put("MOV_A_DATA", "Data Transfer");
        instructionTable.put("MOV_DIRECT_A", "Data Transfer");
        instructionTable.put("ADD", "Arithmetic");
        instructionTable.put("SUBB", "Arithmetic");
        instructionTable.put("ANL", "Logical Operation");
        instructionTable.put("INC", "Increment / Decrement");
        instructionTable.put("SJMP", "Control Flow");
        instructionTable.put("HALT", "Program Termination");

        for (int i = 0; i < 8; i++) r.add(0);       // R0-R7
        // dataMemory (DataMemory) already allocates its own 256-byte array
    }

    // Loads a program (a list of instructions) and resets the CPU.
    public void loadProgram(ArrayList<Instruction> program) {
        this.programMemory = program;
        reset();
    }

    // Resets CPU state to power-on values.
    public void reset() {
        pc = 0;
        a = 0;
        for (int i = 0; i < r.size(); i++) r.set(i, 0);
        cy = false;
        ov = false;
        stack.reset();
        running = true;
    }

    /* ================= FETCH =================
       Uses the Program Counter to get the next instruction from
       program memory, then advances the PC. */
    public Instruction fetch() {
        Instruction instr = programMemory.get(pc);
        pc = pc + 1;
        return instr;
    }

    /* ================= DECODE =================
       Looks the instruction up in the instruction table to confirm it
       is a recognized operation before executing it. */
    public Instruction decode(Instruction instr) {
        if (!instructionTable.containsKey(instr.mnemonic)) {
            throw new IllegalStateException("Unknown instruction: " + instr.mnemonic);
        }
        return instr;
    }

    /* ================= EXECUTE =================
       Performs the instruction's operation, updating the accumulator,
       registers, data memory, flags, and/or PC as required. */
    public void execute(Instruction instr) {
        switch (instr.mnemonic) {

            case "MOV_A_DATA":
                a = Integer.parseInt(instr.operands.get(0)) & 0xFF;
                break;

            case "MOV_DIRECT_A":
                int addr = Integer.parseInt(instr.operands.get(0));
                dataMemory.write(addr, (byte) a);
                break;

            case "ADD": {
                int n = regIndex(instr.operands.get(0));
                int operand = r.get(n);
                int result = a + operand;
                cy = result > 0xFF;
                boolean signA = (a & 0x80) != 0;
                boolean signOperand = (operand & 0x80) != 0;
                boolean signResult = ((result & 0xFF) & 0x80) != 0;
                ov = (signA == signOperand) && (signResult != signA);
                a = result & 0xFF;
                break;
            }

            case "SUBB": {
                int n = regIndex(instr.operands.get(0));
                int operand = r.get(n);
                int carryIn = cy ? 1 : 0;
                int result = a - operand - carryIn;
                cy = result < 0;
                a = result & 0xFF;
                break;
            }

            case "ANL": {
                int n = regIndex(instr.operands.get(0));
                a = a & r.get(n);
                break;
            }

            case "INC": {
                int n = regIndex(instr.operands.get(0));
                r.set(n, (r.get(n) + 1) & 0xFF);
                break;
            }

            case "SJMP": {
                int offset = Integer.parseInt(instr.operands.get(0));
                pc = pc + offset;
                break;
            }

            case "HALT":
                running = false;
                break;

            default:
                throw new IllegalStateException("Unhandled instruction: " + instr.mnemonic);
        }
    }

    // Converts "R1" -> 1
    private int regIndex(String token) {
        return Integer.parseInt(token.substring(1));
    }

    // Runs one full FETCH -> DECODE -> EXECUTE cycle for one instruction.
    public Instruction step() {
        if (!running) return null;
        Instruction fetched = fetch();
        Instruction decoded = decode(fetched);
        execute(decoded);
        return decoded;
    }

    // Runs instructions repeatedly until HALT.
    public void run() {
        while (running) {
            step();
        }
    }

    // --- State accessors ---
    public int getA() { return a; }
    public int getR(int n) { return r.get(n); }
    public void setR(int n, int value) { r.set(n, value & 0xFF); }
    public int getPC() { return pc; }
    public boolean isCY() { return cy; }
    public boolean isOV() { return ov; }
    public boolean isRunning() { return running; }

    // Returned as an unsigned 0-255 value, since DataMemory stores signed bytes.
    public int readDataMemory(int address) { return dataMemory.read(address) & 0xFF; }

    public StackMemory getStack() { return stack; }
    public int getSP() { return stack.getSP(); }
}
package cpu_core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import memory.DataMemory;
import memory.StackMemory;
import memory.QueueMemory;

// CPU for the MS51FB9AE (8051-based) emulator.
// Data-transfer instructions are MOV_A_DATA and MOV_RN_DATA only
// (per professor's guidance -- this microcontroller's instruction
// set does not include a MOV_DIRECT_A style direct-address move).
// Week 3 adds Stack (PUSH/POP) and FIFO Queue (ENQUEUE/DEQUEUE).
public class CPU {

    // Registers
    private int a = 0;
    private final ArrayList<Integer> r = new ArrayList<>();

    // Program Counter
    private int pc = 0;

    // Flags
    private boolean cy = false;
    private boolean ov = false;

    // Execution state
    private boolean running = false;

    // Memory
    private ArrayList<Instruction> programMemory = new ArrayList<>();
    private final DataMemory dataMemory = new DataMemory();
    private final StackMemory stack = new StackMemory();
    private final QueueMemory queue = new QueueMemory();

    // Instruction table
    private final HashMap<String, String> instructionTable = new HashMap<>();

    public CPU() {

        instructionTable.put("MOV_A_DATA", "Data Transfer");
        instructionTable.put("MOV_RN_DATA", "Data Transfer");
        instructionTable.put("ADD", "Arithmetic");
        instructionTable.put("SUBB", "Arithmetic");
        instructionTable.put("ANL", "Logical Operation");
        instructionTable.put("INC", "Increment / Decrement");
        instructionTable.put("SJMP", "Control Flow");
        instructionTable.put("HALT", "Program Termination");
        instructionTable.put("PUSH", "Stack");
        instructionTable.put("POP", "Stack");
        instructionTable.put("ENQUEUE", "Queue");
        instructionTable.put("DEQUEUE", "Queue");

        // R0 - R7
        for (int i = 0; i < 8; i++) {
            r.add(0);
        }
    }


    // ================= LOAD PROGRAM =================

    public void loadProgram(ArrayList<Instruction> program) {
        this.programMemory = program;
        reset();
    }


    // ================= RESET =================

    public void reset() {

        pc = 0;
        a = 0;

        for (int i = 0; i < r.size(); i++) {
            r.set(i, 0);
        }

        cy = false;
        ov = false;

        stack.reset();
        queue.reset();
        dataMemory.clear();

        running = !programMemory.isEmpty();
    }


    // ================= FETCH =================

    public Instruction fetch() {

        if (pc < 0 || pc >= programMemory.size()) {
            running = false;
            return null;
        }

        Instruction instr = programMemory.get(pc);
        pc = pc + 1;

        return instr;
    }


    // ================= DECODE =================

    public Instruction decode(Instruction instr) {

        if (instr == null) {
            return null;
        }

        if (!instructionTable.containsKey(instr.mnemonic)) {
            throw new IllegalStateException(
                "Unknown instruction: " + instr.mnemonic
            );
        }

        return instr;
    }


    // ================= EXECUTE =================

    public void execute(Instruction instr) {

        if (instr == null) {
            return;
        }

        switch (instr.mnemonic) {

            // MOV A,#data
            case "MOV_A_DATA": {
                int value = Integer.parseInt(instr.operands.get(0));
                a = value & 0xFF;
                break;
            }

            // MOV Rn,#data
            case "MOV_RN_DATA": {
                String register = instr.operands.get(0);
                int registerNumber = regIndex(register);
                int value = Integer.parseInt(instr.operands.get(1));
                r.set(registerNumber, value & 0xFF);
                break;
            }

            // ADD A,Rn
            case "ADD": {
                int n = regIndex(instr.operands.get(0));
                int operand = r.get(n);
                int oldA = a;
                int result = oldA + operand;
                cy = result > 0xFF;
                boolean signA = (oldA & 0x80) != 0;
                boolean signOperand = (operand & 0x80) != 0;
                boolean signResult = ((result & 0xFF) & 0x80) != 0;
                ov = (signA == signOperand) && (signResult != signA);
                a = result & 0xFF;
                break;
            }

            // SUBB A,Rn
            case "SUBB": {
                int n = regIndex(instr.operands.get(0));
                int operand = r.get(n);
                int carryIn = cy ? 1 : 0;
                int oldA = a;
                int result = oldA - operand - carryIn;
                cy = result < 0;
                int finalResult = result & 0xFF;
                boolean signA = (oldA & 0x80) != 0;
                boolean signOperand = (operand & 0x80) != 0;
                boolean signResult = (finalResult & 0x80) != 0;
                ov = (signA != signOperand) && (signResult != signA);
                a = finalResult;
                break;
            }

            // ANL A,Rn
            case "ANL": {
                int n = regIndex(instr.operands.get(0));
                a = a & r.get(n);
                break;
            }

            // INC Rn
            case "INC": {
                int n = regIndex(instr.operands.get(0));
                r.set(n, (r.get(n) + 1) & 0xFF);
                break;
            }

            // SJMP relative
            case "SJMP": {
                int offset = Integer.parseInt(instr.operands.get(0));
                pc = pc + offset;
                break;
            }

            // PUSH A  or  PUSH Rn
            case "PUSH": {
                int value = resolveByteOperand(instr.operands.get(0));
                stack.push((byte) value);
                break;
            }

            // POP A  or  POP Rn
            case "POP": {
                int value = stack.pop() & 0xFF;
                writeByteOperand(instr.operands.get(0), value);
                break;
            }

            // ENQUEUE A  or  ENQUEUE Rn  or  ENQUEUE #data
            case "ENQUEUE": {
                int value = resolveByteOperand(instr.operands.get(0));
                queue.enqueue(value);
                break;
            }

            // DEQUEUE A  or  DEQUEUE Rn
            case "DEQUEUE": {
                int value = queue.dequeue();
                writeByteOperand(instr.operands.get(0), value);
                break;
            }

            // Simulator termination
            case "HALT": {
                running = false;
                break;
            }

            default:
                throw new IllegalStateException(
                    "Unhandled instruction: " + instr.mnemonic
                );
        }
    }

    // Resolves an operand token to a byte value: "A" -> accumulator,
    // "Rn" -> that register, "#data" -> the immediate value itself.
    private int resolveByteOperand(String token) {
        if (token.equals("A")) {
            return a & 0xFF;
        }
        if (token.startsWith("#")) {
            return Integer.parseInt(token.substring(1)) & 0xFF;
        }
        return r.get(regIndex(token)) & 0xFF;
    }

    // Writes a byte value into the destination named by an operand
    // token: "A" -> accumulator, "Rn" -> that register.
    private void writeByteOperand(String token, int value) {
        if (token.equals("A")) {
            a = value & 0xFF;
        } else {
            r.set(regIndex(token), value & 0xFF);
        }
    }

    // Converts R1 -> 1
    private int regIndex(String token) {

        int index = Integer.parseInt(token.substring(1));

        if (index < 0 || index > 7) {
            throw new IllegalArgumentException(
                "Invalid register: " + token
            );
        }

        return index;
    }


    // ================= STEP =================

    public Instruction step() {

        if (!running) {
            return null;
        }

        Instruction fetched = fetch();

        if (fetched == null) {
            return null;
        }

        Instruction decoded = decode(fetched);

        execute(decoded);

        return decoded;
    }


    // ================= RUN =================

    public void run() {
        while (running) {
            step();
        }
    }


    // ================= GETTERS =================

    public int getA() {
        return a;
    }

    public int getR(int n) {
        return r.get(n);
    }

    public void setR(int n, int value) {
        r.set(n, value & 0xFF);
    }

    public int getPC() {
        return pc;
    }

    public boolean isCY() {
        return cy;
    }

    public boolean isOV() {
        return ov;
    }

    public boolean isRunning() {
        return running;
    }

    public String getInstructionCategory(String mnemonic) {
        return instructionTable.getOrDefault(mnemonic, "Unknown");
    }

    public int readDataMemory(int address) {
        return dataMemory.read(address) & 0xFF;
    }

    public void writeDataMemory(int address, int value) {
        dataMemory.write(address, (byte) value);
    }

    public StackMemory getStack() {
        return stack;
    }

    public int getSP() {
        return stack.getSP();
    }

    public QueueMemory getQueue() {
        return queue;
    }

    // Builds a frozen snapshot of everything the GUI needs to display.
    // This is the only place that touches StackMemory/QueueMemory/DataMemory
    // directly for display purposes -- GUI panels use the returned
    // CpuSnapshot instead, so they never depend on those classes' APIs.
    public CpuSnapshot getSnapshot() {

        int[] registers = new int[8];
        for (int i = 0; i < 8; i++) {
            registers[i] = r.get(i);
        }

        List<int[]> nonZeroMemory = new ArrayList<>();
        for (int addr = 0; addr < 256; addr++) {
            int value = readDataMemory(addr);
            if (value != 0) {
                nonZeroMemory.add(new int[]{addr, value});
            }
        }

        return new CpuSnapshot(
            a, registers, pc, cy, ov, running,
            stack.getSP(), stack.snapshot(), stack.isEmpty(), stack.isFull(),
            queue.snapshot(), queue.getCount(), queue.getCapacity(),
            queue.isEmpty(), queue.isFull(), nonZeroMemory
        );
    }
}
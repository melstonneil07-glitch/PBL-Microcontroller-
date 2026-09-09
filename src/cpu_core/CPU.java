package cpu_core;

import java.util.ArrayList;
import java.util.HashMap;
import memory.DataMemory;
import memory.StackMemory;

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
    private ArrayList<Instruction> programMemory =
            new ArrayList<>();

    private final DataMemory dataMemory =
            new DataMemory();

    private final StackMemory stack =
            new StackMemory();

    // Instruction table
    private final HashMap<String, String> instructionTable =
            new HashMap<>();


    public CPU() {

        instructionTable.put(
                "MOV_A_DATA",
                "Data Transfer"
        );

        instructionTable.put(
                "MOV_RN_DATA",
                "Data Transfer"
        );

        instructionTable.put(
                "ADD",
                "Arithmetic"
        );

        instructionTable.put(
                "SUBB",
                "Arithmetic"
        );

        instructionTable.put(
                "ANL",
                "Logical Operation"
        );

        instructionTable.put(
                "INC",
                "Increment / Decrement"
        );

        instructionTable.put(
                "SJMP",
                "Control Flow"
        );

        instructionTable.put(
                "HALT",
                "Program Termination"
        );

        // R0 - R7
        for (int i = 0; i < 8; i++) {
            r.add(0);
        }
    }


    // ================= LOAD PROGRAM =================

    public void loadProgram(
            ArrayList<Instruction> program
    ) {

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

        running = !programMemory.isEmpty();
    }


    // ================= FETCH =================

    public Instruction fetch() {

        if (
                pc < 0 ||
                pc >= programMemory.size()
        ) {

            running = false;

            return null;
        }

        Instruction instr =
                programMemory.get(pc);

        pc = pc + 1;

        return instr;
    }


    // ================= DECODE =================

    public Instruction decode(
            Instruction instr
    ) {

        if (instr == null) {
            return null;
        }

        if (
                !instructionTable.containsKey(
                        instr.mnemonic
                )
        ) {

            throw new IllegalStateException(
                    "Unknown instruction: "
                            + instr.mnemonic
            );
        }

        return instr;
    }


    // ================= EXECUTE =================

    public void execute(
            Instruction instr
    ) {

        if (instr == null) {
            return;
        }

        switch (instr.mnemonic) {


            // MOV A,#data
            case "MOV_A_DATA": {

                int value =
                        Integer.parseInt(
                                instr.operands.get(0)
                        );

                a = value & 0xFF;

                break;
            }


            // MOV Rn,#data
            case "MOV_RN_DATA": {

                String register =
                        instr.operands.get(0);

                int registerNumber =
                        regIndex(register);

                int value =
                        Integer.parseInt(
                                instr.operands.get(1)
                        );

                r.set(
                        registerNumber,
                        value & 0xFF
                );

                break;
            }


            // ADD A,Rn
            case "ADD": {

                int n =
                        regIndex(
                                instr.operands.get(0)
                        );

                int operand =
                        r.get(n);

                int oldA =
                        a;

                int result =
                        oldA + operand;

                cy =
                        result > 0xFF;

                boolean signA =
                        (oldA & 0x80) != 0;

                boolean signOperand =
                        (operand & 0x80) != 0;

                boolean signResult =
                        ((result & 0xFF) & 0x80) != 0;

                ov =
                        (signA == signOperand)
                                &&
                        (signResult != signA);

                a =
                        result & 0xFF;

                break;
            }


            // SUBB A,Rn
            case "SUBB": {

                int n =
                        regIndex(
                                instr.operands.get(0)
                        );

                int operand =
                        r.get(n);

                int carryIn =
                        cy ? 1 : 0;

                int oldA =
                        a;

                int result =
                        oldA
                        - operand
                        - carryIn;

                cy =
                        result < 0;

                int finalResult =
                        result & 0xFF;

                boolean signA =
                        (oldA & 0x80) != 0;

                boolean signOperand =
                        (operand & 0x80) != 0;

                boolean signResult =
                        (finalResult & 0x80) != 0;

                ov =
                        (signA != signOperand)
                                &&
                        (signResult != signA);

                a =
                        finalResult;

                break;
            }


            // ANL A,Rn
            case "ANL": {

                int n =
                        regIndex(
                                instr.operands.get(0)
                        );

                a =
                        a & r.get(n);

                break;
            }


            // INC Rn
            case "INC": {

                int n =
                        regIndex(
                                instr.operands.get(0)
                        );

                r.set(
                        n,
                        (r.get(n) + 1) & 0xFF
                );

                break;
            }


            // SJMP relative
            case "SJMP": {

                int offset =
                        Integer.parseInt(
                                instr.operands.get(0)
                        );

                pc =
                        pc + offset;

                break;
            }


            // Simulator termination
            case "HALT": {

                running = false;

                break;
            }


            default:

                throw new IllegalStateException(
                        "Unhandled instruction: "
                                + instr.mnemonic
                );
        }
    }


    // Converts R1 -> 1
    private int regIndex(
            String token
    ) {

        int index =
                Integer.parseInt(
                        token.substring(1)
                );

        if (
                index < 0 ||
                index > 7
        ) {

            throw new IllegalArgumentException(
                    "Invalid register: "
                            + token
            );
        }

        return index;
    }


    // ================= STEP =================

    public Instruction step() {

        if (!running) {
            return null;
        }

        Instruction fetched =
                fetch();

        if (fetched == null) {
            return null;
        }

        Instruction decoded =
                decode(fetched);

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


    public int getR(
            int n
    ) {

        return r.get(n);
    }


    public void setR(
            int n,
            int value
    ) {

        r.set(
                n,
                value & 0xFF
        );
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


    public String getInstructionCategory(
            String mnemonic
    ) {

        return instructionTable.getOrDefault(
                mnemonic,
                "Unknown"
        );
    }


    public int readDataMemory(
            int address
    ) {

        return dataMemory.read(address)
                & 0xFF;
    }


    public StackMemory getStack() {
        return stack;
    }


    public int getSP() {
        return stack.getSP();
    }
}
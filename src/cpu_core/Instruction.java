package cpu_core;

import java.util.List;

// A single instruction: its name (mnemonic) and its operands.
// Examples:
//   new Instruction("ADD", List.of("R1"))              == ADD A, R1
//   new Instruction("MOV_RN_DATA", List.of("R1","3"))  == MOV R1, #3
//   new Instruction("PUSH", List.of("A"))              == PUSH A
//   new Instruction("POP", List.of("R2"))              == POP R2
//   new Instruction("ENQUEUE", List.of("#5"))          == ENQUEUE #5
//   new Instruction("DEQUEUE", List.of("A"))           == DEQUEUE A
public class Instruction {
    public final String mnemonic;
    public final List<String> operands;

    public Instruction(String mnemonic, List<String> operands) {
        this.mnemonic = mnemonic;
        this.operands = operands;
    }

    @Override
    public String toString() {
        return mnemonic + " " + String.join(", ", operands);
    }
}
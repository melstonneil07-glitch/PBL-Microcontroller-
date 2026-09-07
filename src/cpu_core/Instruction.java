package cpu_core;

import java.util.List;

// A single instruction: its name (mnemonic) and its operands.
// e.g. new Instruction("ADD", List.of("R1"))  ==  ADD A, R1
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
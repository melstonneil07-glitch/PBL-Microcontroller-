package cpu_core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Instruction {
    public final String mnemonic;
    public final List<String> operands;

    public Instruction(String mnemonic, List<String> operands) {
        this.mnemonic = mnemonic;
        this.operands = Collections.unmodifiableList(
            new ArrayList<>(operands)
        );
    }

    @Override
    public String toString() {
        if (operands.isEmpty()) {
            return mnemonic;
        }

        return mnemonic + " " + String.join(", ", operands);
    }
}
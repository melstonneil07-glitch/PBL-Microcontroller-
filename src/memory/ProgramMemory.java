package memory;

public class ProgramMemory {

    private String[] memory = new String[256];

    public void write(int address, String instruction) {
        memory[address] = instruction;
    }

    public String read(int address) {
        return memory[address];
    }
}

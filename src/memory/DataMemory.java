package memory;

public class DataMemory {

    private int[] memory = new int[256];

    public void write(int address, int value) {
        memory[address] = value;
    }

    public int read(int address) {
        return memory[address];
    }
}

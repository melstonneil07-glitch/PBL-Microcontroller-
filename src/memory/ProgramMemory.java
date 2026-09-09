package memory;

public class ProgramMemory {

    // MS51FB9AE has 16 KB program memory
    private static final int MEMORY_SIZE = 16 * 1024;

    private byte[] memory = new byte[MEMORY_SIZE];

    // Write one byte into program memory
    public void write(int address, byte value) {
        checkAddress(address);
        memory[address] = value;
    }

    // Read one byte from program memory
    public byte read(int address) {
        checkAddress(address);
        return memory[address];
    }

    // Check whether the address is valid or not
    private void checkAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException(
                "Invalid program memory address: " + address
            );
        }
    }

    // Get program memory size
    public int getSize() {
        return MEMORY_SIZE;
    }
}

package memory;

public class DataMemory {

    // MS51FB9AE has 256 bytes of internal RAM
    private static final int MEMORY_SIZE = 256;
    private byte[] memory = new byte[MEMORY_SIZE];
    // Writingone byte into data memory
    public void write(int address, byte value) {
        checkAddress(address);
        memory[address] = value;
    }
    // Read one byte from data memory
    public byte read(int address) {
        checkAddress(address);
        return memory[address];
    }
    // Check whether the address is valid or not
    private void checkAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException(
                "Invalid data memory address: " + address
            );
        }
    }

    // Get data memory size
    public int getSize() {
        return MEMORY_SIZE;
    }
}

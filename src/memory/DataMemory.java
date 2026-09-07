package memory;

// Internal Data Memory (RAM) for the Nuvoton MS51FB9AE.
// Per the MS51FB9AE datasheet, the chip has 256 bytes of on-chip
// internal RAM (separate from its 1K bytes of extended SRAM, which
// this simplified emulator does not model).
public class DataMemory {

    // MS51FB9AE has 256 bytes of internal RAM
    private static final int MEMORY_SIZE = 256;

    private final byte[] memory = new byte[MEMORY_SIZE];

    // Writes one byte into data memory at the given address.
    public void write(int address, byte value) {
        checkAddress(address);
        memory[address] = value;
    }

    // Reads one byte from data memory at the given address.
    public byte read(int address) {
        checkAddress(address);
        return memory[address];
    }

    // Validates that an address falls within the 256-byte range.
    private void checkAddress(int address) {
        if (address < 0 || address >= MEMORY_SIZE) {
            throw new IllegalArgumentException(
                "Invalid data memory address: " + address
            );
        }
    }

    // Returns the total size of data memory in bytes (256).
    public int getSize() {
        return MEMORY_SIZE;
    }

    // Resets all data memory to zero (used on CPU reset).
    public void clear() {
        for (int i = 0; i < MEMORY_SIZE; i++) {
            memory[i] = 0;
        }
    }
}
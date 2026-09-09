package memory;

public class StackMemory {
    private byte[] stack = new byte[256];

    // MS51FB9AE/8051 stack pointer starts at 07H after reset
    private int sp = 0x07;
    public void push(byte value) {
        if (sp >= 0xFF) {
            throw new IllegalStateException("Stack overflow");
        }

        stack[++sp] = value;
    }
    public byte pop() {
        if (sp <= 0x07) {
            throw new IllegalStateException("Stack underflow");
        }

        return stack[sp--];
    }

    public byte peek() {
        if (sp <= 0x07) {
            throw new IllegalStateException("Stack is empty");
        }

        return stack[sp];
    }

    public int getSP() {
        return sp;
    }

    public void reset() {
        sp = 0x07;
    }
}

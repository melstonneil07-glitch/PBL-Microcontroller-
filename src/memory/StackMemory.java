package memory;

import java.util.ArrayList;

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

    public boolean isEmpty() {
        return sp <= 0x07;
    }

    public boolean isFull() {
        return sp >= 0xFF;
    }

    // Number of bytes currently pushed onto the stack.
    public int getStackSize() {
        return sp - 0x07;
    }

    // Returns pushed values, bottom -> top, as unsigned ints, for UI display.
    public ArrayList<Integer> snapshot() {
        ArrayList<Integer> result = new ArrayList<>();
        for (int i = 0x08; i <= sp; i++) {
            result.add(stack[i] & 0xFF);
        }
        return result;
    }
}
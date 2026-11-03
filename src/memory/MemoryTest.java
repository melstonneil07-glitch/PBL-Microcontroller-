package memory;

public class MemoryTest {
    public static void main(String[] args) {
        // Test Data Memory
        DataMemory dataMemory = new DataMemory();
        dataMemory.write(10, (byte) 50);

        if (dataMemory.read(10) == 50) {
            System.out.println("Data Memory Test: PASS");
        } else {
            System.out.println("Data Memory Test: FAIL");
        }
        // Test Program Memory
        ProgramMemory programMemory = new ProgramMemory();
        programMemory.write(100, (byte) 25);

        if (programMemory.read(100) == 25) {
            System.out.println("Program Memory Test: PASS");
        } else {
            System.out.println("Program Memory Test: FAIL");
        }
        // Test Stack Memory
        StackMemory stackMemory = new StackMemory();
        stackMemory.push((byte) 30);

        if (stackMemory.pop() == 30) {
            System.out.println("Stack Memory Test: PASS");
        } else {
            System.out.println("Stack Memory Test: FAIL");
        }
        //Test Stack Peek
       stackMemory.push((byte) 10);
       stackMemory.push((byte) 20);
       if (stackMemory.peek() == 20) {
        System.out.println("Stack Peek Test: PASS");
    } else {
         System.out.println("Stack Peek Test: FAIL");
        }
        // Test Stack Order
        if (stackMemory.pop() == 20 && stackMemory.pop() == 10) {
             System.out.println("Stack Order Test: PASS");
            } else {
                System.out.println("Stack Order Test: FAIL");
            }
            // Test invalid Data Memory address
            try {
                 dataMemory.read(256);
                 System.out.println("Invalid Data Memory Test: FAIL");
                } catch (IllegalArgumentException e) {
                    System.out.println("Invalid Data Memory Test: PASS");
                }

        // ===== Week 3: Stack status tests =====

        // Test Stack Empty condition (freshly reset stack)
        StackMemory emptyStack = new StackMemory();
        if (emptyStack.isEmpty() && !emptyStack.isFull()) {
            System.out.println("Stack Empty Condition Test: PASS");
        } else {
            System.out.println("Stack Empty Condition Test: FAIL");
        }

        // Test Stack Underflow (pop with nothing pushed)
        try {
            emptyStack.pop();
            System.out.println("Stack Underflow Test: FAIL");
        } catch (IllegalStateException e) {
            System.out.println("Stack Underflow Test: PASS");
        }

        // Test Stack Overflow (push past capacity: SP starts at 0x07, max is 0xFF)
        StackMemory fullStack = new StackMemory();
        for (int i = 0; i < 248; i++) { // 248 pushes brings SP from 0x07 to 0xFF exactly
            fullStack.push((byte) i);
        }
        boolean overflowed = false;
        try {
            fullStack.push((byte) 1); // stack is now full -- this push should overflow
        } catch (IllegalStateException e) {
            overflowed = true;
        }
        if (overflowed && fullStack.isFull()) {
            System.out.println("Stack Overflow Test: PASS");
        } else {
            System.out.println("Stack Overflow Test: FAIL");
        }

        // ===== Week 3: FIFO Queue tests =====

        // Test Queue Enqueue/Dequeue basic
        QueueMemory queueMemory = new QueueMemory();
        queueMemory.enqueue(5);
        if (queueMemory.dequeue() == 5) {
            System.out.println("Queue Memory Test: PASS");
        } else {
            System.out.println("Queue Memory Test: FAIL");
        }

        // Test Queue FIFO order (first in, first out)
        queueMemory.enqueue(10);
        queueMemory.enqueue(20);
        queueMemory.enqueue(30);
        if (queueMemory.dequeue() == 10
                && queueMemory.dequeue() == 20
                && queueMemory.dequeue() == 30) {
            System.out.println("Queue FIFO Order Test: PASS");
        } else {
            System.out.println("Queue FIFO Order Test: FAIL");
        }

        // Test Queue Empty condition
        if (queueMemory.isEmpty() && !queueMemory.isFull()) {
            System.out.println("Queue Empty Condition Test: PASS");
        } else {
            System.out.println("Queue Empty Condition Test: FAIL");
        }

        // Test Queue Underflow (dequeue with nothing enqueued)
        try {
            queueMemory.dequeue();
            System.out.println("Queue Underflow Test: FAIL");
        } catch (IllegalStateException e) {
            System.out.println("Queue Underflow Test: PASS");
        }

        // Test Queue Full condition / overflow
        QueueMemory fullQueue = new QueueMemory();
        boolean queueOverflowed = false;
        try {
            for (int i = 0; i < fullQueue.getCapacity(); i++) {
                fullQueue.enqueue(i);
            }
            if (fullQueue.isFull()) {
                fullQueue.enqueue(99); // this one should overflow
            }
        } catch (IllegalStateException e) {
            queueOverflowed = true;
        }
        if (queueOverflowed && fullQueue.isFull()) {
            System.out.println("Queue Full Condition Test: PASS");
        } else {
            System.out.println("Queue Full Condition Test: FAIL");
        }
    }
}
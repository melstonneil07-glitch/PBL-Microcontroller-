package memory;

import java.util.ArrayList;

// FIFO Queue for the Week 3 enhancement.
// Implements Enqueue/Dequeue over a fixed-capacity circular buffer,
// using an ArrayList as the underlying storage.
public class QueueMemory {

    private static final int CAPACITY = 16;

    private final ArrayList<Integer> buffer = new ArrayList<>();

    public QueueMemory() {
        for (int i = 0; i < CAPACITY; i++) {
            buffer.add(0);
        }
    }

    private int front = 0;  // index of the next item to dequeue
    private int rear = 0;   // index where the next item will be enqueued
    private int count = 0;  // number of items currently queued

    // Adds one value to the back of the queue.
    public void enqueue(int value) {
        if (isFull()) {
            throw new IllegalStateException("Queue overflow: queue is full");
        }
        buffer.set(rear, value & 0xFF);
        rear = (rear + 1) % CAPACITY;
        count++;
    }

    // Removes and returns the value at the front of the queue.
    public int dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue underflow: queue is empty");
        }
        int value = buffer.get(front);
        front = (front + 1) % CAPACITY;
        count--;
        return value;
    }

    // Returns the value at the front without removing it.
    public int peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Queue is empty");
        }
        return buffer.get(front);
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public boolean isFull() {
        return count == CAPACITY;
    }

    public int getCount() {
        return count;
    }

    public int getCapacity() {
        return CAPACITY;
    }

    // Resets the queue to empty.
    public void reset() {
        front = 0;
        rear = 0;
        count = 0;
        for (int i = 0; i < CAPACITY; i++) {
            buffer.set(i, 0);
        }
    }

    // Returns the queued values in FIFO order (front -> rear), for UI display.
    public ArrayList<Integer> snapshot() {
        ArrayList<Integer> result = new ArrayList<>();
        int idx = front;
        for (int i = 0; i < count; i++) {
            result.add(buffer.get(idx));
            idx = (idx + 1) % CAPACITY;
        }
        return result;
    }
}
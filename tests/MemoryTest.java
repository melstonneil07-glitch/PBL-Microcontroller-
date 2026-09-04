import memory.DataMemory;
import memory.ProgramMemory;
import memory.StackMemory;

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
    }
}

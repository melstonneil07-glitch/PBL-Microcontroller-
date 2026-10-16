package cpu_core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// Test cases for the Week 4 FCFS scheduler, PCB and context switching.
// TS01-TS02: FCFS ordering and timing    TS03-TS04: context isolation
// TS05-TS06: idle gap / arrival tie      TS07-TS08: fault isolation / reset
public class SchedulerTest {

    private static int total = 0;
    private static int passed = 0;

    public static void main(String[] args) {
        System.out.println("Test    Description                              Expected            Actual              Status");
        System.out.println("-----------------------------------------------------------------------------------------------");

        testFcfsOrderAndTimes();
        testNonPreemptiveGantt();
        testContextRegistersIsolated();
        testContextStackIsolated();
        testIdleUntilArrival();
        testSameArrivalUsesCreationOrder();
        testFaultDoesNotKillOthers();
        testResetRestoresProcesses();

        System.out.println("-----------------------------------------------------------------------------------------------");
        System.out.println(passed + " / " + total + " test cases passed.");
    }

    private static void check(String id, String description, String expected, Object actual) {
        total++;
        boolean ok = expected.equals(String.valueOf(actual));
        if (ok) passed++;
        System.out.printf("%-7s %-40s %-19s %-19s %s%n", id, description, expected, actual, ok ? "PASS" : "FAIL");
    }

    // ---------- helpers ----------

    private static Instruction ins(String mnemonic, String... operands) {
        return new Instruction(mnemonic, Arrays.asList(operands));
    }

    // n-1 filler instructions followed by HALT => burst of exactly n ticks.
    private static ArrayList<Instruction> burst(int n) {
        ArrayList<Instruction> p = new ArrayList<>();
        for (int i = 0; i < n - 1; i++) p.add(ins("INC", "R0"));
        p.add(new Instruction("HALT", Collections.emptyList()));
        return p;
    }

    private static ProcessManager newManager() {
        return new ProcessManager(new CPU(), new FCFSScheduler());
    }

    private static void runToEnd(ProcessManager pm) {
        int guard = 10000;
        while (pm.tick().kind != ProcessManager.TickResult.Kind.DONE && guard-- > 0) { /* keep ticking */ }
    }

    private static String finishOrder(ProcessManager pm) {
        List<PCB> done = pm.getProcesses();
        done.sort((a, b) -> Integer.compare(a.finishTime, b.finishTime));
        StringBuilder sb = new StringBuilder();
        for (PCB p : done) sb.append(p.name);
        return sb.toString();
    }

    // ---------- tests ----------

    private static void testFcfsOrderAndTimes() {
        ProcessManager pm = newManager();
        PCB p1 = pm.create("P1", 0, burst(3));
        PCB p2 = pm.create("P2", 0, burst(2));
        PCB p3 = pm.create("P3", 1, burst(2));
        runToEnd(pm);

        check("TS01a", "FCFS finish order", "P1P2P3", finishOrder(pm));
        check("TS01b", "Finish times P1,P2,P3", "3,5,7", p1.finishTime + "," + p2.finishTime + "," + p3.finishTime);
        check("TS01c", "Waiting times P1,P2,P3", "0,3,4",
                p1.waitingTime(pm.getClock()) + "," + p2.waitingTime(pm.getClock()) + "," + p3.waitingTime(pm.getClock()));
        check("TS01d", "Turnaround P1,P2,P3", "3,5,6", p1.turnaroundTime() + "," + p2.turnaroundTime() + "," + p3.turnaroundTime());
    }

    private static void testNonPreemptiveGantt() {
        ProcessManager pm = newManager();
        pm.create("P1", 0, burst(4));
        pm.create("P2", 1, burst(2));
        runToEnd(pm);
        StringBuilder g = new StringBuilder();
        for (ProcessManager.Segment s : pm.getGantt()) g.append(s.pid).append(":").append(s.start).append("-").append(s.end).append(" ");
        check("TS02a", "Gantt: one unbroken bar per process", "1:0-4 2:4-6", g.toString().trim());
        check("TS02b", "Context switches (2 processes)", "1", pm.getContextSwitches());
    }

    private static void testContextRegistersIsolated() {
        ProcessManager pm = newManager();
        ArrayList<Instruction> a = new ArrayList<>();
        a.add(ins("MOV_RN_DATA", "R1", "7"));
        a.add(ins("MOV_A_DATA", "21"));
        a.add(ins("HALT"));
        ArrayList<Instruction> b = new ArrayList<>();
        b.add(ins("MOV_RN_DATA", "R1", "9"));
        b.add(ins("MOV_A_DATA", "42"));
        b.add(ins("HALT"));
        PCB p1 = pm.create("P1", 0, a);
        PCB p2 = pm.create("P2", 0, b);
        runToEnd(pm);
        check("TS03a", "P1 saved R1 / A", "7 / 21", p1.context.registers[1] + " / " + p1.context.a);
        check("TS03b", "P2 saved R1 / A", "9 / 42", p2.context.registers[1] + " / " + p2.context.a);
    }

    private static void testContextStackIsolated() {
        ProcessManager pm = newManager();
        ArrayList<Instruction> a = new ArrayList<>();
        a.add(ins("MOV_A_DATA", "5"));
        a.add(ins("PUSH", "A"));
        a.add(ins("ENQUEUE", "#3"));
        a.add(ins("HALT"));
        PCB p1 = pm.create("P1", 0, a);
        PCB p2 = pm.create("P2", 0, burst(2));
        runToEnd(pm);
        check("TS04a", "P1 stack kept after P2 ran (SP)", "8", p1.context.stack.getSP());
        check("TS04b", "P2 stack untouched (SP)", "7", p2.context.stack.getSP());
        check("TS04c", "P1 queue count / P2 queue count", "1 / 0",
                p1.context.queue.getCount() + " / " + p2.context.queue.getCount());
    }

    private static void testIdleUntilArrival() {
        ProcessManager pm = newManager();
        PCB p1 = pm.create("P1", 3, burst(2));
        int idle = 0;
        ProcessManager.TickResult r;
        while ((r = pm.tick()).kind != ProcessManager.TickResult.Kind.DONE) {
            if (r.kind == ProcessManager.TickResult.Kind.IDLE) idle++;
        }
        check("TS05a", "Idle ticks before arrival at t=3", "3", idle);
        check("TS05b", "P1 start / finish", "3 / 5", p1.startTime + " / " + p1.finishTime);
        check("TS05c", "No wait for a lone late arrival", "0", p1.waitingTime(pm.getClock()));
    }

    private static void testSameArrivalUsesCreationOrder() {
        ProcessManager pm = newManager();
        pm.create("A", 2, burst(1));
        pm.create("B", 2, burst(1));
        pm.create("C", 2, burst(1));
        runToEnd(pm);
        check("TS06", "Equal arrival -> creation order", "ABC", finishOrder(pm));
    }

    private static void testFaultDoesNotKillOthers() {
        ProcessManager pm = newManager();
        ArrayList<Instruction> bad = new ArrayList<>();
        bad.add(ins("POP", "A"));                 // stack underflow
        bad.add(new Instruction("HALT", Collections.emptyList()));
        PCB p1 = pm.create("BAD", 0, bad);
        PCB p2 = pm.create("OK", 0, burst(2));
        runToEnd(pm);
        check("TS07a", "Faulting process terminated", "TERMINATED", p1.state);
        check("TS07b", "Fault message recorded", "true", p1.fault != null && p1.fault.contains("underflow"));
        check("TS07c", "Next process still completes", "TERMINATED / 2", p2.state + " / " + p2.executed);
    }

    private static void testResetRestoresProcesses() {
        ProcessManager pm = newManager();
        PCB p1 = pm.create("P1", 0, burst(3));
        runToEnd(pm);
        pm.reset();
        check("TS08a", "Reset: state / clock", "NEW / 0", p1.state + " / " + pm.getClock());
        runToEnd(pm);
        check("TS08b", "Re-run after reset finishes at", "3", p1.finishTime);
    }
}

# Initial System Architecture Decision:

## Decision:
**The team decided to use a modular architecture for the MS51FB9AE microcontroller simulator.**

**The initial system will be divided into three main modules:**

**1. User Interface & Controls**

**2. OS & Process Management**

**3. MS51FB9AE Hardware Simulation**

# 1.User Interface & Controls:
## The UI module will contain:
**1. Control Panel: provides Run, Single-Step, and Reset controls.**

**Run – starts the execution of the loaded program.**

**Single-Step – executes one instruction at a time, which helps the user understand instruction execution.**

**Reset – resets the CPU and simulator state to its initial condition.**

**2. live State & Performance Visualizer – displays the current simulator state and performance information.**

**It can be used to show:**

**1. Current CPU state**

**2. Register values**

**3. Program Counter**

**4. Stack Pointer**

**5. Current instruction**

**6. Process/scheduling information**

**7. Basic performance information**

**The visualizer will help users understand what is happening inside the simulated microcontroller.**

# 2.OS & Process Management:
**This subsystem handles the Operating System concepts included in the project. It manages processes and determines which process should receive CPU time.**

## The OS subsystem will contain:

**1. Process Manager & PCB Generator – manages processes and their process control information.**

**2. Ready Queue & Circular Queues – maintains processes waiting for CPU execution.**

**3. CPU Scheduler – supports FCFS, Round Robin, and Priority scheduling.**

**4. Context Switch Engine – manages switching between processes and the CPU.**

# 3.MS51FB9AE Hardware Simulation:
## The hardware simulation will contain three main parts:
## 1. 8051 CPU Core:
**1. Registers:**

**The architecture includes:**

**A (Accumulator) – used during many arithmetic and logical operations.**
**B Register – used by certain arithmetic operations.**
**PSW (Program Status Word) – stores status/flag information.**
**PC (Program Counter) – keeps track of the program execution position.**
**SP (Stack Pointer) – manages the stack.**
**R0–R7 – general-purpose registers.**

**The simulator will maintain the values of these registers during program execution.**

**2. Instruction Decoder:**

**The Instruction Decoder will identify what operation an instruction represents.**

**3. Fetch-Execute Loop:**

**The simulator will fetch an instruction, decode it, and then execute the corresponding operation.**

**Detailed instruction implementation is planned for later development.**

## 2.Memory Space:
**The Memory module represents the memory used by the simulated microcontroller.**

**Flash ROM:**

**The architecture represents 16 KB Flash ROM for storing program/code instructions.**

**The simulator will use this area to represent the program that the CPU needs to execute.**

**Internal SRAM & Stack:**

**The architecture represents 1 KB Internal SRAM for temporary data storage.**

**The stack will use the appropriate memory area and will be managed using the Stack Pointer.**

## 3.Peripheral Emulation:
**The Peripheral module represents hardware peripherals that interact with the CPU.**

**GPIO:**

**The simulator will represent the GPIO ports:**

**P0**
**P1**
**P2**
**P3**

**GPIO simulation will allow the project to demonstrate basic input/output operations.**

**Timers:**

**The architecture includes:**

**Timer 0**
**Timer 1**
**Timer 2**

**These will be used to represent timer-related operations in the simulator.**

**Interrupt Controller:**

**The Interrupt Controller will represent the handling of interrupts and their corresponding vectors.**

**When an interrupt occurs, the simulator can later transfer control to the appropriate interrupt-handling mechanism.**

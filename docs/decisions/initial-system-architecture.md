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
## The OS subsystem will contain:

**1. Process Manager & PCB Generator – manages processes and their process control information.**

**2. Ready Queue & Circular Queues – maintains processes waiting for CPU execution.**

**3. CPU Scheduler – supports FCFS, Round Robin, and Priority scheduling.**

**4. Context Switch Engine – manages switching between processes and the CPU.**

# 3.MS51FB9AE Hardware Simulation:
## The hardware simulation will contain three main parts:
## 1.CPU Core:
**1. Registers: A, B, PSW, PC, SP, R0–R7**

**2. Instruction Decoder**

**3. Fetch-Execute Loop**

## 2.Memory Space:
**1. 16 KB Flash ROM for program/code storage**

**2. 1 KB Internal SRAM and Stack**

## 3.Peripheral Emulation:
**1. GPIO Ports: P0, P1, P2, P3**

**2. 16-bit Timers: Timer 0, Timer 1, Timer 2**

**3. Interrupt Controller and Vectors**

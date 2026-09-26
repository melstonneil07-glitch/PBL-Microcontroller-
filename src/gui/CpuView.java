package gui;

import cpu_core.CpuSnapshot;

// Any panel that displays live CPU state implements this.
// MainWindow holds a list of CpuView and calls refresh() on all of
// them the same way, instead of calling each panel's own differently
// named update method. Panels receive a CpuSnapshot -- a plain data
// object -- rather than the CPU class itself, so panels never depend
// on CPU, StackMemory, or QueueMemory's internal APIs.
public interface CpuView {
    void refresh(CpuSnapshot snapshot, String status);
}
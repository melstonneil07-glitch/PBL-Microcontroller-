package gui;

import cpu_core.CpuSnapshot;

import javax.swing.*;
import java.awt.*;

public class MemoryPanel extends JPanel implements CpuView {

    private JTextArea contentsArea;

    public MemoryPanel() {

        setBorder(
                BorderFactory.createTitledBorder("DATA MEMORY (non-zero addresses)")
        );

        setLayout(new BorderLayout());

        contentsArea = new JTextArea(4, 20);
        contentsArea.setEditable(false);

        add(new JScrollPane(contentsArea), BorderLayout.CENTER);
    }

    @Override
    public void refresh(CpuSnapshot snapshot, String status) {

        StringBuilder sb = new StringBuilder();

        for (int[] entry : snapshot.nonZeroMemory) {
            sb.append(String.format("[%02XH] = %02XH%n", entry[0], entry[1]));
        }

        if (snapshot.nonZeroMemory.isEmpty()) {
            sb.append("(all zero)\n");
        }

        contentsArea.setText(sb.toString());
    }
}
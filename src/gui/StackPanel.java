package gui;

import cpu_core.CpuSnapshot;

import javax.swing.*;
import java.awt.*;

public class StackPanel extends JPanel implements CpuView {

    private JLabel spLabel;
    private JLabel statusLabel;
    private JTextArea contentsArea;

    public StackPanel() {

        setBorder(
                BorderFactory.createTitledBorder("STACK")
        );

        setLayout(new BorderLayout());

        JPanel info = new JPanel(new GridLayout(2, 1));

        spLabel = new JLabel("SP: 07H");
        statusLabel = new JLabel("Status: Empty");

        info.add(spLabel);
        info.add(statusLabel);

        contentsArea = new JTextArea(4, 20);
        contentsArea.setEditable(false);

        add(info, BorderLayout.NORTH);
        add(new JScrollPane(contentsArea), BorderLayout.CENTER);
    }

    @Override
    public void refresh(CpuSnapshot snapshot, String status) {

        spLabel.setText(
                String.format("SP: %02XH", snapshot.sp)
        );

        String stackStatus;
        if (snapshot.stackEmpty) {
            stackStatus = "Empty";
        } else if (snapshot.stackFull) {
            stackStatus = "Full";
        } else {
            stackStatus = "OK (" + snapshot.stackContents.size() + " byte(s))";
        }
        statusLabel.setText("Status: " + stackStatus);

        StringBuilder sb = new StringBuilder();
        int size = snapshot.stackContents.size();
        for (int i = size - 1; i >= 0; i--) {
            sb.append(String.format("[SP-%d] %02XH%n", size - 1 - i, snapshot.stackContents.get(i)));
        }
        if (size == 0) {
            sb.append("(stack empty)\n");
        }
        contentsArea.setText(sb.toString());
    }
}
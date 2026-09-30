package gui;

import cpu_core.CpuSnapshot;

import javax.swing.*;
import java.awt.*;

public class QueuePanel extends JPanel implements CpuView {

    private JLabel countLabel;
    private JLabel statusLabel;
    private JTextArea contentsArea;

    public QueuePanel() {

        setBorder(
                BorderFactory.createTitledBorder("FIFO QUEUE")
        );

        setLayout(new BorderLayout());

        JPanel info = new JPanel(new GridLayout(2, 1));

        countLabel = new JLabel("Count: 0 / 16");
        statusLabel = new JLabel("Status: Empty");

        info.add(countLabel);
        info.add(statusLabel);

        contentsArea = new JTextArea(4, 20);
        contentsArea.setEditable(false);

        add(info, BorderLayout.NORTH);
        add(new JScrollPane(contentsArea), BorderLayout.CENTER);
    }

    @Override
    public void refresh(CpuSnapshot snapshot, String status) {

        countLabel.setText(
                "Count: " + snapshot.queueCount + " / " + snapshot.queueCapacity
        );

        String queueStatus;
        if (snapshot.queueEmpty) {
            queueStatus = "Empty";
        } else if (snapshot.queueFull) {
            queueStatus = "Full";
        } else {
            queueStatus = "OK";
        }
        statusLabel.setText("Status: " + queueStatus);

        StringBuilder sb = new StringBuilder();
        sb.append("Front -> Rear:\n");
        for (int i = 0; i < snapshot.queueContents.size(); i++) {
            sb.append(String.format("[%d] %02XH%n", i, snapshot.queueContents.get(i)));
        }
        if (snapshot.queueContents.isEmpty()) {
            sb.append("(queue empty)\n");
        }
        contentsArea.setText(sb.toString());
    }
}
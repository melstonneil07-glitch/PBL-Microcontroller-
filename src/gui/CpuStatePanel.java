package gui;

import cpu_core.CPU;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class CpuStatePanel extends JPanel {

    private JLabel statusLabel;
    private JLabel pcLabel;
    private JLabel spLabel;
    private JLabel aLabel;
    private JLabel bLabel;
    private JLabel cyLabel;
    private JLabel ovLabel;
    private JLabel[] registerLabels;

    public CpuStatePanel() {
        setBorder(BorderFactory.createTitledBorder("CPU State"));
        setLayout(new GridLayout(0, 2, 10, 5));

        statusLabel = new JLabel("Status: Not loaded");
        pcLabel = new JLabel("PC: 0000");
        spLabel = new JLabel("SP: 07");
        aLabel = new JLabel("A: 00");
        bLabel = new JLabel("B: N/A");
        cyLabel = new JLabel("CY: 0");
        ovLabel = new JLabel("OV: 0");

        add(statusLabel);
        add(new JLabel(""));
        add(pcLabel);
        add(spLabel);
        add(aLabel);
        add(bLabel);

        registerLabels = new JLabel[8];

        for (int i = 0; i < 8; i++) {
            registerLabels[i] = new JLabel("R" + i + ": 00");
            add(registerLabels[i]);
        }

        add(cyLabel);
        add(ovLabel);
    }

    public void updateState(CPU cpu, String status) {
        statusLabel.setText("Status: " + status);
        pcLabel.setText("PC: " + String.format("%04X", cpu.getPC()));
        spLabel.setText("SP: " + String.format("%02X", cpu.getSP()));
        aLabel.setText("A: " + String.format("%02X", cpu.getA()));

        // The current CPU class does not contain a B register/getB() method.
        bLabel.setText("B: N/A");

        for (int i = 0; i < 8; i++) {
            registerLabels[i].setText(
                "R" + i + ": " + String.format("%02X", cpu.getR(i))
            );
        }

        cyLabel.setText("CY: " + (cpu.isCY() ? "1" : "0"));
        ovLabel.setText("OV: " + (cpu.isOV() ? "1" : "0"));
    }
}

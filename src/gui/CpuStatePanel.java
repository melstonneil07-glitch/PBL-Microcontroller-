package gui;

import cpu_core.CpuSnapshot;
import java.awt.*;
import javax.swing.*;

public class CpuStatePanel extends JPanel implements CpuView {

    private JLabel aLabel;
    private JLabel bLabel;
    private JLabel pswLabel;
    private JLabel pcLabel;
    private JLabel spLabel;

    private JLabel[] rLabels;

    private JLabel cyLabel;
    private JLabel ovLabel;

    private JLabel statusLabel;

    public CpuStatePanel() {

        setBorder(
                BorderFactory.createTitledBorder(
                        "CPU REGISTERS"
                )
        );

        setLayout(
                new GridLayout(0, 2, 10, 12)
        );

        aLabel = new JLabel("A    00H");
        bLabel = new JLabel("B    00H");

        pswLabel = new JLabel("PSW  00H");
        pcLabel = new JLabel("PC   0000H");

        spLabel = new JLabel("SP   07H");
        cyLabel = new JLabel("CY   0");

        ovLabel = new JLabel("OV   0");
        statusLabel = new JLabel("Status: Ready");

        rLabels = new JLabel[8];

        for (int i = 0; i < 8; i++) {
            rLabels[i] = new JLabel(
                    "R" + i + "   00H"
            );
        }

        add(aLabel);
        add(bLabel);

        add(pswLabel);
        add(pcLabel);

        add(spLabel);
        add(cyLabel);

        add(ovLabel);
        add(statusLabel);

        add(rLabels[0]);
        add(rLabels[4]);

        add(rLabels[1]);
        add(rLabels[5]);

        add(rLabels[2]);
        add(rLabels[6]);

        add(rLabels[3]);
        add(rLabels[7]);
    }

    @Override
    public void refresh(CpuSnapshot snapshot, String status) {

        aLabel.setText(
                String.format("A    %02XH", snapshot.a)
        );

        pcLabel.setText(
                String.format("PC   %04XH", snapshot.pc)
        );

        spLabel.setText(
                String.format("SP   %02XH", snapshot.sp)
        );

        cyLabel.setText(
                "CY   " + (snapshot.cy ? "1" : "0")
        );

        ovLabel.setText(
                "OV   " + (snapshot.ov ? "1" : "0")
        );

        for (int i = 0; i < 8; i++) {
            rLabels[i].setText(
                    String.format("R%d   %02XH", i, snapshot.registers[i])
            );
        }

        statusLabel.setText(
                "Status: " + status
        );
    }
}
package gui;

import cpu_core.CPU;

import javax.swing.*;
import java.awt.*;

public class CpuStatePanel extends JPanel {

    private JLabel a;
    private JLabel b;
    private JLabel psw;
    private JLabel pc;
    private JLabel sp;

    private JLabel[] registers;

    private JLabel cy;
    private JLabel ov;
    private JLabel status;

    public CpuStatePanel() {

        setBorder(
                BorderFactory.createTitledBorder(
                        "MS51FB9AE CPU STATE"
                )
        );

        setLayout(
                new GridLayout(0, 2, 5, 5)
        );

        a = new JLabel("A : 00H");
        b = new JLabel("B : 00H");
        psw = new JLabel("PSW : 00H");
        pc = new JLabel("PC : 0000H");
        sp = new JLabel("SP : 07H");

        cy = new JLabel("CY : 0");
        ov = new JLabel("OV : 0");
        status = new JLabel("Status : Ready");

        registers = new JLabel[8];

        for (int i = 0; i < 8; i++) {

            registers[i] =
                    new JLabel("R" + i + " : 00H");
        }

        add(a);
        add(b);

        add(psw);
        add(pc);

        add(sp);
        add(cy);

        add(ov);
        add(status);

        for (int i = 0; i < 8; i++) {
            add(registers[i]);
        }
    }

    public void updateState(
            CPU cpu,
            String state
    ) {

        a.setText(
                String.format(
                        "A : %02XH",
                        cpu.getA()
                )
        );

        pc.setText(
                String.format(
                        "PC : %04XH",
                        cpu.getPC()
                )
        );

        sp.setText(
                String.format(
                        "SP : %02XH",
                        cpu.getSP()
                )
        );

        cy.setText(
                "CY : "
                        + (cpu.isCY() ? "1" : "0")
        );

        ov.setText(
                "OV : "
                        + (cpu.isOV() ? "1" : "0")
        );

        for (int i = 0; i < 8; i++) {

            registers[i].setText(
                    String.format(
                            "R%d : %02XH",
                            i,
                            cpu.getR(i)
                    )
            );
        }

        status.setText(
                "Status : " + state
        );
    }
}

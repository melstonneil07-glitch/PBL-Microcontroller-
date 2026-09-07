package gui;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private JButton loadButton;
    private JButton stepButton;
    private JButton runButton;
    private JButton resetButton;

    public ControlPanel() {

        setLayout(new FlowLayout());

        loadButton = new JButton("LOAD");
        stepButton = new JButton("STEP");
        runButton = new JButton("RUN");
        resetButton = new JButton("RESET");

        add(loadButton);
        add(stepButton);
        add(runButton);
        add(resetButton);
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public JButton getStepButton() {
        return stepButton;
    }

    public JButton getRunButton() {
        return runButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }
}

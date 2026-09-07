package gui;

import javax.swing.*;
import java.awt.*;

public class ControlPanel extends JPanel {

    private JButton loadButton;
    private JButton runButton;
    private JButton stepButton;
    private JButton resetButton;

    public ControlPanel() {

        setLayout(
                new FlowLayout()
        );

        loadButton = new JButton("LOAD");
        runButton = new JButton("RUN");
        stepButton = new JButton("STEP");
        resetButton = new JButton("RESET");

        add(loadButton);
        add(runButton);
        add(stepButton);
        add(resetButton);
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public JButton getRunButton() {
        return runButton;
    }

    public JButton getStepButton() {
        return stepButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }
}

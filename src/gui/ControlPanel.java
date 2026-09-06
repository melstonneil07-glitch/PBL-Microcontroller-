package gui;

import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class ControlPanel extends JPanel {

    private JButton loadButton;
    private JButton resetButton;
    private JButton stepButton;
    private JButton runButton;

    public ControlPanel() {
        setLayout(new FlowLayout());

        loadButton = new JButton("Load");
        resetButton = new JButton("Reset");
        stepButton = new JButton("Step");
        runButton = new JButton("Run");

        add(loadButton);
        add(resetButton);
        add(stepButton);
        add(runButton);
    }

    public JButton getLoadButton() {
        return loadButton;
    }

    public JButton getResetButton() {
        return resetButton;
    }

    public JButton getStepButton() {
        return stepButton;
    }

    public JButton getRunButton() {
        return runButton;
    }
}

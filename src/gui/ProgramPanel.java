package gui;

import cpu_core.Instruction;
import java.awt.BorderLayout;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ProgramPanel extends JPanel {

    private DefaultListModel<String> programModel;
    private JList<String> programList;
    private JLabel currentInstructionLabel;

    public ProgramPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Loaded Program"));

        programModel = new DefaultListModel<>();
        programList = new JList<>(programModel);

        currentInstructionLabel = new JLabel(
            "Current Instruction: None"
        );

        add(new JScrollPane(programList), BorderLayout.CENTER);
        add(currentInstructionLabel, BorderLayout.SOUTH);
    }

    public void showProgram(ArrayList<Instruction> program) {
        programModel.clear();

        for (int i = 0; i < program.size(); i++) {
            String line = String.format(
                "%04X  %s",
                i,
                program.get(i).toString()
            );

            programModel.addElement(line);
        }
    }

    public void showCurrentInstruction(
        int address,
        Instruction instruction
    ) {
        if (instruction == null) {
            currentInstructionLabel.setText(
                "Current Instruction: None"
            );
            programList.clearSelection();
            return;
        }

        currentInstructionLabel.setText(
            "Current Instruction: " + instruction.toString()
        );

        programList.setSelectedIndex(address);
        programList.ensureIndexIsVisible(address);
    }
}

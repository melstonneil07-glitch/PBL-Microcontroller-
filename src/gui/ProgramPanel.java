package gui;

import cpu_core.Instruction;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class ProgramPanel extends JPanel {

    private JLabel categoryLabel;
    private DefaultListModel<String> programModel;
    private JList<String> programList;
    private JLabel currentInstructionLabel;

    public ProgramPanel() {

        setLayout(new BorderLayout());
        setBorder(
            BorderFactory.createTitledBorder("Loaded Program")
        );

        programModel = new DefaultListModel<>();
        programList = new JList<>(programModel);

        currentInstructionLabel = new JLabel(
            "Current Instruction: None"
        );

        categoryLabel = new JLabel(
            "Category: None"
        );

        add(
            new JScrollPane(programList),
            BorderLayout.CENTER
        );

        JPanel infoPanel = new JPanel(
            new GridLayout(2, 1)
        );

        infoPanel.add(currentInstructionLabel);
        infoPanel.add(categoryLabel);

        add(
            infoPanel,
            BorderLayout.SOUTH
        );
    }

    public void showProgram(
        ArrayList<Instruction> program
    ) {
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

            categoryLabel.setText(
                "Category: None"
            );

            programList.clearSelection();
            return;
        }

        currentInstructionLabel.setText(
            "Current Instruction: "
            + instruction.toString()
        );

        programList.setSelectedIndex(address);
        programList.ensureIndexIsVisible(address);
    }

    public void showNextInstruction(
        int address,
        ArrayList<Instruction> program
    ) {

        if (address >= 0 && address < program.size()) {

            currentInstructionLabel.setText(
                "Next Instruction: "
                + program.get(address).toString()
            );

            programList.setSelectedIndex(address);
            programList.ensureIndexIsVisible(address);

        } else {

            currentInstructionLabel.setText(
                "Next Instruction: None (Program Halted)"
            );

            categoryLabel.setText(
                "Category: None"
            );

            programList.clearSelection();
        }
    }

    public void showCategory(
        String category
    ) {

        categoryLabel.setText(
            "Category: " + category
        );
    }
}

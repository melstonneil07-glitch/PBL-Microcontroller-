package gui;

import cpu_core.Instruction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ProgramPanel extends JPanel {

    private DefaultListModel<String> model;
    private JList<String> programList;

    private JLabel currentLabel;
    private JLabel categoryLabel;

    public ProgramPanel() {

        setBorder(
                BorderFactory.createTitledBorder(
                        "PROGRAM MEMORY"
                )
        );

        setLayout(
                new BorderLayout()
        );

        model = new DefaultListModel<>();

        programList =
                new JList<>(model);

        currentLabel =
                new JLabel(
                        "Current Instruction: None"
                );

        categoryLabel =
                new JLabel(
                        "Category: None"
                );

        JPanel information =
                new JPanel(
                        new GridLayout(2, 1)
                );

        information.add(currentLabel);
        information.add(categoryLabel);

        add(
                new JScrollPane(programList),
                BorderLayout.CENTER
        );

        add(
                information,
                BorderLayout.SOUTH
        );
    }

    public void showProgram(
            ArrayList<Instruction> program
    ) {

        model.clear();

        for (int i = 0; i < program.size(); i++) {

            Instruction instruction =
                    program.get(i);

            String display =
                    getDisplayInstruction(
                            instruction
                    );

            model.addElement(
                    String.format(
                            "%04X  %s",
                            i,
                            display
                    )
            );
        }
    }

    public void showCurrentInstruction(
            int pc,
            Instruction instruction
    ) {

        if (instruction == null) {
            return;
        }

        currentLabel.setText(
                "Current Instruction: "
                        + getDisplayInstruction(instruction)
        );

        categoryLabel.setText(
                "Category: "
                        + getCategory(
                        instruction.mnemonic
                )
        );

        if (pc >= 0 && pc < model.size()) {

            programList.setSelectedIndex(pc);

            programList.ensureIndexIsVisible(pc);
        }
    }

    public void showNextInstruction(
            int pc,
            ArrayList<Instruction> program
    ) {

        if (
                pc >= 0 &&
                pc < program.size()
        ) {

            showCurrentInstruction(
                    pc,
                    program.get(pc)
            );
        }
    }

    private String getDisplayInstruction(
            Instruction instruction
    ) {

        switch (instruction.mnemonic) {

            case "MOV_A_DATA":
                return "MOV A,#"
                        + instruction.operands.get(0);

            case "MOV_RN_DATA":
                return "MOV "
                        + instruction.operands.get(0)
                        + ",#"
                        + instruction.operands.get(1);

            case "ADD":
                return "ADD A,"
                        + instruction.operands.get(0);

            case "SUBB":
                return "SUBB A,"
                        + instruction.operands.get(0);

            case "ANL":
                return "ANL A,"
                        + instruction.operands.get(0);

            case "INC":
                return "INC "
                        + instruction.operands.get(0);

            case "SJMP":
                return "SJMP "
                        + instruction.operands.get(0);

            case "HALT":
                return "HALT";

            default:
                return instruction.toString();
        }
    }

    private String getCategory(
            String mnemonic
    ) {

        switch (mnemonic) {

            case "MOV_A_DATA":
            case "MOV_RN_DATA":
                return "Data Transfer";

            case "ADD":
            case "SUBB":
                return "Arithmetic";

            case "ANL":
                return "Logical";

            case "INC":
                return "Increment / Decrement";

            case "SJMP":
                return "Control Flow";

            case "HALT":
                return "Program Termination";

            default:
                return "Unknown";
        }
    }
}

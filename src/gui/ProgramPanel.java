package gui;

import cpu_core.Instruction;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class ProgramPanel extends JPanel {

    private DefaultListModel<String> model;
    private JList<String> programList;

    private JLabel currentInstruction;
    private JLabel category;

    public ProgramPanel() {

        setBorder(
                BorderFactory.createTitledBorder(
                        "PROGRAM MEMORY - APROM"
                )
        );

        setLayout(
                new BorderLayout()
        );

        model =
                new DefaultListModel<>();

        programList =
                new JList<>(model);

        currentInstruction =
                new JLabel(
                        "Current Instruction : None"
                );

        category =
                new JLabel(
                        "Category : None"
                );

        JPanel info =
                new JPanel(
                        new GridLayout(2, 1)
                );

        info.add(currentInstruction);
        info.add(category);

        add(
                new JScrollPane(programList),
                BorderLayout.CENTER
        );

        add(
                info,
                BorderLayout.SOUTH
        );
    }

    public void showProgram(
            ArrayList<Instruction> program
    ) {

        model.clear();

        for (int i = 0; i < program.size(); i++) {

            model.addElement(
                    String.format(
                            "%04XH    %s",
                            i,
                            program.get(i)
                    )
            );
        }
    }

    public void showCurrentInstruction(
            int pc,
            Instruction instruction
    ) {

        currentInstruction.setText(
                "Current Instruction : "
                        + instruction
        );

        category.setText(
                "Category : "
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

        } else {

            currentInstruction.setText(
                    "Current Instruction : None"
            );

            category.setText(
                    "Category : None"
            );
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
                return "Simulator Termination";

            default:
                return "Unknown";
        }
    }
}

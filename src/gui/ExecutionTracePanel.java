package gui;

import javax.swing.*;
import java.awt.*;

public class ExecutionTracePanel extends JPanel {

    private JTextArea traceArea;

    public ExecutionTracePanel() {

        setBorder(
                BorderFactory.createTitledBorder(
                        "FETCH -> DECODE -> EXECUTE"
                )
        );

        setLayout(
                new BorderLayout()
        );

        traceArea =
                new JTextArea(8, 70);

        traceArea.setEditable(false);

        add(
                new JScrollPane(traceArea),
                BorderLayout.CENTER
        );
    }

    public void addTrace(String text) {

        traceArea.append(
                text + "\n"
        );

        traceArea.setCaretPosition(
                traceArea.getDocument().getLength()
        );
    }

    public void clearTrace() {

        traceArea.setText("");
    }
}

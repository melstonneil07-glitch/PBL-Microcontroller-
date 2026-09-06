package gui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class ExecutionTracePanel extends JPanel {

    private JTextArea traceArea;

    public ExecutionTracePanel() {
        setLayout(new BorderLayout());
        setBorder(
            BorderFactory.createTitledBorder("Execution Trace")
        );

        traceArea = new JTextArea(10, 60);
        traceArea.setEditable(false);

        add(new JScrollPane(traceArea), BorderLayout.CENTER);
    }

    public void clearTrace() {
        traceArea.setText("");
    }

    public void addTrace(String text) {
        traceArea.append(text + "\n");
        traceArea.setCaretPosition(traceArea.getDocument().getLength());
    }
}
